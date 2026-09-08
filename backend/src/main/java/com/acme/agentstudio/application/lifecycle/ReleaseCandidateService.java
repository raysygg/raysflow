package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.application.workflow.WorkflowDependencyResolver;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateDetail;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateDiff;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateSnapshot;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateSummary;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.KnowledgeBinding;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleException;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseCandidateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationReleaseCandidateMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.workflow.GraphContractValidator;
import com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 应用发布候选版本（Release Candidate）冻结与全生命周期服务。
 * 负责从已通过校验的最新 Draft 提取不可变运行图（Graph）与依赖快照（Dependency Snapshot）、脱敏擦除凭据明文、计算 SHA-256 特征指纹并保证并发创建的幂等落盘，为后续的自动化测试与门禁审查提供数据源。
 */
@Service
public class ReleaseCandidateService {

    /** 快照 Schema 版本定义 */
    private static final String SNAPSHOT_SCHEMA = "application-release-candidate-v1";

    /** 哈希摘要算法名 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** 敏感加密与凭据字段名集合（脱敏擦除目标） */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "apiKey", "apiSecret", "secret", "password", "token", "credential",
            "accessToken", "webhookSecret", "connectionUrl", "baseUrl");

    /** 候选版本 Persistence Mapper */
    private final ApplicationReleaseCandidateMapper candidateMapper;

    /** 工作流应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 草稿版本 Mapper */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /** 图定义解析组件 */
    private final GraphDefinitionParser graphParser;

    /** 图定义规范校验器 */
    private final GraphContractValidator graphValidator;

    /** 工作流依赖解析器 */
    private final WorkflowDependencyResolver dependencyResolver;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入候选版本服务依赖组件。
     */
    public ReleaseCandidateService(ApplicationReleaseCandidateMapper candidateMapper,
                                   OrchestrationAppMapper appMapper,
                                   OrchestrationDraftRevisionMapper draftMapper,
                                   GraphDefinitionParser graphParser,
                                   GraphContractValidator graphValidator,
                                   WorkflowDependencyResolver dependencyResolver,
                                   ObjectMapper objectMapper) {
        this.candidateMapper = candidateMapper;
        this.appMapper = appMapper;
        this.draftMapper = draftMapper;
        this.graphParser = graphParser;
        this.graphValidator = graphValidator;
        this.dependencyResolver = dependencyResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * 从最新的应用草稿版本（Draft）冻结生成一份不可变的 Candidate 候选版本快照。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param expectedRevisionNo 期望的基础草稿 Revision 版本号
     * @param changeSummary 变更说明摘要
     * @return 生成的候选版本摘要契约
     */
    @Transactional
    public CandidateSummary create(SecurityUser user, Long applicationId, Integer expectedRevisionNo,
                                   String changeSummary) {
        requireIdentity(user);
        requireApplication(user.getTenantId(), applicationId);
        OrchestrationDraftRevisionEntity draft = currentDraft(user.getTenantId(), applicationId);
        if (draft == null) {
            throw error("DRAFT_NOT_FOUND", "当前应用没有可创建候选版本的草稿。");
        }
        if (expectedRevisionNo != null && !expectedRevisionNo.equals(draft.getRevisionNo())) {
            throw error("DRAFT_REVISION_CONFLICT", "草稿已被他人修改更新，请刷新页面后重新提交创建。");
        }

        var graph = graphParser.read(draft.getGraphJson());
        var validation = graphValidator.validateForPublish(graph);
        if (!validation.valid()) {
            throw error("DRAFT_INVALID", validation.issues().get(0).message());
        }
        var dependencies = dependencyResolver.resolve(user.getTenantId(), graph);
        if (!dependencies.issues().isEmpty()) {
            throw error("DEPENDENCY_INVALID", dependencies.issues().get(0).message());
        }

        JsonNode sanitizedGraph = sanitize(graphParser.write(graph));
        String dependencyFingerprint = sha256(write(dependencies.snapshot()));
        CandidateSnapshot snapshot = new CandidateSnapshot(
                SNAPSHOT_SCHEMA,
                applicationId,
                draft.getRevisionNo(),
                sanitizedGraph,
                dependencyFingerprint,
                List.of(),
                List.of(),
                knowledgeBinding(sanitizedGraph)
        );
        String snapshotJson = write(snapshot);
        String fingerprint = sha256(snapshotJson);

        ApplicationReleaseCandidateEntity existing = findByFingerprint(user.getTenantId(), applicationId, fingerprint);
        if (existing != null) {
            return toSummary(existing);
        }

        ApplicationReleaseCandidateEntity entity = new ApplicationReleaseCandidateEntity();
        entity.setTenantId(user.getTenantId());
        entity.setApplicationId(applicationId);
        entity.setDraftRevisionId(draft.getId());
        entity.setDraftRevisionNo(draft.getRevisionNo());
        entity.setSnapshotJson(snapshotJson);
        entity.setSnapshotFingerprint(fingerprint);
        entity.setBaseReleaseId(draft.getBaseVersionId());
        entity.setChangeSummary(changeSummary == null ? draft.getChangeSummary() : changeSummary);
        entity.setCandidateStatus(CandidateStatus.CREATED.name());
        entity.setCreatedBy(user.getUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setLockVersion(0);

        try {
            candidateMapper.insert(entity);
        } catch (RuntimeException exception) {
            ApplicationReleaseCandidateEntity concurrent = findByFingerprint(user.getTenantId(), applicationId, fingerprint);
            if (concurrent != null) {
                return toSummary(concurrent);
            }
            throw exception;
        }
        return toSummary(entity);
    }

    /**
     * 查询指定应用下的候选发布版本列表。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @return 候选版本摘要列表
     */
    public List<CandidateSummary> list(SecurityUser user, Long applicationId) {
        requireIdentity(user);
        requireApplication(user.getTenantId(), applicationId);
        return candidateMapper.selectList(new LambdaQueryWrapper<ApplicationReleaseCandidateEntity>()
                        .eq(ApplicationReleaseCandidateEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationReleaseCandidateEntity::getApplicationId, applicationId)
                        .orderByDesc(ApplicationReleaseCandidateEntity::getCreatedAt))
                .stream().map(this::toSummary).toList();
    }

    /**
     * 查询单个 Candidate 候选版本的元数据与锁定状态详情。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @return 候选版本详情契约对象
     */
    public CandidateDetail detail(SecurityUser user, Long applicationId, Long candidateId) {
        ApplicationReleaseCandidateEntity entity = requireCandidate(user, applicationId, candidateId);
        return new CandidateDetail(toSummary(entity), entity.getChangeSummary(), entity.getBaseReleaseId(), entity.getLockVersion());
    }

    /**
     * 获取候选版本与基线/历史版本的 Diff 对比信息。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @return 候选版本 Diff 对比结构
     */
    public CandidateDiff diff(SecurityUser user, Long applicationId, Long candidateId) {
        ApplicationReleaseCandidateEntity entity = requireCandidate(user, applicationId, candidateId);
        List<String> sections = new ArrayList<>();
        sections.add("workflow");
        if (entity.getBaseReleaseId() != null) {
            sections.add("releaseDependencies");
        }
        return new CandidateDiff(entity.getId(), entity.getDraftRevisionId(), sections);
    }

    /**
     * 推进候选版本的生命周期状态（CAS 乐观锁防并发覆盖）。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @param expected 期望的现处于状态
     * @param target 推进的目标状态
     */
    @Transactional
    public void transition(SecurityUser user, Long applicationId, Long candidateId,
                           CandidateStatus expected, CandidateStatus target) {
        ApplicationReleaseCandidateEntity current = requireCandidate(user, applicationId, candidateId);
        int updated = candidateMapper.update(null, new LambdaUpdateWrapper<ApplicationReleaseCandidateEntity>()
                .eq(ApplicationReleaseCandidateEntity::getTenantId, user.getTenantId())
                .eq(ApplicationReleaseCandidateEntity::getApplicationId, applicationId)
                .eq(ApplicationReleaseCandidateEntity::getId, candidateId)
                .eq(ApplicationReleaseCandidateEntity::getCandidateStatus, expected.name())
                .eq(ApplicationReleaseCandidateEntity::getLockVersion, current.getLockVersion())
                .set(ApplicationReleaseCandidateEntity::getCandidateStatus, target.name())
                .setSql("lock_version = lock_version + 1"));
        if (updated != 1) {
            throw error("CANDIDATE_STATE_CONFLICT", "候选版本已被他人修改或评测状态已变化，请刷新页面后重试。");
        }
    }

    /**
     * 校验并获取特定候选版本实体。
     */
    public ApplicationReleaseCandidateEntity requireCandidate(SecurityUser user, Long applicationId, Long candidateId) {
        requireIdentity(user);
        requireApplication(user.getTenantId(), applicationId);
        ApplicationReleaseCandidateEntity entity = candidateMapper.selectOne(new LambdaQueryWrapper<ApplicationReleaseCandidateEntity>()
                .eq(ApplicationReleaseCandidateEntity::getTenantId, user.getTenantId())
                .eq(ApplicationReleaseCandidateEntity::getApplicationId, applicationId)
                .eq(ApplicationReleaseCandidateEntity::getId, candidateId));
        if (entity == null) {
            throw error("CANDIDATE_NOT_FOUND", "候选发布版本不存在。");
        }
        return entity;
    }

    /**
     * 按特征指纹查询已有候选版本实体。
     */
    private ApplicationReleaseCandidateEntity findByFingerprint(Long tenantId, Long applicationId, String fingerprint) {
        return candidateMapper.selectOne(new LambdaQueryWrapper<ApplicationReleaseCandidateEntity>()
                .eq(ApplicationReleaseCandidateEntity::getTenantId, tenantId)
                .eq(ApplicationReleaseCandidateEntity::getApplicationId, applicationId)
                .eq(ApplicationReleaseCandidateEntity::getSnapshotFingerprint, fingerprint)
                .last("LIMIT 1"));
    }

    /**
     * 校验应用存在。
     */
    private void requireApplication(Long tenantId, Long applicationId) {
        OrchestrationAppEntity app = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, tenantId)
                .eq(OrchestrationAppEntity::getId, applicationId));
        if (app == null) {
            throw error("APPLICATION_NOT_FOUND", "应用不存在或不属于当前租户。");
        }
    }

    /**
     * 获取最新草稿版本。
     */
    private OrchestrationDraftRevisionEntity currentDraft(Long tenantId, Long applicationId) {
        return draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, tenantId)
                .eq(OrchestrationDraftRevisionEntity::getAppId, applicationId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo)
                .last("LIMIT 1"));
    }

    /**
     * 递归从快照节点数据中擦除 API Key 等敏感凭据字符串。
     */
    private JsonNode sanitize(JsonNode value) {
        if (value == null || value.isValueNode()) {
            return value;
        }
        if (value.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            value.forEach(item -> result.add(sanitize(item)));
            return result;
        }
        ObjectNode result = objectMapper.createObjectNode();
        value.fields().forEachRemaining(entry -> {
            if (!SENSITIVE_FIELDS.contains(entry.getKey())) {
                result.set(entry.getKey(), sanitize(entry.getValue()));
            }
        });
        return result;
    }

    /**
     * 绑定 RAG 节点固定的 Profile Version 与 Generation。
     */
    private KnowledgeBinding knowledgeBinding(JsonNode graph) {
        Long profileVersionId = findLong(graph, "profileVersionId");
        Long generationId = findLong(graph, "generationId");
        return new KnowledgeBinding(profileVersionId, generationId);
    }

    /**
     * 深度递归寻找属性名对应的 Long 值。
     */
    private Long findLong(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.canConvertToLong()) {
                return value.longValue();
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Long found = findLong(fields.next().getValue(), fieldName);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                Long found = findLong(item, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 序列化对象为 JSON 字符串。
     */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw error("SNAPSHOT_SERIALIZE_FAILED", "候选版本快照序列化失败。");
        }
    }

    /**
     * 计算字符串 SHA-256 特征摘要。
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw error("SNAPSHOT_HASH_FAILED", "候选版本快照指纹计算生成失败。");
        }
    }

    /**
     * 转换实体对象为 CandidateSummary 契约。
     */
    private CandidateSummary toSummary(ApplicationReleaseCandidateEntity entity) {
        return new CandidateSummary(
                entity.getId(),
                entity.getApplicationId(),
                entity.getDraftRevisionId(),
                entity.getDraftRevisionNo(),
                entity.getSnapshotFingerprint(),
                CandidateStatus.valueOf(entity.getCandidateStatus()),
                entity.getCreatedAt()
        );
    }

    /**
     * 校验身份。
     */
    private void requireIdentity(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw error("IDENTITY_INVALID", "当前身份无效。");
        }
    }

    /**
     * 构造 ApplicationLifecycleException。
     */
    private ApplicationLifecycleException error(String code, String message) {
        return new ApplicationLifecycleException(code, message);
    }
}

