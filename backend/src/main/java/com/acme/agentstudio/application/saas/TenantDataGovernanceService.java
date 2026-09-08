package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.GovernanceRequestStatus;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.GovernanceRequestType;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasDataRetentionPolicyEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasGovernanceEvidenceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasGovernanceRequestEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasLegalHoldEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasDataRetentionPolicyMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasGovernanceEvidenceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasGovernanceRequestMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasLegalHoldMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SaaS 租户数据保留策略（Retention Policy）、合规导出（Export）、物理清理删除（Delete）与诉讼冻结（Legal Hold）数据治理服务。
 * 严格遵从 Legal Hold 保护机制防止数据误删，记录操作审计证据链（Checksum），支持断点失败重试与到期凭证自动清理。
 */
@Service
public class TenantDataGovernanceService {

    /** 状态标识：活动中 */
    private static final String ACTIVE = "ACTIVE";

    /** 状态标识：已解除 Legal Hold */
    private static final String RELEASED = "RELEASED";

    /** 导出存储标识 */
    private static final String EXPORT_STORE = "EXPORT";

    /** 凭证状态：已完成 */
    private static final String EVIDENCE_COMPLETED = "COMPLETED";

    /** 凭证状态：失败 */
    private static final String EVIDENCE_FAILED = "FAILED";

