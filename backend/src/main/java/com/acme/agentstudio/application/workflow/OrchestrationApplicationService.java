package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.application.lifecycle.ApplicationReleaseGateService;
import com.acme.agentstudio.application.saas.AdoptionAnalyticsService;
import com.acme.agentstudio.common.exception.OrchestrationConflictException;
import com.acme.agentstudio.common.exception.VersionUnavailableException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateLevel;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventSource;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventType;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphEdge;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.domain.workflow.model.OrchestrationDraftSummary;
import com.acme.agentstudio.domain.workflow.model.OrchestrationVersionSummary;
import com.acme.agentstudio.domain.workflow.model.VariableReference;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.domain.workflow.model.WorkflowReleaseBundle;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseCandidateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEdgeEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEnvironmentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationNodeEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVariableEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationReleaseCandidateMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEdgeMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEnvironmentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationNodeMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVariableMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.acme.agentstudio.infrastructure.workflow.GraphContractValidator;
import com.acme.agentstudio.infrastructure.workflow.GraphValidationResult;
import com.acme.agentstudio.infrastructure.workflow.NodeRegistry;
import com.acme.agentstudio.infrastructure.workflow.NodeTypeDescriptor;
import com.acme.agentstudio.infrastructure.workflow.ValidationIssue;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationDraftRequest;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationPublishRequest;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationRollbackRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 编排应用核心应用服务（Orchestration Application Service）。
 * 负责画布草稿多版本修订保存（Revision）、图结构静态校验与依赖分析、不可变版本发布（Publish）、
 * 生产/测试环境指针绑定（Environment Pointer）以及快速一键回滚（Rollback）。
 */