    /** 可申请和查看租户治理任务的角色 */
    private static final Set<String> GOVERNANCE_REQUEST_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "COMPLIANCE");

    /** 可审批、执行和管理法律冻结的角色 */
    private static final Set<String> GOVERNANCE_APPROVE_ROLES = Set.of("SUPER_ADMIN", "COMPLIANCE");

    /** 数据保留策略 Mapper */
    private final SaasDataRetentionPolicyMapper retentionMapper;

    /** 治理工单 Mapper */
    private final SaasGovernanceRequestMapper requestMapper;

    /** 治理证据链 Mapper */
    private final SaasGovernanceEvidenceMapper evidenceMapper;

    /** Legal Hold 诉讼冻结 Mapper */
    private final SaasLegalHoldMapper holdMapper;

    /** 数据存储治理适配器列表 */
    private final List<DataGovernanceStoreAdapter> storeAdapters;

    /** 合规数据导出适配器列表 */
    private final List<GovernanceExportAdapter> exportAdapters;

    /** SaaS 治理配置属性 */
    private final SaasGovernanceProperties properties;

    /**
     * 构造函数注入数据治理相关组件依赖。
     */
    public TenantDataGovernanceService(SaasDataRetentionPolicyMapper retentionMapper,
                                       SaasGovernanceRequestMapper requestMapper,
                                       SaasGovernanceEvidenceMapper evidenceMapper,
                                       SaasLegalHoldMapper holdMapper,
                                       List<DataGovernanceStoreAdapter> storeAdapters,
                                       List<GovernanceExportAdapter> exportAdapters,
                                       SaasGovernanceProperties properties) {
        this.retentionMapper = retentionMapper;
        this.requestMapper = requestMapper;
        this.evidenceMapper = evidenceMapper;
        this.holdMapper = holdMapper;
        this.storeAdapters = storeAdapters;
        this.exportAdapters = exportAdapters;
        this.properties = properties;
    }

    /**
     * 创建或更新租户针对特定数据类别的保留策略（自动退役同类旧版本）。
     *
     * @param actor 当前操作用户
     * @param command 保留策略提交命令
     * @return 新建的策略实体
     */
    @Transactional
    public SaasDataRetentionPolicyEntity createRetentionPolicy(SecurityUser actor, RetentionPolicyCommand command) {
        requireRole(actor, GOVERNANCE_APPROVE_ROLES, "当前账号没有修改数据保留策略的合规权限。");

        int version = retentionMapper.selectList(new LambdaQueryWrapper<SaasDataRetentionPolicyEntity>()
                        .eq(SaasDataRetentionPolicyEntity::getTenantId, actor.getTenantId())
                        .eq(SaasDataRetentionPolicyEntity::getDataCategory, command.dataCategory())
                        .orderByDesc(SaasDataRetentionPolicyEntity::getVersionNo))
                .stream()
                .findFirst()
                .map(SaasDataRetentionPolicyEntity::getVersionNo)
                .orElse(0) + 1;

        retentionMapper.update(null, new LambdaUpdateWrapper<SaasDataRetentionPolicyEntity>()
                .eq(SaasDataRetentionPolicyEntity::getTenantId, actor.getTenantId())
                .eq(SaasDataRetentionPolicyEntity::getDataCategory, command.dataCategory())
                .eq(SaasDataRetentionPolicyEntity::getStatus, ACTIVE)
                .set(SaasDataRetentionPolicyEntity::getStatus, "RETIRED"));

        SaasDataRetentionPolicyEntity entity = new SaasDataRetentionPolicyEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setDataCategory(command.dataCategory());
        entity.setVersionNo(version);
        entity.setRetentionDays(command.retentionDays());
        entity.setLegalBasis(command.legalBasis());
        entity.setStatus(ACTIVE);
        entity.setEffectiveAt(LocalDateTime.now());
        entity.setCreatedBy(actor.getUserId());
        entity.setCreatedAt(LocalDateTime.now());

        retentionMapper.insert(entity);
        return entity;
    }

    /**
     * 预检在特定范围 JSON 限制下的待删除数据规模及 Legal Hold 阻塞状态。
     *
     * @param actor 当前操作用户
     * @param scopeJson 包含筛选条件的数据范围 JSON
     * @return GovernancePreview 预检结果
     */
    public GovernancePreview previewDelete(SecurityUser actor, String scopeJson) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有预览数据删除范围的权限。");

        boolean held = holdMapper.selectCount(new LambdaQueryWrapper<SaasLegalHoldEntity>()
                .eq(SaasLegalHoldEntity::getTenantId, actor.getTenantId())
                .eq(SaasLegalHoldEntity::getHoldStatus, ACTIVE)) > 0;

        List<StorePreviewView> stores = storeAdapters.stream().map(adapter -> {
            var preview = adapter.preview(actor.getTenantId(), scopeJson);
            return new StorePreviewView(adapter.storeType(), preview.objectCount(), preview.safeSummary(), held || preview.blocked());
        }).toList();

        return new GovernancePreview(held, stores, stores.stream().mapToLong(StorePreviewView::objectCount).sum());
    }

    /**
     * 获取当前租户所有生效中的数据保留策略。
     *
     * @param actor 当前操作用户
     * @return 保留策略列表
     */
    public List<SaasDataRetentionPolicyEntity> activeRetentionPolicies(SecurityUser actor) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有查看数据保留策略的权限。");
        return retentionMapper.selectList(new LambdaQueryWrapper<SaasDataRetentionPolicyEntity>()
                .eq(SaasDataRetentionPolicyEntity::getTenantId, actor.getTenantId())
                .eq(SaasDataRetentionPolicyEntity::getStatus, ACTIVE)
                .orderByAsc(SaasDataRetentionPolicyEntity::getDataCategory));
    }

    /**
     * 查询当前租户的所有治理工单列表（按创建时间倒序）。
     *
     * @param actor 当前操作用户
     * @return 治理工单实体列表
     */
    public List<SaasGovernanceRequestEntity> requests(SecurityUser actor) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有查看数据治理请求的权限。");
        return requestMapper.selectList(new LambdaQueryWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .orderByDesc(SaasGovernanceRequestEntity::getCreatedAt));
    }

    /**
     * 根据 ID 获取特定的数据治理工单。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return 治理工单实体
     */
    public SaasGovernanceRequestEntity request(SecurityUser actor, Long requestId) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有查看数据治理请求的权限。");
        return requestMapper.selectOne(new LambdaQueryWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceRequestEntity::getId, requestId));
    }

    /**
     * 查询治理工单对应的所有存储执行审计凭证（Evidence）。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return 审计凭证实体列表
     */
    public List<SaasGovernanceEvidenceEntity> evidence(SecurityUser actor, Long requestId) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有查看治理证据的权限。");
        return evidenceMapper.selectList(new LambdaQueryWrapper<SaasGovernanceEvidenceEntity>()
                .eq(SaasGovernanceEvidenceEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceEvidenceEntity::getRequestId, requestId)
                .orderByAsc(SaasGovernanceEvidenceEntity::getStoreType));
    }

    /**
     * 提交一条新的数据治理工单（如申请合规导出或数据物理删除）。
     *
     * @param actor 当前操作用户
     * @param type 治理请求类型（EXPORT / DELETE）
     * @param scopeJson 治理涉及的数据范围 JSON
     * @return 治理工单实体
     */
    @Transactional
    public SaasGovernanceRequestEntity request(SecurityUser actor, GovernanceRequestType type, String scopeJson) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有申请数据治理任务的权限。");
        if (type == GovernanceRequestType.DELETE && previewDelete(actor, scopeJson).legalHoldBlocked()) {
            throw new IllegalStateException("当前申请的数据范围处于活动 Legal Hold 诉讼冻结保护状态，禁止创建物理删除工单。");
        }

        SaasGovernanceRequestEntity entity = new SaasGovernanceRequestEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setRequestType(type.name());
        entity.setRequestStatus(GovernanceRequestStatus.REQUESTED.name());
        entity.setScopeJson(scopeJson);
        entity.setRequestedBy(actor.getUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());

        requestMapper.insert(entity);
        return entity;
    }

    /**
     * 管理员审批通过治理工单。
     *
     * @param actor 拥有审批权限的用户
     * @param requestId 工单 ID
     */
    @Transactional
    public void approve(SecurityUser actor, Long requestId) {
        requireRole(actor, GOVERNANCE_APPROVE_ROLES, "当前账号没有审批数据治理任务的权限。");
        requestMapper.update(null, new LambdaUpdateWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceRequestEntity::getId, requestId)
                .eq(SaasGovernanceRequestEntity::getRequestStatus, GovernanceRequestStatus.REQUESTED.name())
                .set(SaasGovernanceRequestEntity::getRequestStatus, GovernanceRequestStatus.APPROVED.name())
                .set(SaasGovernanceRequestEntity::getApprovedBy, actor.getUserId())
                .set(SaasGovernanceRequestEntity::getApprovedAt, LocalDateTime.now()));
    }

    /**
     * 执行已审批通过的数据物理删除工单（分派至各大存储 Adaptor 并写留凭证）。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return 存储删除执行小结
     */
    @Transactional
    public ExecutionSummary executeDelete(SecurityUser actor, Long requestId) {
        SaasGovernanceRequestEntity request = requireApproved(actor, requestId, GovernanceRequestType.DELETE);
        if (previewDelete(actor, request.getScopeJson()).legalHoldBlocked()) {
            throw new IllegalStateException("执行数据清理前检测到活动的 Legal Hold 诉讼冻结，删除被安全强行阻断。");
        }

        ArrayList<StoreResult> results = new ArrayList<>();
        for (DataGovernanceStoreAdapter adapter : storeAdapters) {
            try {
                var result = adapter.delete(actor.getTenantId(), request.getScopeJson(), "delete:" + requestId + ":" + adapter.storeType());
                saveEvidence(actor.getTenantId(), requestId, adapter.storeType(), result.success(), result.objectCount(), result.checksum(), result.safeSummary());
                results.add(new StoreResult(adapter.storeType(), result.success(), result.safeSummary()));
            } catch (RuntimeException exception) {
                saveEvidence(actor.getTenantId(), requestId, adapter.storeType(), false, 0, null, "存储数据物理清理失败。");
                results.add(new StoreResult(adapter.storeType(), false, "存储数据物理清理失败。"));
            }
        }

        boolean success = results.stream().allMatch(StoreResult::success);
        requestMapper.update(null, new LambdaUpdateWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceRequestEntity::getId, requestId)
                .set(SaasGovernanceRequestEntity::getRequestStatus, success ? GovernanceRequestStatus.COMPLETED.name() : GovernanceRequestStatus.PARTIAL_FAILED.name()));

        if (success) {
            requestMapper.update(null, new LambdaUpdateWrapper<SaasGovernanceRequestEntity>()
                    .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                    .eq(SaasGovernanceRequestEntity::getId, requestId)
                    .set(SaasGovernanceRequestEntity::getCompletedAt, LocalDateTime.now()));
        }

        return new ExecutionSummary(success, List.copyOf(results));
    }

    /**
     * 执行已审批通过的合规数据打包导出工单。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return 导出执行小结
     */
    @Transactional
    public ExecutionSummary executeExport(SecurityUser actor, Long requestId) {
        SaasGovernanceRequestEntity request = requireApproved(actor, requestId, GovernanceRequestType.EXPORT);
        ArrayList<StoreResult> results = new ArrayList<>();
        String protectedReference = null;

        for (GovernanceExportAdapter adapter : exportAdapters) {
            var result = adapter.generate(actor.getTenantId(), requestId, request.getScopeJson());
            saveEvidence(actor.getTenantId(), requestId, EXPORT_STORE, result.success(), result.objectCount(), result.checksum(), result.safeSummary());
            results.add(new StoreResult(EXPORT_STORE, result.success(), result.safeSummary()));
            if (result.success()) {
                protectedReference = result.protectedReference();
            }
        }

        boolean success = !results.isEmpty() && results.stream().allMatch(StoreResult::success);
        requestMapper.update(null, new LambdaUpdateWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceRequestEntity::getId, requestId)
                .set(SaasGovernanceRequestEntity::getRequestStatus, success ? GovernanceRequestStatus.COMPLETED.name() : GovernanceRequestStatus.PARTIAL_FAILED.name())
                .set(SaasGovernanceRequestEntity::getProtectedReference, success ? protectedReference : null)
                .set(SaasGovernanceRequestEntity::getExpiresAt, success ? LocalDateTime.now().plusMinutes(properties.getExportLinkExpiryMinutes()) : null));

        if (success) {
            requestMapper.update(null, new LambdaUpdateWrapper<SaasGovernanceRequestEntity>()
                    .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                    .eq(SaasGovernanceRequestEntity::getId, requestId)
                    .set(SaasGovernanceRequestEntity::getCompletedAt, LocalDateTime.now()));
        }

        return new ExecutionSummary(success, List.copyOf(results));
    }

    /**
     * 获取合规导出文件的短寿命临时安全下载凭证（不向 API 直接暴露出数据正文或长效下载路径）。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return ExportDownload 下载信息
     */
    public ExportDownload download(SecurityUser actor, Long requestId) {
        requireRole(actor, GOVERNANCE_REQUEST_ROLES, "当前账号没有下载数据导出文件的权限。");
        SaasGovernanceRequestEntity value = request(actor, requestId);
        if (value == null || !GovernanceRequestType.EXPORT.name().equals(value.getRequestType())
                || !GovernanceRequestStatus.COMPLETED.name().equals(value.getRequestStatus())) {
            throw new IllegalArgumentException("对应的合规数据导出工单不存在或尚未完成打包。");
        }
        if (value.getExpiresAt() == null || !value.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("导出打包文件的临时安全下载链接已过期，请重新申请。");
        }
        return new ExportDownload(value.getProtectedReference(), value.getExpiresAt(), value.getEvidenceHash());
    }

    /**
     * 定时清理已超过法定审计保留期（Audit Retention Days）的历史删除凭证记录。
     *
     * @return 被清理的废弃凭证条数
     */
    @Transactional
    @Scheduled(cron = "${app.saas-governance.evidence-cleanup-cron:0 30 3 * * *}")
    public int cleanupExpiredEvidence() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.getAuditRetentionDays());
        return evidenceMapper.delete(new LambdaQueryWrapper<SaasGovernanceEvidenceEntity>()
                .lt(SaasGovernanceEvidenceEntity::getCreatedAt, cutoff));
    }

    /**
     * 对部分由于底层存储异常导致失败（PARTIAL_FAILED）的治理工单发起断点重试。
     *
     * @param actor 当前操作用户
     * @param requestId 工单 ID
     * @return 重试执行结果
     */
    @Transactional
    public ExecutionSummary retry(SecurityUser actor, Long requestId) {
        SaasGovernanceRequestEntity value = request(actor, requestId);
        if (value == null || !GovernanceRequestStatus.PARTIAL_FAILED.name().equals(value.getRequestStatus())) {
            throw new IllegalArgumentException("只有处于部分失败（PARTIAL_FAILED）状态的治理工单才允许再次重试。");
        }
        return GovernanceRequestType.DELETE.name().equals(value.getRequestType())
                ? executeDelete(actor, requestId)
                : executeExport(actor, requestId);
    }

    /**
     * 开启法务诉讼冻结（Legal Hold），被冻结范围内的所有物理删除操作均将被强制关停阻断。
     *
     * @param actor 授权管理员
     * @param code 诉讼冻结编码/案件号
     * @param scopeJson 冻结适用范围 JSON
     * @param reason 诉讼冻结原因说明
     * @return Legal Hold 记录实体
     */
    @Transactional
    public SaasLegalHoldEntity createHold(SecurityUser actor, String code, String scopeJson, String reason) {
        requireRole(actor, GOVERNANCE_APPROVE_ROLES, "当前账号没有创建法律冻结的合规权限。");
        SaasLegalHoldEntity hold = new SaasLegalHoldEntity();
        hold.setTenantId(actor.getTenantId());
        hold.setHoldCode(code);
        hold.setScopeJson(scopeJson);
        hold.setReasonSummary(reason);
        hold.setHoldStatus(ACTIVE);
        hold.setAuthorizedBy(actor.getUserId());
        hold.setStartsAt(LocalDateTime.now());
        hold.setCreatedAt(LocalDateTime.now());
        holdMapper.insert(hold);
        return hold;
    }

    /**
     * 解除法务诉讼冻结（Legal Hold Release）。
     *
     * @param actor 授权管理员
     * @param holdId 诉讼冻结 ID
     */
    @Transactional
    public void releaseHold(SecurityUser actor, Long holdId) {
        requireRole(actor, GOVERNANCE_APPROVE_ROLES, "当前账号没有解除法律冻结的合规权限。");
        holdMapper.update(null, new LambdaUpdateWrapper<SaasLegalHoldEntity>()
                .eq(SaasLegalHoldEntity::getTenantId, actor.getTenantId())
                .eq(SaasLegalHoldEntity::getId, holdId)
                .eq(SaasLegalHoldEntity::getHoldStatus, ACTIVE)
                .set(SaasLegalHoldEntity::getHoldStatus, RELEASED)
                .set(SaasLegalHoldEntity::getReleasedBy, actor.getUserId())
                .set(SaasLegalHoldEntity::getReleasedAt, LocalDateTime.now()));
    }

    /** 保存审计证据链信息 */
    private void saveEvidence(Long tenantId, Long requestId, String store, boolean success, long count, String checksum, String summary) {
        SaasGovernanceEvidenceEntity evidence = evidenceMapper.selectOne(new LambdaQueryWrapper<SaasGovernanceEvidenceEntity>()
                .eq(SaasGovernanceEvidenceEntity::getTenantId, tenantId)
                .eq(SaasGovernanceEvidenceEntity::getRequestId, requestId)
                .eq(SaasGovernanceEvidenceEntity::getStoreType, store));

        if (evidence == null) {
            evidence = new SaasGovernanceEvidenceEntity();
            evidence.setTenantId(tenantId);
            evidence.setRequestId(requestId);
            evidence.setStoreType(store);
            evidence.setCreatedAt(LocalDateTime.now());
        }

        evidence.setExecutionStatus(success ? EVIDENCE_COMPLETED : EVIDENCE_FAILED);
        evidence.setObjectCount(count);
        evidence.setChecksum(checksum);
        evidence.setSafeSummary(summary);
        evidence.setCompletedAt(success ? LocalDateTime.now() : null);

        if (evidence.getId() == null) {
            evidenceMapper.insert(evidence);
        } else {
            evidenceMapper.updateById(evidence);
        }
    }

    /** 校验已被审批允许的治理工单 */
    private SaasGovernanceRequestEntity requireApproved(SecurityUser actor, Long id, GovernanceRequestType type) {
        requireRole(actor, GOVERNANCE_APPROVE_ROLES, "当前账号没有执行数据治理任务的权限。");
        SaasGovernanceRequestEntity value = requestMapper.selectOne(new LambdaQueryWrapper<SaasGovernanceRequestEntity>()
                .eq(SaasGovernanceRequestEntity::getTenantId, actor.getTenantId())
                .eq(SaasGovernanceRequestEntity::getId, id)
                .eq(SaasGovernanceRequestEntity::getRequestType, type.name())
                .in(SaasGovernanceRequestEntity::getRequestStatus, GovernanceRequestStatus.APPROVED.name(), GovernanceRequestStatus.PARTIAL_FAILED.name()));

        if (value == null) {
            throw new IllegalArgumentException("治理工单不存在、未审批通过或不匹配对应的操作类型。");
        }
        return value;
    }

    /** 校验 Actor 非空 */
    private void requireActor(SecurityUser actor) {
        if (actor == null || actor.getTenantId() == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份无效，请重新登录。");
        }
    }

    /** 校验数据治理角色边界 */
    private void requireRole(SecurityUser actor, Set<String> allowedRoles, String message) {
        requireActor(actor);
        if (actor.getRoles().stream().noneMatch(allowedRoles::contains)) {
            throw new AuthorizationDeniedException(message);
        }
    }

    /** 保留策略命令 Record */
    public record RetentionPolicyCommand(String dataCategory, int retentionDays, String legalBasis) { }

    /** 单个存储视图 Record */
    public record StorePreviewView(String storeType, long objectCount, String safeSummary, boolean blocked) { }

    /** 预检综合汇总 Record */
    public record GovernancePreview(boolean legalHoldBlocked, List<StorePreviewView> stores, long totalObjects) { }

    /** 单个存储执行结果 Record */
    public record StoreResult(String storeType, boolean success, String safeSummary) { }

    /** 执行汇总小结 Record */
    public record ExecutionSummary(boolean completed, List<StoreResult> stores) { }

    /** 导出下载契约 Record */
    public record ExportDownload(String protectedReference, LocalDateTime expiresAt, String checksum) { }
}