@Service
public class OrchestrationApplicationService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(OrchestrationApplicationService.class);

    /**
     * 默认部署环境 Code
     */
    private static final String DEFAULT_ENVIRONMENT = "PRODUCTION";

    /**
     * 发布快照 Schema 标识
     */
    private static final String RELEASE_BUNDLE_SCHEMA = "release-bundle-v1";

    /**
     * 哈希摘要算法
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * 编排应用 Mapper
     */
    private final OrchestrationAppMapper appMapper;

    /**
     * 草稿修订 Mapper
     */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /**
     * 正式版本 Mapper
     */
    private final OrchestrationVersionMapper versionMapper;

    /**
     * 环境绑定 Mapper
     */
    private final OrchestrationEnvironmentMapper environmentMapper;

    /**
     * 画布节点 Mapper
     */
    private final OrchestrationNodeMapper nodeMapper;

    /**
     * 画布连线 Mapper
     */
    private final OrchestrationEdgeMapper edgeMapper;

    /**
     * 画布变量 Mapper
     */
    private final OrchestrationVariableMapper variableMapper;

    /**
     * 工作流图契约校验器
     */
    private final GraphContractValidator graphValidator;

    /**
     * 节点注册表
     */
    private final NodeRegistry nodeRegistry;

    /**
     * 编排权限校验服务
     */
    private final OrchestrationAuthorizationService authorizationService;

    /**
     * 审计应用服务
     */
    private final AuditApplicationService auditApplicationService;

    /**
     * 工作流依赖分析解析器
     */
    private final WorkflowDependencyResolver dependencyResolver;

    /**
     * 工作流入口校验器
     */
    private final WorkflowEntrypointValidator entrypointValidator;

    /**
     * Jackson JSON 映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 发布候选 Candidate Mapper
     */
    private final ApplicationReleaseCandidateMapper candidateMapper;

    /**
     * 发布门禁服务
     */
    private final ApplicationReleaseGateService gateService;

    /**
     * SaaS 采用度分析服务
     */
    private final AdoptionAnalyticsService adoptionAnalyticsService;

    /**
     * 构造函数注入编排应用服务相关依赖组件。
     */
    public OrchestrationApplicationService(
            OrchestrationAppMapper appMapper,
            OrchestrationDraftRevisionMapper draftMapper,
            OrchestrationVersionMapper versionMapper,
            OrchestrationEnvironmentMapper environmentMapper,
            OrchestrationNodeMapper nodeMapper,
            OrchestrationEdgeMapper edgeMapper,
            OrchestrationVariableMapper variableMapper,
            GraphContractValidator graphValidator,
            NodeRegistry nodeRegistry,
            OrchestrationAuthorizationService authorizationService,
            AuditApplicationService auditApplicationService,
            WorkflowDependencyResolver dependencyResolver,
            WorkflowEntrypointValidator entrypointValidator,
            ObjectMapper objectMapper,
            ApplicationReleaseCandidateMapper candidateMapper,
            ApplicationReleaseGateService gateService,
            AdoptionAnalyticsService adoptionAnalyticsService
    ) {
        this.appMapper = appMapper;
        this.draftMapper = draftMapper;
        this.versionMapper = versionMapper;
        this.environmentMapper = environmentMapper;
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
        this.variableMapper = variableMapper;
        this.graphValidator = graphValidator;
        this.nodeRegistry = nodeRegistry;
        this.authorizationService = authorizationService;
        this.auditApplicationService = auditApplicationService;
        this.dependencyResolver = dependencyResolver;
        this.entrypointValidator = entrypointValidator;
        this.objectMapper = objectMapper;
        this.candidateMapper = candidateMapper;
        this.gateService = gateService;
        this.adoptionAnalyticsService = adoptionAnalyticsService;
    }

    /**
     * 保存画布新编辑的草稿修订（Draft Revision）。
     * 采用追加修订号 revisionNo 的设计，配合 expectedRevisionNo 防止多终端并发覆盖。
     *
     * @param user    当前操作用户安全上下文
     * @param request 草稿保存请求体
     * @return 最新的草稿修订摘要对象
     */
    @Transactional
    public OrchestrationDraftSummary saveDraft(SecurityUser user, OrchestrationDraftRequest request) {
        requireIdentity(user);
        if (request == null || request.graphJson() == null || request.graphJson().isBlank()) {
            throw new IllegalArgumentException("保存草稿的工作流 JSON 字符串不能为空。");
        }

        GraphDefinition graph = normalizeAndValidate(request.graphJson());
        OrchestrationAppEntity app = findOrCreateApp(user, request, graph);
        authorizationService.require(user, app.getId(), "EDIT_DRAFT");

        OrchestrationDraftRevisionEntity current = currentDraft(app.getId(), user.getTenantId());
        if (request.expectedRevisionNo() != null && current != null && !request.expectedRevisionNo().equals(current.getRevisionNo())) {
            throw new OrchestrationConflictException("草稿修订检测到并发冲突：数据库当前最新修订号为 [" + current.getRevisionNo() + "]，请刷新画布后重试。");
        }

        int revisionNo = (current == null) ? 1 : (current.getRevisionNo() + 1);
        OrchestrationDraftRevisionEntity draft = new OrchestrationDraftRevisionEntity();
        draft.setTenantId(user.getTenantId());
        draft.setAppId(app.getId());
        draft.setRevisionNo(revisionNo);
        draft.setGraphJson(writeGraph(graph));
        draft.setBaseVersionId(current != null && current.getBaseVersionId() != null
                ? current.getBaseVersionId()
                : currentVersionId(app.getId(), user.getTenantId(), DEFAULT_ENVIRONMENT));
        draft.setChangeSummary(request.changeSummary());
        draft.setCreatedBy(user.getUserId());
        draft.setCreatedAt(LocalDateTime.now());
        draftMapper.insert(draft);

        app.setCurrentRevisionId(draft.getId());
        app.setGraphType(graph.graphType());
        app.setStatus(BusinessStatus.DRAFT);
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);

        persistGraphParts(app, draft, graph);

        auditApplicationService.recordWorkflowAction(user.getTenantId(), user.getUsername(), "ORCHESTRATION_DRAFT_SAVE", app.getId(), detail("revisionNo", revisionNo));
        adoptionAnalyticsService.record(new AdoptionAnalyticsService.EventCommand(
                user.getTenantId(),
                app.getId(),
                null,
                null,
                AdoptionEventType.DRAFT_CREATED,
                AdoptionEventSource.PRODUCTION,
                "draft:" + app.getId() + ":" + draft.getId(),
                draft.getCreatedAt(),
                null
        ));
        return toDraft(app, draft);
    }

    /**
     * 获取指定应用的最新草稿内容。
     *
     * @param user  当前操作用户
     * @param appId 编排应用 ID
     * @return 草稿修订摘要对象
     */
    public OrchestrationDraftSummary getDraft(SecurityUser user, Long appId) {
        requireIdentity(user);
        authorizationService.require(user, appId, "READ_DRAFT");
        OrchestrationAppEntity app = requireApp(user.getTenantId(), appId);
        OrchestrationDraftRevisionEntity draft = currentDraft(appId, user.getTenantId());
        if (draft == null) {
            throw new IllegalArgumentException("当前编排应用尚未创建任何草稿修订。");
        }
        return toDraft(app, draft);
    }

    /**
     * 获取平台注册的所有工作流节点组件定义列表。
     *
     * @return 节点组件描述符列表
     */
    public List<NodeTypeDescriptor> listNodeTypes() {
        return nodeRegistry.list().stream().toList();
    }

    /**
     * 对当前草稿执行静态图校验，返回潜在的配置缺陷与校验问题列表。
     *
     * @param user  当前操作用户
     * @param appId 编排应用 ID
     * @return 静态校验发现的问题列表
     */
    public List<ValidationIssue> checkDraft(SecurityUser user, Long appId) {
        requireIdentity(user);
        authorizationService.require(user, appId, "READ_DRAFT");
        OrchestrationDraftRevisionEntity draft = currentDraft(appId, user.getTenantId());
        if (draft == null) {
            throw new IllegalArgumentException("当前编排应用不存在可校验的草稿。");
        }
        GraphDefinition graph = graphValidator.normalize(draft.getGraphJson());
        return validateForPublish(user.getTenantId(), appId, graph);
    }

    /**
     * 将当前草稿发布为不可变的正式版本（Version），并将环境指针指向该版本。
     *
     * @param user    当前操作用户
     * @param appId   编排应用 ID
     * @param request 发布请求参数
     * @return 发布生成的版本摘要对象
     */
    @Transactional
    public OrchestrationVersionSummary publish(SecurityUser user, Long appId, OrchestrationPublishRequest request) {
        requireIdentity(user);
        authorizationService.require(user, appId, "PUBLISH");
        OrchestrationAppEntity app = requireApp(user.getTenantId(), appId);
        OrchestrationDraftRevisionEntity draft = currentDraft(appId, user.getTenantId());
        if (draft == null) {
            throw new IllegalArgumentException("当前没有可用于发布的草稿修订。");
        }

        if (request != null && request.expectedRevisionNo() != null && !request.expectedRevisionNo().equals(draft.getRevisionNo())) {
            throw new OrchestrationConflictException("当前发布的草稿修订号已过期，请刷新画布后重试。");
        }
        if (request == null || request.candidateId() == null || request.candidateFingerprint() == null || request.evaluationRunId() == null) {
            throw new IllegalArgumentException("发布操作必须提供有效的 Candidate ID、指纹摘要与评测任务编号。");
        }

        ApplicationReleaseCandidateEntity candidate = candidateMapper.selectOne(
                new LambdaQueryWrapper<ApplicationReleaseCandidateEntity>()
                        .eq(ApplicationReleaseCandidateEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationReleaseCandidateEntity::getApplicationId, appId)
                        .eq(ApplicationReleaseCandidateEntity::getId, request.candidateId())
        );
        if (candidate == null || !request.candidateFingerprint().equals(candidate.getSnapshotFingerprint())) {
            throw new IllegalArgumentException("发布候选 Candidate 不存在或快照指纹与提交要求不匹配。");
        }

        var gate = gateService.evaluate(user, appId, candidate.getId(), request.candidateFingerprint(), request.evaluationRunId());
        if (gate.overallLevel() == GateLevel.BLOCKER) {
            throw new IllegalArgumentException("应用发布已被质量门禁强行阻断，请先修复所有 BLOCKER 阻断项后再试。");
        }

        GraphDefinition graph = graphValidator.normalize(draft.getGraphJson());
        WorkflowDependencyResolver.Resolution dependencies = requirePublishable(user.getTenantId(), appId, graph);

        int versionNo = versionMapper.selectList(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                        .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                        .eq(OrchestrationVersionEntity::getAppId, appId)).stream()
                .map(OrchestrationVersionEntity::getVersionNo)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;

        String versionId = "v_" + UUID.randomUUID().toString().replace("-", "");
        OrchestrationVersionEntity version = new OrchestrationVersionEntity();
        version.setTenantId(user.getTenantId());
        version.setAppId(appId);
        version.setCandidateId(candidate.getId());
        version.setCandidateFingerprint(candidate.getSnapshotFingerprint());
        version.setVersionId(versionId);
        version.setVersionNo(versionNo);
        version.setGraphJson(writeGraph(graph));

        String releaseBundleJson = writeReleaseBundle(app, versionId, versionNo, graph, dependencies.snapshot(), candidate);
        version.setReleaseBundleJson(releaseBundleJson);
        version.setReleaseBundleHash(hash(releaseBundleJson));
        version.setGateReportId(gate.reportId());
        version.setOverrideGranted(false);
        version.setChangeSummary(request == null ? null : request.changeSummary());
        version.setReleasedBy(user.getUserId());
        version.setReleasedAt(LocalDateTime.now());
        version.setStatus(BusinessStatus.PUBLISHED);
        versionMapper.insert(version);

        bindEnvironment(user, appId, request == null ? DEFAULT_ENVIRONMENT : environment(request.environmentCode()), versionId);
        app.setStatus(BusinessStatus.PUBLISHED);
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);

        if (CandidateStatus.READY.name().equals(candidate.getCandidateStatus())) {
            candidate.setCandidateStatus(CandidateStatus.PUBLISHED.name());
            candidateMapper.updateById(candidate);
        }

        auditApplicationService.recordWorkflowAction(user.getTenantId(), user.getUsername(), "ORCHESTRATION_PUBLISH", appId, detail("versionId", versionId, "versionNo", versionNo));
        adoptionAnalyticsService.record(new AdoptionAnalyticsService.EventCommand(
                user.getTenantId(),
                appId,
                versionId,
                null,
                AdoptionEventType.RELEASE_PUBLISHED,
                AdoptionEventSource.PRODUCTION,
                "release:" + versionId,
                version.getReleasedAt(),
                null
        ));
        log.info("工作流应用正式发布完成，tenantId=[{}], applicationId=[{}], versionId=[{}], versionNo=[{}]",
                user.getTenantId(), appId, versionId, versionNo);
        return toVersion(version, request == null ? DEFAULT_ENVIRONMENT : environment(request.environmentCode()), true);
    }

    /**
     * 将当前草稿发布为不可变版本并绑定环境。
     */
    @Transactional
    public OrchestrationVersionSummary rollback(SecurityUser user, Long appId, OrchestrationRollbackRequest request) {
        requireIdentity(user);
        authorizationService.require(user, appId, "ROLLBACK");
        if (request == null || request.versionId() == null || request.versionId().isBlank()) {
            throw new IllegalArgumentException("回滚版本不能为空。");
        }
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, request.versionId())
                .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));
        if (version == null) throw new IllegalArgumentException("目标发布版本不存在或不属于当前租户。");
        String env = environment(request.environmentCode());
        bindEnvironment(user, appId, env, version.getVersionId());
        auditApplicationService.recordWorkflowAction(user.getTenantId(), user.getUsername(),
                "ORCHESTRATION_ROLLBACK", appId, detail("versionId", version.getVersionId(), "reason", request.reason()));
        return toVersion(version, env, true);
    }

    /**
     * 查询指定环境当前指向的正式版本。
     */
    public OrchestrationVersionSummary currentVersion(SecurityUser user, Long appId, String environmentCode) {
        requireIdentity(user);
        authorizationService.require(user, appId, "RUN");
        String versionId = currentVersionId(appId, user.getTenantId(), environment(environmentCode));
        if (versionId == null) throw new IllegalArgumentException("当前环境没有可运行的已发布版本。");
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId)
                .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));
        if (version == null) throw new VersionUnavailableException("当前环境绑定的正式版本不可用。");
        requireReleaseBundle(version);
        return toVersion(version, environment(environmentCode), true);
    }

    /**
     * 从正式版本派生新草稿。
     */
    @Transactional
    public OrchestrationDraftSummary createDraftFromVersion(SecurityUser user, Long appId, String versionId) {
        requireIdentity(user);
        authorizationService.require(user, appId, "EDIT_DRAFT");
        OrchestrationAppEntity app = requireApp(user.getTenantId(), appId);
        OrchestrationVersionEntity version = requirePublishedVersion(user.getTenantId(), appId, versionId);
        GraphDefinition graph = normalizeAndValidate(version.getGraphJson());
        OrchestrationDraftRevisionEntity current = currentDraft(appId, user.getTenantId());
        OrchestrationDraftRevisionEntity draft = new OrchestrationDraftRevisionEntity();
        draft.setTenantId(user.getTenantId());
        draft.setAppId(appId);
        draft.setRevisionNo(current == null ? 1 : current.getRevisionNo() + 1);
        draft.setGraphJson(writeGraph(graph));
        draft.setBaseVersionId(versionId);
        draft.setChangeSummary("从正式版本创建草稿");
        draft.setCreatedBy(user.getUserId());
        draft.setCreatedAt(LocalDateTime.now());
        draftMapper.insert(draft);
        persistGraphParts(app, draft, graph);
        app.setCurrentRevisionId(draft.getId());
        app.setStatus(BusinessStatus.DRAFT);
        app.setUpdatedAt(LocalDateTime.now());
        appMapper.updateById(app);
        return toDraft(app, draft);
    }

    private List<ValidationIssue> validateForPublish(Long tenantId, Long appId, GraphDefinition graph) {
        List<ValidationIssue> issues = new ArrayList<>(graphValidator.validateForPublish(graph).issues());
        issues.addAll(dependencyResolver.resolve(tenantId, graph).issues());
        issues.addAll(entrypointValidator.validate(tenantId, appId, graph));
        return List.copyOf(issues);
    }

    private WorkflowDependencyResolver.Resolution requirePublishable(Long tenantId, Long appId, GraphDefinition graph) {
        GraphValidationResult graphResult = graphValidator.validateForPublish(graph);
        WorkflowDependencyResolver.Resolution dependencies = dependencyResolver.resolve(tenantId, graph);
        List<ValidationIssue> issues = new ArrayList<>(graphResult.issues());
        issues.addAll(dependencies.issues());
        issues.addAll(entrypointValidator.validate(tenantId, appId, graph));
        if (!issues.isEmpty())
            throw new IllegalArgumentException(issues.get(0).message() + " 修复建议：" + issues.get(0).suggestion());
        return dependencies;
    }

    private OrchestrationVersionEntity requirePublishedVersion(Long tenantId, Long appId, String versionId) {
        if (versionId == null || versionId.isBlank()) throw new IllegalArgumentException("发布版本不能为空。");
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, tenantId).eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId).eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));
        if (version == null) throw new IllegalArgumentException("目标发布版本不存在或不属于当前租户。");
        requireReleaseBundle(version);
        return version;
    }

    private void requireReleaseBundle(OrchestrationVersionEntity version) {
        if (version.getReleaseBundleJson() == null || version.getReleaseBundleJson().isBlank()
                || version.getReleaseBundleHash() == null || version.getReleaseBundleHash().isBlank()
                || !hash(version.getReleaseBundleJson()).equals(version.getReleaseBundleHash())) {
            throw new VersionUnavailableException("发布版本快照校验失败，禁止进入生产运行。");
        }
    }

    private GraphDefinition normalizeAndValidate(String graphJson) {
        GraphValidationResult result = graphValidator.validate(graphJson);
        if (!result.valid()) throw new IllegalArgumentException(result.issues().get(0).message());
        return result.graph();
    }

    private OrchestrationAppEntity findOrCreateApp(SecurityUser user, OrchestrationDraftRequest request, GraphDefinition graph) {
        OrchestrationAppEntity app = request.appId() == null ? null : requireApp(user.getTenantId(), request.appId());
        if (app == null && (request.appCode() == null || request.appCode().isBlank()))
            throw new IllegalArgumentException("应用编码不能为空。");
        if (app == null) app = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId()).eq(OrchestrationAppEntity::getAppCode, request.appCode()));
        if (app == null) {
            app = new OrchestrationAppEntity();
            app.setTenantId(user.getTenantId());
            app.setAppCode(request.appCode());
            app.setAppName(request.appName() == null ? request.appCode() : request.appName());
            app.setGraphType(graph.graphType());
            app.setStatus(BusinessStatus.DRAFT);
            app.setCreatedBy(user.getUserId());
            app.setCreatedAt(LocalDateTime.now());
            app.setUpdatedAt(app.getCreatedAt());
            appMapper.insert(app);
        }
        return app;
    }

    private void persistGraphParts(OrchestrationAppEntity app, OrchestrationDraftRevisionEntity draft, GraphDefinition graph) {
        graph.nodes().forEach(node -> {
            OrchestrationNodeEntity e = new OrchestrationNodeEntity();
            e.setTenantId(app.getTenantId());
            e.setAppId(app.getId());
            e.setRevisionId(draft.getId());
            e.setNodeId(node.nodeId());
            e.setNodeType(node.nodeType());
            e.setTitle(node.title());
            e.setConfigJson(node.config().toString());
            e.setInputSchemaJson(node.inputSchema().toString());
            e.setOutputSchemaJson(node.outputSchema().toString());
            nodeMapper.insert(e);
        });
        graph.edges().forEach(edge -> {
            OrchestrationEdgeEntity e = new OrchestrationEdgeEntity();
            e.setTenantId(app.getTenantId());
            e.setAppId(app.getId());
            e.setRevisionId(draft.getId());
            e.setEdgeId(edge.edgeId());
            e.setSourceNodeId(edge.sourceNodeId());
            e.setSourcePort(edge.sourcePort());
            e.setTargetNodeId(edge.targetNodeId());
            e.setTargetPort(edge.targetPort());
            edgeMapper.insert(e);
        });
        graph.variables().forEach(variable -> {
            OrchestrationVariableEntity e = new OrchestrationVariableEntity();
            e.setTenantId(app.getTenantId());
            e.setAppId(app.getId());
            e.setRevisionId(draft.getId());
            e.setSourceNodeId(variable.sourceNodeId());
            e.setOutputPath(variable.outputPath());
            e.setDataType(variable.dataType());
            e.setSensitiveFlag(variable.sensitive());
            variableMapper.insert(e);
        });
    }

    private void bindEnvironment(SecurityUser user, Long appId, String env, String versionId) {
        OrchestrationEnvironmentEntity entity = environmentMapper.selectOne(new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                .eq(OrchestrationEnvironmentEntity::getTenantId, user.getTenantId()).eq(OrchestrationEnvironmentEntity::getAppId, appId).eq(OrchestrationEnvironmentEntity::getEnvironmentCode, env));
        if (entity == null) {
            entity = new OrchestrationEnvironmentEntity();
            entity.setTenantId(user.getTenantId());
            entity.setAppId(appId);
            entity.setEnvironmentCode(env);
            entity.setCurrentVersionId(versionId);
            entity.setUpdatedBy(user.getUserId());
            entity.setUpdatedAt(LocalDateTime.now());
            environmentMapper.insert(entity);
        } else {
            entity.setCurrentVersionId(versionId);
            entity.setUpdatedBy(user.getUserId());
            entity.setUpdatedAt(LocalDateTime.now());
            environmentMapper.updateById(entity);
        }
    }

    private OrchestrationAppEntity requireApp(Long tenantId, Long appId) {
        OrchestrationAppEntity app = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>().eq(OrchestrationAppEntity::getId, appId).eq(OrchestrationAppEntity::getTenantId, tenantId));
        if (app == null) throw new IllegalArgumentException("编排应用不存在或不属于当前租户。");
        return app;
    }

    private OrchestrationDraftRevisionEntity currentDraft(Long appId, Long tenantId) {
        return draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>().eq(OrchestrationDraftRevisionEntity::getAppId, appId).eq(OrchestrationDraftRevisionEntity::getTenantId, tenantId).orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
    }

    private String currentVersionId(Long appId, Long tenantId, String env) {
        OrchestrationEnvironmentEntity entity = environmentMapper.selectOne(new LambdaQueryWrapper<OrchestrationEnvironmentEntity>().eq(OrchestrationEnvironmentEntity::getTenantId, tenantId).eq(OrchestrationEnvironmentEntity::getAppId, appId).eq(OrchestrationEnvironmentEntity::getEnvironmentCode, env));
        return entity == null ? null : entity.getCurrentVersionId();
    }

    private String writeGraph(GraphDefinition graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (Exception exception) {
            throw new IllegalStateException("编排图序列化失败。", exception);
        }
    }

    private OrchestrationDraftSummary toDraft(OrchestrationAppEntity app, OrchestrationDraftRevisionEntity draft) {
        return new OrchestrationDraftSummary(app.getId(), draft.getId(), draft.getRevisionNo(), app.getAppCode(), app.getAppName(), app.getGraphType(), app.getStatus(), draft.getGraphJson(), draft.getBaseVersionId());
    }

    private OrchestrationVersionSummary toVersion(OrchestrationVersionEntity version, String env, boolean current) {
        return new OrchestrationVersionSummary(version.getAppId(), version.getVersionId(), version.getVersionNo(), env, version.getStatus(), version.getReleasedBy(), version.getReleasedAt(), version.getGraphJson(), version.getReleaseBundleHash(), current);
    }

    private String writeReleaseBundle(OrchestrationAppEntity app, String versionId, int versionNo,
                                      GraphDefinition graph, WorkflowDependencySnapshot dependencies,
                                      ApplicationReleaseCandidateEntity candidate) {
        WorkflowReleaseBundle bundle = new WorkflowReleaseBundle(RELEASE_BUNDLE_SCHEMA,
                new WorkflowReleaseBundle.ApplicationSnapshot(app.getId(), app.getAppCode(), app.getAppName(), app.getGraphType()),
                new WorkflowReleaseBundle.ReleaseSnapshot(versionId, versionNo, DEFAULT_ENVIRONMENT, candidate.getId(), candidate.getSnapshotFingerprint()),
                graph.inputSchema(), graph.outputSchema(), graph, dependencies,
                new WorkflowReleaseBundle.RuntimePolicy(true, true));
        try {
            return objectMapper.writeValueAsString(bundle);
        } catch (Exception exception) {
            throw new IllegalStateException("发布快照序列化失败。", exception);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("系统不支持发布快照摘要算法。", exception);
        }
    }

    /**
     * 构造可变审计细项 Map
     */
    private Map<String, Object> detail(Object... values) {
        Map<String, Object> detail = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            detail.put(String.valueOf(values[i]), values[i + 1]);
        }
        return detail;
    }

    /**
     * 规范化环境编码字符串
     */
    private String environment(String value) {
        return value == null || value.isBlank() ? DEFAULT_ENVIRONMENT : value.toUpperCase();
    }

    /**
     * 校验安全用户身份非空
     */
    private void requireIdentity(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前操作用户的安全身份上下文无效或已失效。");
        }
    }
}
