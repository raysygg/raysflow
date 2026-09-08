package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.metrics.PlatformEventFactService;
import com.acme.agentstudio.application.saas.AdoptionAnalyticsService;
import com.acme.agentstudio.application.task.PersistentTaskQueueService;
import com.acme.agentstudio.application.task.TaskType;
import com.acme.agentstudio.config.RequestTraceContext;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventSource;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventType;
import com.acme.agentstudio.domain.workflow.model.ExecutionType;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.NodeDebugResult;
import com.acme.agentstudio.domain.workflow.model.RunType;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.domain.workflow.model.WorkflowReleaseBundle;
import com.acme.agentstudio.infrastructure.persistence.entity.ApprovalTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEnvironmentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationMessageEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApprovalTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEnvironmentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMessageMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.acme.agentstudio.infrastructure.security.ExecutionDataMasker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 编排工作流持久化执行与生命周期调度应用服务（Persistent Orchestration Execution Service）。
 * 负责应用工作流的队列排队（Enqueue）、实时触发（Realtime Run）、节点拓扑步进推进、断点恢复（Resume/Approval）、
 * 单节点草稿调试（Debug Node）以及执行轨迹事件（Execution Event Trace）的数据库持久化与脱敏广播。
 */
@Service
public class PersistentOrchestrationExecutionService {

    /**
     * 默认查询分页大小
     */
    private static final int DEFAULT_EXECUTION_PAGE_SIZE = 20;

    /**
     * 最大查询分页限制
     */
    private static final int MAX_EXECUTION_PAGE_SIZE = 50;

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(PersistentOrchestrationExecutionService.class);

    /**
     * 循环节点最大迭代上限
     */
    private static final int MAX_LOOP_ITERATIONS = 10_000;

    /**
     * 单节点最大重试尝试次数
     */
    private static final int MAX_NODE_RETRY_ATTEMPTS = RetryPolicy.PLATFORM_MAX_ATTEMPTS;

    /**
     * 单节点最大重试退避等待时间（毫秒）
     */
    private static final long MAX_RETRY_BACKOFF_MILLIS = RetryPolicy.PLATFORM_MAX_BACKOFF_MILLIS;

    /**
     * 最大事件单次查询大小
     */
    private static final int MAX_EVENT_QUERY_SIZE = 500;

    /**
     * 执行状态常量：运行中
     */
    private static final String STATUS_RUNNING = ExecutionStatus.RUNNING;

    /**
     * 执行状态常量：已成功
     */
    private static final String STATUS_SUCCEEDED = ExecutionStatus.SUCCEEDED;

    /**
     * 执行状态常量：等待审批
     */
    private static final String STATUS_WAITING_APPROVAL = "WAITING_APPROVAL";

    /**
     * 事件类型常量：节点开始
     */
    private static final String EVENT_NODE_STARTED = ExecutionEventType.NODE_STARTED;

    /**
     * 事件类型常量：节点成功
     */
    private static final String EVENT_NODE_SUCCEEDED = ExecutionEventType.NODE_SUCCEEDED;

    /**
     * 事件类型常量：节点调度重试
     */
    private static final String EVENT_NODE_RETRY_SCHEDULED = ExecutionEventType.NODE_RETRY_SCHEDULED;

    /**
     * 事件类型常量：LLM 上下文已解析
     */
    private static final String EVENT_LLM_CONTEXT_RESOLVED = ExecutionEventType.LLM_CONTEXT_RESOLVED;

    /**
     * 事件类型常量：模型流式 Delta 吐字
     */
    private static final String EVENT_MODEL_DELTA = ExecutionEventType.MODEL_DELTA;

    /**
     * 正式版本 Mapper
     */
    private final OrchestrationVersionMapper versionMapper;

    /**
     * 运行上下文 Mapper
     */
    private final PlatformExecutionContextMapper contextMapper;

    /**
     * 轨迹事件 Mapper
     */
    private final PlatformExecutionEventMapper eventMapper;

    /**
     * Jackson JSON 映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 对话 Entity Mapper
     */
    private final PlatformConversationMapper conversationMapper;

    /**
     * 消息 Entity Mapper
     */
    private final PlatformConversationMessageMapper messageMapper;

    /**
     * 平台事件统计事实服务
     */
    private final PlatformEventFactService eventFactService;

    /**
     * 编排应用 Mapper
     */
    private final OrchestrationAppMapper appMapper;

    /**
     * 环境绑定 Mapper
     */
    private final OrchestrationEnvironmentMapper environmentMapper;

    /**
     * 草稿修订 Mapper
     */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /**
     * 编排权限校验服务
     */
    private final OrchestrationAuthorizationService authorizationService;

    /**
     * 异步持久化任务队列服务
     */
    private final PersistentTaskQueueService taskQueueService;

    /**
     * 人工审批任务 Mapper
     */
    private final ApprovalTaskMapper approvalTaskMapper;

    /**
     * 实时执行事件发布器
     */
    private final ExecutionEventPublisher eventPublisher;

    /**
     * 节点策略注册表
     */
    private final NodeExecutionHandlerRegistry handlerRegistry;

    /**
     * 节点执行上下文组装器
     */
    private final ExecutionContextAssembler executionContextAssembler;

    /**
     * 敏感情报数据脱敏器
     */
    private final ExecutionDataMasker dataMasker;

    /**
     * 资源依赖解析器
     */
    private final WorkflowDependencyResolver dependencyResolver;

    /**
     * 统一工作流图执行引擎
     */
    private final WorkflowExecutionEngine workflowExecutionEngine;

    /**
     * SaaS 采用度分析服务
     */
    private final AdoptionAnalyticsService adoptionAnalyticsService;

    /**
     * 构造函数注入所有必要的持久化与业务处理依赖组件。
     */
    public PersistentOrchestrationExecutionService(
            OrchestrationVersionMapper versionMapper,
            PlatformExecutionContextMapper contextMapper,
            PlatformExecutionEventMapper eventMapper,
            ObjectMapper objectMapper,
            PlatformConversationMapper conversationMapper,
            PlatformConversationMessageMapper messageMapper,
            PlatformEventFactService eventFactService,
            OrchestrationAppMapper appMapper,
            OrchestrationEnvironmentMapper environmentMapper,
            OrchestrationAuthorizationService authorizationService,
            PersistentTaskQueueService taskQueueService,
            ApprovalTaskMapper approvalTaskMapper,
            ExecutionEventPublisher eventPublisher,
            NodeExecutionHandlerRegistry handlerRegistry,
            OrchestrationDraftRevisionMapper draftMapper,
            ExecutionContextAssembler executionContextAssembler,
            ExecutionDataMasker dataMasker,
            WorkflowDependencyResolver dependencyResolver,
            WorkflowExecutionEngine workflowExecutionEngine,
            AdoptionAnalyticsService adoptionAnalyticsService
    ) {
        this.versionMapper = versionMapper;
        this.contextMapper = contextMapper;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.eventFactService = eventFactService;
        this.appMapper = appMapper;
        this.environmentMapper = environmentMapper;
        this.draftMapper = draftMapper;
        this.executionContextAssembler = executionContextAssembler;
        this.authorizationService = authorizationService;
        this.taskQueueService = taskQueueService;
        this.approvalTaskMapper = approvalTaskMapper;
        this.eventPublisher = eventPublisher;
        this.handlerRegistry = handlerRegistry;
        this.dataMasker = dataMasker;
        this.dependencyResolver = dependencyResolver;
        this.workflowExecutionEngine = workflowExecutionEngine;
        this.adoptionAnalyticsService = adoptionAnalyticsService;
    }

    /**
     * 提交已发布工作流到持久化任务队列。
     */
    @Transactional
    public Map<String, Object> enqueue(SecurityUser user, Long appId, String versionId, String executionType,
                                       String idempotencyKey, String conversationId, String messageId,
                                       Map<String, Object> input) {
        requireUser(user);
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("幂等校验键不能为空。");
        PlatformExecutionContextEntity existing = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getIdempotencyKey, idempotencyKey));
        if (existing != null) return result(existing);
        PlatformExecutionContextEntity context = createQueuedContext(user, appId, versionId, executionType, idempotencyKey, input);
        Map<String, Object> payload = executionPayload(user, context, appId, versionId, executionType,
                idempotencyKey, conversationId, messageId, input);
        Long taskId = taskQueueService.enqueueOnce(user.getTenantId(), TaskType.ORCHESTRATION_EXECUTION,
                idempotencyKey, payload, MAX_NODE_RETRY_ATTEMPTS);
        context.setTaskId(taskId);
        contextMapper.updateById(context);
        return result(context);
    }

    /**
     * 创建由实时执行器立即接管的运行上下文。
     */
    @Transactional
    public Map<String, Object> createRealtimeRun(SecurityUser user, Long appId, String versionId,
                                                 String executionType, String idempotencyKey,
                                                 Map<String, Object> input) {
        requireUser(user);
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("实时运行的幂等键不能为空。");
        PlatformExecutionContextEntity existing = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getIdempotencyKey, idempotencyKey));
        return existing == null ? result(createQueuedContext(user, appId, versionId, executionType, idempotencyKey, input)) : result(existing);
    }

    /**
     * 提交当前草稿快照测试运行。
     */
    @Transactional
    public Map<String, Object> enqueueDraftTest(SecurityUser user, Long appId, String executionType,
                                                String idempotencyKey, String conversationId, String messageId,
                                                Map<String, Object> input) {
        requireUser(user);
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("测试运行的幂等校验键不能为空。");
        PlatformExecutionContextEntity existing = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getIdempotencyKey, idempotencyKey));
        if (existing != null) return result(existing);
        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationDraftRevisionEntity::getAppId, appId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
        if (draft == null || draft.getGraphJson() == null || draft.getGraphJson().isBlank()) {
            throw new IllegalArgumentException("当前应用没有可测试的流程草稿。");
        }
        authorizationService.require(user, appId, "RUN");
        authorizationService.requireGraphResources(user, appId,
                new com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser(objectMapper).read(draft.getGraphJson()));
        PlatformExecutionContextEntity context = new PlatformExecutionContextEntity();
        context.setTenantId(user.getTenantId());
        context.setExecutionId(UUID.randomUUID().toString());
        context.setRequestId(RequestTraceContext.currentRequestId());
        context.setTraceId(RequestTraceContext.currentTraceId());
        context.setSpanId(UUID.randomUUID().toString());
        context.setAppId(appId);
        context.setDraftRevisionId(draft.getId());
        context.setDraftRevisionNo(draft.getRevisionNo());
        context.setDraftGraphJson(draft.getGraphJson());
        context.setRunType(RunType.DRAFT_TEST.name());
        context.setExecutionType(executionType);
        context.setIdempotencyKey(idempotencyKey);
        context.setStatus(ExecutionStatus.QUEUED);
        context.setInputJson(write(input == null ? Map.of() : input));
        contextMapper.insert(context);
        Map<String, Object> payload = executionPayload(user, context, appId, null, executionType,
                idempotencyKey, conversationId, messageId, input);
        Long taskId = taskQueueService.enqueueOnce(user.getTenantId(), TaskType.ORCHESTRATION_DRAFT_TEST,
                idempotencyKey, payload, MAX_NODE_RETRY_ATTEMPTS);
        context.setTaskId(taskId);
        contextMapper.updateById(context);
        return result(context);
    }

    /**
     * 回写 Worker 失败状态，保持任务与运行事实一致。
     */
    @Transactional
    public void recordWorkerFailure(Long tenantId, String executionId, Long taskId,
                                    String taskStatus, Integer retryCount, String errorMessage) {
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, tenantId)
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) return;
        boolean terminal = ExecutionStatus.FAILED.equals(taskStatus);
        context.setTaskId(taskId);
        context.setRetryCount(retryCount == null ? 0 : retryCount);
        context.setStatus(terminal ? ExecutionStatus.FAILED : ExecutionStatus.QUEUED);
        context.setErrorCode(terminal ? "WORKER_TASK_FAILED" : "WORKER_TASK_RETRYING");
        context.setErrorMessage(errorMessage == null || errorMessage.isBlank() ? "异步任务执行失败。" : errorMessage);
        if (terminal) {
            context.setFinishedAt(LocalDateTime.now());
            context.setLeaseOwner(null);
            context.setLeaseUntil(null);
        }
        contextMapper.updateById(context);
        appendEvent(tenantId, executionId, context.getCurrentNodeId(), terminal ? "WORKER_TASK_FAILED" : "WORKER_TASK_RETRYING",
                latestSequence(tenantId, executionId) + 1,
                Map.of("taskId", taskId == null ? "" : taskId, "retryCount", context.getRetryCount(), "error", context.getErrorMessage()));
    }

    /**
     * 从失败运行创建独立重试运行。
     */
    @Transactional
    public Map<String, Object> enqueueRetry(SecurityUser user, PlatformExecutionContextEntity source) {
        requireUser(user);
        if (source == null || !user.getTenantId().equals(source.getTenantId()))
            throw new IllegalArgumentException("待重试运行不存在或不属于当前租户。");
        if (RunType.NODE_DEBUG.name().equals(source.getRunType()))
            throw new IllegalArgumentException("单节点调试运行不支持重试。");
        String executionId = UUID.randomUUID().toString();
        String idempotencyKey = source.getExecutionId() + ":RETRY:" + executionId;
        PlatformExecutionContextEntity retry = new PlatformExecutionContextEntity();
        retry.setTenantId(user.getTenantId());
        retry.setExecutionId(executionId);
        retry.setRequestId(source.getRequestId());
        retry.setTraceId(source.getTraceId());
        retry.setSpanId(UUID.randomUUID().toString());
        retry.setAppId(source.getAppId());
        retry.setVersionId(source.getVersionId());
        retry.setDraftRevisionId(source.getDraftRevisionId());
        retry.setDraftRevisionNo(source.getDraftRevisionNo());
        retry.setDraftGraphJson(source.getDraftGraphJson());
        retry.setRunType(source.getRunType());
        retry.setRuntimeSnapshotJson(source.getRuntimeSnapshotJson());
        retry.setRuntimeSnapshotHash(source.getRuntimeSnapshotHash());
        retry.setExecutionType(source.getExecutionType());
        retry.setIdempotencyKey(idempotencyKey);
        retry.setRetryOfExecutionId(source.getExecutionId());
        retry.setRetryAttempt((source.getRetryAttempt() == null ? 0 : source.getRetryAttempt()) + 1);
        retry.setRetryCount(0);
        retry.setStatus(ExecutionStatus.QUEUED);
        retry.setInputJson(source.getInputJson());
        contextMapper.insert(retry);
        Map<String, Object> payload = executionPayload(user, retry, retry.getAppId(), null, retry.getExecutionType(),
                idempotencyKey, null, null, parseJsonMap(retry.getInputJson()));
        String taskType = retry.getDraftGraphJson() == null ? TaskType.ORCHESTRATION_EXECUTION : TaskType.ORCHESTRATION_DRAFT_TEST;
        Long taskId = taskQueueService.enqueueOnce(user.getTenantId(), taskType, idempotencyKey, payload, MAX_NODE_RETRY_ATTEMPTS);
        retry.setTaskId(taskId);
        contextMapper.updateById(retry);
        return result(retry);
    }

    /**
     * 组装持久化任务队列载荷。
     */
    private Map<String, Object> executionPayload(SecurityUser user, PlatformExecutionContextEntity context,
                                                 Long appId, String versionId, String executionType,
                                                 String idempotencyKey, String conversationId, String messageId,
                                                 Map<String, Object> input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionId", context.getExecutionId());
        payload.put("appId", appId);
        payload.put("versionId", versionId);
        payload.put("executionType", executionType);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("conversationId", conversationId);
        payload.put("messageId", messageId);
        payload.put("input", input == null ? Map.of() : input);
        payload.put("userId", user.getUserId());
        payload.put("username", user.getUsername());
        payload.put("role", user.getRole());
        payload.put("requestId", context.getRequestId());
        payload.put("traceId", context.getTraceId());
        return payload;
    }

    private Map<String, Object> parseJsonMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(value, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("重试运行输入无法解析", exception);
        }
    }

    /**
     * 审批恢复也进入持久化队列。审批接口只完成状态决策，耗时的图调度由 Worker 执行，避免占用 HTTP 线程。
     */
    @Transactional
    public Map<String, Object> enqueueResume(SecurityUser user, String executionId, String decision) {
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) throw new IllegalArgumentException("未找到工作流执行上下文：" + executionId);
        if ("WAITING_APPROVAL".equals(context.getStatus())) {
            context.setApprovalDecision(decision);
            context.setStatus(ExecutionStatus.QUEUED);
            contextMapper.updateById(context);
        }
        if (!ExecutionStatus.QUEUED.equals(context.getStatus())) {
            throw new IllegalStateException("当前工作流状态不可提交恢复任务。");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionId", executionId);
        payload.put("decision", decision);
        payload.put("idempotencyKey", executionId + ":" + decision.toUpperCase());
        payload.put("userId", user.getUserId());
        payload.put("username", user.getUsername());
        payload.put("role", user.getRole());
        payload.put("requestId", context.getRequestId());
        payload.put("traceId", context.getTraceId());
        Long taskId = taskQueueService.enqueueOnce(user.getTenantId(), TaskType.ORCHESTRATION_RESUME,
                executionId + ":" + decision.toUpperCase(), payload, 3);
        context.setTaskId(taskId);
        contextMapper.updateById(context);
        return Map.of("executionId", executionId, "taskId", taskId, "status", ExecutionStatus.QUEUED);
    }

    /**
     * 暂停后的继续执行也必须进入持久化队列，避免控制请求线程直接运行图。
     */
    @Transactional
    public Map<String, Object> enqueueRecovery(SecurityUser user, String executionId, String reason) {
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) throw new IllegalArgumentException("未找到工作流执行上下文：" + executionId);
        if (!ExecutionStatus.QUEUED.equals(context.getStatus())) {
            throw new IllegalStateException("当前执行状态不可恢复：" + context.getStatus());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("executionId", executionId);
        payload.put("reason", reason);
        payload.put("idempotencyKey", executionId + ":" + reason);
        payload.put("userId", user.getUserId());
        payload.put("username", user.getUsername());
        payload.put("role", user.getRole());
        payload.put("requestId", context.getRequestId());
        payload.put("traceId", context.getTraceId());
        Long taskId = taskQueueService.enqueueOnce(user.getTenantId(), TaskType.ORCHESTRATION_RECOVERY,
                executionId + ":" + reason, payload, 3);
        context.setTaskId(taskId);
        contextMapper.updateById(context);
        return Map.of("executionId", executionId, "taskId", taskId, "status", ExecutionStatus.QUEUED);
    }

    /**
     * Worker 恢复入口只根据持久化执行实例加载版本和输入，禁止重新生成执行 ID。
     */
    public Map<String, Object> recover(SecurityUser user, String executionId) {
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) throw new IllegalArgumentException("未找到工作流执行上下文：" + executionId);
        try {
            Map<String, Object> input = context.getInputJson() == null || context.getInputJson().isBlank()
                    ? Map.of() : objectMapper.readValue(context.getInputJson(), Map.class);
            // 执行上下文保存的是版本表内部主键，执行引擎接收的是发布版本业务标识，不能直接转换成字符串。
            String releaseVersionId = resolveReleaseVersionId(user, context);
            return execute(user, context.getAppId(), releaseVersionId, context.getExecutionType(),
                    context.getIdempotencyKey(), null, null, input);
        } catch (Exception exception) {
            throw new IllegalStateException("恢复工作流执行失败：" + exception.getMessage(), exception);
        }
    }

    /**
     * 根据执行上下文解析发布版本业务标识。
     * 数据库关联字段使用内部主键，运行时合同使用 versionId 字符串，两者必须在边界处明确转换。
     */
    String resolveReleaseVersionId(SecurityUser user, PlatformExecutionContextEntity context) {
        if (context.getVersionId() == null) {
            throw new IllegalStateException("执行上下文缺少版本主键：" + context.getExecutionId());
        }
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getId, context.getVersionId())
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, context.getAppId())
                .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));
        if (version == null || version.getVersionId() == null || version.getVersionId().isBlank()) {
            throw new IllegalStateException("未找到执行上下文对应的已发布版本：" + context.getVersionId());
        }
        return version.getVersionId();
    }

    /**
     * 租约回收没有人工用户上下文，使用租户内置恢复身份入队，仍携带租户和执行实例边界。
     */
    @Transactional
    public Long enqueueLeaseRecovery(Long tenantId, String executionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, tenantId)
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        payload.put("executionId", executionId);
        payload.put("reason", RecoveryReason.LEASE_EXPIRED);
        payload.put("idempotencyKey", executionId + ":" + RecoveryReason.LEASE_EXPIRED);
        payload.put("userId", 0L);
        payload.put("username", "lease-recovery");
        payload.put("role", "SYSTEM");
        if (context != null) {
            payload.put("requestId", context.getRequestId());
            payload.put("traceId", context.getTraceId());
        }
        return taskQueueService.enqueueOnce(tenantId, TaskType.ORCHESTRATION_RECOVERY,
                executionId + ":" + RecoveryReason.LEASE_EXPIRED, payload, 3);
    }

    /**
     * 在事务内预建执行上下文。队列只负责调度，执行 ID 的归属和可追踪性由该记录保证。
     */
    private PlatformExecutionContextEntity createQueuedContext(SecurityUser user, Long appId, String versionId,
                                                               String executionType, String idempotencyKey,
                                                               Map<String, Object> input) {
        if (!ExecutionType.ALL.contains(executionType)) throw new IllegalArgumentException("不支持的执行类型。");
        OrchestrationVersionEntity version = versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId)
                .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED));
        if (version == null) throw new IllegalArgumentException("未找到已发布的版本。");
        authorizationService.require(user, appId, "RUN");
        PlatformExecutionContextEntity context = new PlatformExecutionContextEntity();
        context.setTenantId(user.getTenantId());
        context.setExecutionId(UUID.randomUUID().toString());
        context.setRequestId(RequestTraceContext.currentRequestId());
        context.setTraceId(RequestTraceContext.currentTraceId());
        context.setSpanId(UUID.randomUUID().toString());
        context.setAppId(appId);
        context.setVersionId(version.getId());
        context.setRunType(RunType.PRODUCTION.name());
        context.setRuntimeSnapshotJson(requireReleaseBundle(version));
        context.setRuntimeSnapshotHash(version.getReleaseBundleHash());
        context.setExecutionType(executionType);
        context.setIdempotencyKey(idempotencyKey);
        context.setStatus(ExecutionStatus.QUEUED);
        context.setInputJson(write(input == null ? Map.of() : input));
        contextMapper.insert(context);
        return context;
    }

    /**
     * 根据应用 Code 自动寻找线上生产环境绑定版本，并提交到统一的持久化执行队列。
     *
     * @param user           当前操作用户
     * @param appCode        应用编码
     * @param executionType  执行类型
     * @param conversationId 关联会话 ID
     * @param messageId      关联消息 ID
     * @param input          输入参数 Map
     * @return 执行结果或状态数据
     */
    public Map<String, Object> executePublishedByCode(SecurityUser user, String appCode, String executionType,
                                                      String conversationId, String messageId, Map<String, Object> input) {
        // 校验身份
        requireUser(user);
        // 按 app_code 查询状态为 PUBLISHED 的应用实体
        OrchestrationAppEntity app = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationAppEntity::getAppCode, appCode)
                .eq(OrchestrationAppEntity::getStatus, BusinessStatus.PUBLISHED));
        if (app == null) {
            throw new IllegalArgumentException("未找到已发布的编排应用：" + appCode);
        }
        // 查询该应用在 PRODUCTION 生产环境指针绑定的已发布版本
        OrchestrationEnvironmentEntity environment = environmentMapper.selectOne(new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                .eq(OrchestrationEnvironmentEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationEnvironmentEntity::getAppId, app.getId())
                .eq(OrchestrationEnvironmentEntity::getEnvironmentCode, "PRODUCTION"));
        if (environment == null || environment.getCurrentVersionId() == null || environment.getCurrentVersionId().isBlank()) {
            throw new IllegalArgumentException("该编排应用在生产环境未配置已发布版本。");
        }
        return execute(user, app.getId(), environment.getCurrentVersionId(), executionType,
                UUID.randomUUID().toString(), conversationId, messageId, input);
    }

    /**
     * 触发指定应用版本的单次同步执行（无对话 ID 上下文重载）
     *
     * @param user           当前操作用户
     * @param appId          应用 ID
     * @param versionId      已发布版本 ID
     * @param executionType  执行类型
     * @param idempotencyKey 幂等 Key
     * @param input          输入参数
     * @return 执行结果
     */
    public Map<String, Object> execute(SecurityUser user, Long appId, String versionId, String executionType,
                                       String idempotencyKey, Map<String, Object> input) {
        return execute(user, appId, versionId, executionType, idempotencyKey, null, null, input);
    }

    /**
     * 执行已经固化在执行上下文中的草稿图快照。
     */
    public Map<String, Object> executeDraftTest(SecurityUser user, Long appId, String executionId,
                                                String executionType, String idempotencyKey,
                                                String conversationId, String messageId,
                                                Map<String, Object> input) {
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId)
                .eq(PlatformExecutionContextEntity::getAppId, appId));
        if (context == null || context.getDraftGraphJson() == null || context.getDraftGraphJson().isBlank()) {
            throw new IllegalArgumentException("未找到可执行的草稿测试快照：" + executionId);
        }
        Map<String, Object> persistedInput = input == null ? Map.of() : input;
        try {
            if (context.getInputJson() != null && !context.getInputJson().isBlank()) {
                persistedInput = objectMapper.readValue(context.getInputJson(), Map.class);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("草稿测试输入快照解析失败。", exception);
        }
        return execute(user, appId, null, executionType, idempotencyKey, conversationId, messageId,
                persistedInput, context.getDraftGraphJson());
    }

    public Map<String, Object> execute(SecurityUser user, Long appId, String versionId, String executionType,
                                       String idempotencyKey, String conversationId, String messageId,
                                       Map<String, Object> input) {
        return execute(user, appId, versionId, executionType, idempotencyKey, conversationId, messageId, input, null);
    }

    /**
     * 正式版本和草稿测试共用同一调度器，差异仅在图来源：发布版本表或草稿快照。
     */
    public Map<String, Object> execute(SecurityUser user, Long appId, String versionId, String executionType,
                                       String idempotencyKey, String conversationId, String messageId,
                                       Map<String, Object> input, String draftGraphJson) {
        return execute(user, appId, versionId, executionType, idempotencyKey, conversationId, messageId,
                input, draftGraphJson, false);
    }

    /**
     * 实时运行显式开启模型增量；即时和后台运行不创建增量事件。
     */
    public Map<String, Object> execute(SecurityUser user, Long appId, String versionId, String executionType,
                                       String idempotencyKey, String conversationId, String messageId,
                                       Map<String, Object> input, String draftGraphJson,
                                       boolean streamModelOutput) {
        // 对外入口只负责转交执行，模型和工具调用不进入数据库长事务。
        return executeInternal(user, appId, versionId, executionType, idempotencyKey, conversationId, messageId,
                input, draftGraphJson, streamModelOutput);
    }

    /**
     * 执行已完成参数归一化的 Run，内部包含版本快照、节点调度和结果落库。
     */
    private Map<String, Object> executeInternal(SecurityUser user, Long appId, String versionId, String executionType,
                                                String idempotencyKey, String conversationId, String messageId,
                                                Map<String, Object> input, String draftGraphJson,
                                                boolean streamModelOutput) {
        // 生命周期入口只负责把一次运行交给图执行器，准备、遍历和收尾逻辑各自保持独立。
        return runGraphExecution(user, appId, versionId, executionType, idempotencyKey, conversationId, messageId,
                input, draftGraphJson, streamModelOutput);
    }

    /**
     * 图执行器：读取版本快照、推进节点、记录事件并持久化 Run 终态。
     * <p>这里刻意不使用长事务：模型和外部工具不属于数据库事务，节点状态与事件按步骤持久化，
     * 执行中断后才能依靠 Run 状态和事件继续恢复。</p>
     */
    private Map<String, Object> runGraphExecution(SecurityUser user, Long appId, String versionId, String executionType,
                                                  String idempotencyKey, String conversationId, String messageId,
                                                  Map<String, Object> input, String draftGraphJson,
                                                  boolean streamModelOutput) {
        PreparedExecution prepared = prepareExecution(user, appId, versionId, executionType, idempotencyKey,
                conversationId, messageId, input, draftGraphJson);
        if (prepared.completed()) return result(prepared.context());
        PlatformExecutionContextEntity context = prepared.context();
        String executionId = prepared.executionId();
        LocalDateTime now = prepared.startedAt();
        long sequence = 0;
        try {
            Map<String, Object> executionContext = prepared.executionContext();
            WorkflowVariableStore variableStore = new WorkflowVariableStore(prepared.values());
            Map<String, Object> values = variableStore.mutableState();
            context.setStateJson(write(executionContext));
            contextMapper.updateById(context);
            GraphPlan graphPlan = buildGraphPlan(prepared.graph());
            String current = prepared.currentNodeId() == null ? graphPlan.startNodeId() : prepared.currentNodeId();
            Set<String> visited = new HashSet<>(prepared.visitedNodes());
            WorkflowExecutionEngine.ExecutionState engineState = new WorkflowExecutionEngine.ExecutionState(
                    graphPlan.nodes(), graphPlan.next(), current, variableStore, visited, prepared.initialSequence());
            WorkflowExecutionEngine.ExecutionResult execution = workflowExecutionEngine.execute(engineState,
                    step -> executeWorkflowNode(user, appId, executionType, executionId, context, executionContext,
                            graphPlan, prepared.dependencies(), streamModelOutput, step));
            sequence = execution.sequence();
            if (execution.waiting()) return result(context);
            completeExecution(user, appId, conversationId, context, executionContext,
                    values, execution.output(), executionId, now, sequence);
        } catch (Exception ex) {
            failExecution(user, appId, context, executionId, now, sequence, ex);
        }
        return result(context);
    }

    /**
     * 单个节点步骤负责短状态写入、重试和控制指令处理，图遍历由统一引擎负责。
     */
    private WorkflowExecutionEngine.NodeStepResult executeWorkflowNode(
            SecurityUser user, Long appId, String executionType, String executionId,
            PlatformExecutionContextEntity context, Map<String, Object> executionContext,
            GraphPlan graphPlan, WorkflowDependencySnapshot dependencies, boolean streamModelOutput,
            WorkflowExecutionEngine.NodeStep step) {
        if (System.currentTimeMillis() > graphPlan.deadline()) throw new IllegalStateException("执行超时。");
        checkRunControl(user, executionId, context);

        JsonNode node = step.node();
        String nodeType = node.path("nodeType").asText(node.path("type").asText(""));
        RequestTraceContext.setNodeId(step.nodeId());
        persistNodePosition(context, executionContext, step);
        rememberCompensation(context, node);
        long sequence = step.sequence() + 1;
        appendEvent(user.getTenantId(), executionId, step.nodeId(), EVENT_NODE_STARTED, sequence,
                Map.of("nodeType", nodeType));

        RegisteredNodeExecution execution = executeNodeWithRetry(user, executionId, context, step,
                nodeType, dependencies, streamModelOutput, sequence);
        NodeExecutionOutcome outcome = execution.outcome();
        NodeFlowState flowState = new NodeFlowState(user, appId, executionType, executionId,
                step.nodeId(), node, graphPlan.nodes(), graphPlan.next(), graphPlan.incoming(),
                step.variables().mutableState(), step.visited(), step.pending(), step.branchValues(),
                step.branchResults(), dependencies, context, execution.sequence());
        ControlFlowResult control = applyControlSignal(outcome, flowState);
        long completedSequence = control.sequence();
        if (!control.waiting()) {
            appendEvent(user.getTenantId(), executionId, step.nodeId(), EVENT_NODE_SUCCEEDED,
                    ++completedSequence, nodeOutputPayload(control.output()));
        }
        return new WorkflowExecutionEngine.NodeStepResult(control.output(), outcome.selectedPort(),
                control.forcedNext(), control.waiting(), control.completed(), completedSequence);
    }

    private void checkRunControl(SecurityUser user, String executionId,
                                 PlatformExecutionContextEntity context) {
        PlatformExecutionContextEntity state = contextMapper.selectOne(
                new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                        .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                        .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (state != null && Boolean.TRUE.equals(state.getCancelRequested())) {
            throw new IllegalStateException("执行已取消。");
        }
        if (state != null && Boolean.TRUE.equals(state.getPauseRequested())) {
            context.setStatus("PAUSED");
            contextMapper.updateById(context);
            throw new IllegalStateException("执行已暂停。");
        }
    }

    private void persistNodePosition(PlatformExecutionContextEntity context,
                                     Map<String, Object> executionContext,
                                     WorkflowExecutionEngine.NodeStep step) {
        context.setCurrentNodeId(step.nodeId());
        Map<String, Object> contextVariables = executionContextAssembler.variables(executionContext);
        contextVariables.clear();
        contextVariables.putAll(step.variables().mutableState());
        executionContext.put("currentNodeId", step.nodeId());
        executionContext.put("visitedNodes", new ArrayList<>(step.visited()));
        context.setStateJson(write(executionContext));
        context.setHeartbeatAt(LocalDateTime.now());
        context.setLeaseUntil(LocalDateTime.now().plusMinutes(2));
        contextMapper.updateById(context);
    }

    private void rememberCompensation(PlatformExecutionContextEntity context, JsonNode node) {
        JsonNode compensation = node.path("config").path("compensation");
        if (!compensation.isObject()) return;
        context.setCompensationJson(compensation.toString());
        contextMapper.updateById(context);
    }

    private RegisteredNodeExecution executeNodeWithRetry(
            SecurityUser user, String executionId, PlatformExecutionContextEntity context,
            WorkflowExecutionEngine.NodeStep step, String nodeType,
            WorkflowDependencySnapshot dependencies, boolean streamModelOutput, long initialSequence) {
        long sequence = initialSequence;
        int nodeAttempt = 0;
        JsonNode retryPolicy = step.node().path("config").path("retryPolicy");
        int maxAttempts = boundedRetryAttempts(retryPolicy.path("maxAttempts").asInt(1));
        while (true) {
            try {
                return executeRegisteredNode(user, executionId, step.nodeId(), nodeType,
                        step.node().path("config"), step.variables(), step.visitCount(), dependencies,
                        streamModelOutput, sequence);
            } catch (Exception nodeError) {
                nodeAttempt++;
                if (nodeAttempt >= maxAttempts) {
                    throw new IllegalStateException(errorMessage(nodeError, "节点执行失败。"), nodeError);
                }
                context.setRetryCount((context.getRetryCount() == null ? 0 : context.getRetryCount()) + 1);
                context.setMaxAttempts(maxAttempts);
                contextMapper.updateById(context);
                long backoff = boundedRetryBackoff(retryPolicy.path("backoffMs").asLong(0), nodeAttempt);
                appendEvent(user.getTenantId(), executionId, step.nodeId(), EVENT_NODE_RETRY_SCHEDULED,
                        ++sequence, Map.of("attempt", nodeAttempt + 1, "maxAttempts", maxAttempts,
                                "backoffMs", backoff, "error", errorMessage(nodeError, "节点执行失败。")));
                sleepBeforeRetry(backoff, nodeError);
            }
        }
    }

    private void sleepBeforeRetry(long backoff, Exception nodeError) {
        if (backoff <= 0) return;
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(errorMessage(nodeError, "节点重试等待被中断。"), nodeError);
        }
    }

    /**
     * 成功收尾只负责快照、终态事件和对话助手消息，不参与节点调度。
     */
    private void completeExecution(SecurityUser user, Long appId, String conversationId,
                                   PlatformExecutionContextEntity context, Map<String, Object> executionContext,
                                   Map<String, Object> values, Object output, String executionId,
                                   LocalDateTime startedAt, long sequence) {
        context.setStatus(STATUS_SUCCEEDED);
        Map<String, Object> finalVariables = executionContextAssembler.variables(executionContext);
        finalVariables.clear();
        finalVariables.putAll(values);
        executionContextAssembler.complete(executionContext, output);
        context.setStateJson(write(executionContext));
        context.setOutputJson(write(output));
        context.setFinishedAt(LocalDateTime.now());
        context.setLeaseOwner(null);
        context.setLeaseUntil(null);
        contextMapper.updateById(context);
        appendEvent(user.getTenantId(), executionId, null, "EXECUTION_SUCCEEDED", sequence + 1,
                Map.of("status", context.getStatus(), "appId", appId));
        eventFactService.record(user.getTenantId(), user.getUserId(), "ORCHESTRATION_EXECUTION", "SUCCEEDED", null,
                java.time.Duration.between(startedAt, context.getFinishedAt()).toMillis(), null, null,
                Map.of("executionId", executionId, "appId", appId));
        recordAdoptionSuccess(user, appId, context, executionId);
        if (hasConversation(conversationId)) {
            appendMessage(user.getTenantId(), conversationId, UUID.randomUUID().toString(), executionId, "ASSISTANT", output);
        }
    }

    /**
     * 诊断事件会保留来源但不进入客户价值指标，避免健康检查污染采用率。
     */
    private void recordAdoptionSuccess(SecurityUser user, Long appId, PlatformExecutionContextEntity context, String executionId) {
        RunType runType = RunType.valueOf(context.getRunType());
        AdoptionEventSource source = runType == RunType.PRODUCTION ? AdoptionEventSource.PRODUCTION
                : (runType == RunType.DRAFT_TEST ? AdoptionEventSource.DRAFT_TEST : AdoptionEventSource.DIAGNOSTIC);
        AdoptionEventType type = runType == RunType.PRODUCTION ? AdoptionEventType.PRODUCTION_SUCCEEDED : AdoptionEventType.TEST_SUCCEEDED;
        adoptionAnalyticsService.record(new AdoptionAnalyticsService.EventCommand(user.getTenantId(), appId,
                context.getVersionId() == null ? null : String.valueOf(context.getVersionId()), executionId, type, source,
                "run-success:" + executionId, context.getFinishedAt(), null));
    }

    /**
     * 失败收尾统一映射取消、暂停、补偿失败和普通失败状态，并保留中文错误信息。
     */
    private void failExecution(SecurityUser user, Long appId, PlatformExecutionContextEntity context,
                               String executionId, LocalDateTime startedAt, long sequence, Exception exception) {
        long nextSequence = Math.max(sequence, latestSequence(user.getTenantId(), executionId)) + 1;
        appendEvent(user.getTenantId(), executionId, null, "EXECUTION_FAILED", nextSequence,
                Map.of("error", String.valueOf(exception.getMessage())));
        String failureMessage = String.valueOf(exception.getMessage());
        if (failureMessage.contains("执行已取消")) {
            context.setStatus("CANCELLED");
        } else if (failureMessage.contains("执行已暂停")) {
            context.setStatus("PAUSED");
        } else {
            context.setStatus("FAILED");
        }
        boolean compensationFailed = "FAILED".equals(context.getStatus()) && !runCompensation(user, context);
        context.setErrorCode(compensationFailed ? "COMPENSATION_FAILED"
                : ("FAILED".equals(context.getStatus()) ? "EXECUTION_FAILED" : context.getStatus()));
        context.setErrorMessage(exception.getMessage());
        context.setFinishedAt(LocalDateTime.now());
        context.setLeaseOwner(null);
        context.setLeaseUntil(null);
        contextMapper.updateById(context);
        eventFactService.record(user.getTenantId(), user.getUserId(), "ORCHESTRATION_EXECUTION", context.getStatus(), null,
                java.time.Duration.between(startedAt, context.getFinishedAt()).toMillis(), null, null,
                Map.of("executionId", executionId, "appId", appId, "error", String.valueOf(exception.getMessage())));
        throw new IllegalStateException(exception.getMessage(), exception);
    }

    /**
     * 准备一次执行所需的不可变版本快照、执行上下文和初始变量。
     * 该阶段不调用模型或外部工具，只完成边界校验和短状态写入。
     */
    private PreparedExecution prepareExecution(SecurityUser user, Long appId, String versionId,
                                               String executionType, String idempotencyKey,
                                               String conversationId, String messageId,
                                               Map<String, Object> input, String draftGraphJson) {
        requireUser(user);
        if (!ExecutionType.ALL.contains(executionType)) {
            throw new IllegalArgumentException("不支持的执行类型");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("幂等校验键不能为空");
        }
        PlatformExecutionContextEntity existing = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getIdempotencyKey, idempotencyKey));
        if (existing != null && !ExecutionStatus.QUEUED.equals(existing.getStatus())) {
            return PreparedExecution.completed(existing);
        }

        OrchestrationVersionEntity version = draftGraphJson == null
                ? versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId)
                .eq(OrchestrationVersionEntity::getStatus, BusinessStatus.PUBLISHED))
                : draftVersion(draftGraphJson);
        if (version == null) {
            throw new IllegalArgumentException("未找到可执行的已发布版本");
        }
        authorizationService.require(user, appId, "RUN");
        String runtimeGraphJson = resolveRuntimeGraphJson(existing, version, draftGraphJson);

        LocalDateTime startedAt = LocalDateTime.now();
        PlatformExecutionContextEntity context = existing == null ? new PlatformExecutionContextEntity() : existing;
        String executionId = existing == null ? UUID.randomUUID().toString() : existing.getExecutionId();
        if (existing == null) {
            context.setTenantId(user.getTenantId());
            context.setExecutionId(executionId);
            String reqId = RequestTraceContext.currentRequestId();
            context.setRequestId(reqId != null && !reqId.isBlank() ? reqId : "req-" + executionId.substring(0, 8));
            String trId = RequestTraceContext.currentTraceId();
            context.setTraceId(trId != null && !trId.isBlank() ? trId : "trace-" + executionId.substring(0, 8));
            String spId = RequestTraceContext.currentSpanId();
            context.setSpanId(spId != null && !spId.isBlank() ? spId : "span-" + UUID.randomUUID().toString().substring(0, 8));
            context.setAppId(appId);
            context.setVersionId(version.getId());
            context.setRunType(draftGraphJson == null ? RunType.PRODUCTION.name() : RunType.DRAFT_TEST.name());
            if (draftGraphJson != null) {
                context.setDraftGraphJson(draftGraphJson);
                OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                        .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                        .eq(OrchestrationDraftRevisionEntity::getAppId, appId)
                        .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
                if (draft != null) {
                    context.setDraftRevisionId(draft.getId());
                    context.setDraftRevisionNo(draft.getRevisionNo());
                }
            } else {
                context.setRuntimeSnapshotJson(version.getReleaseBundleJson());
                context.setRuntimeSnapshotHash(version.getReleaseBundleHash());
            }
            context.setExecutionType(executionType);
            context.setIdempotencyKey(idempotencyKey);
            context.setInputJson(write(input == null ? Map.of() : input));
            context.setStartedAt(startedAt);
        }
        context.setStatus(STATUS_RUNNING);
        context.setLeaseOwner("inline-" + executionId);
        context.setLeaseUntil(startedAt.plusMinutes(2));
        context.setHeartbeatAt(startedAt);
        if (existing == null) {
            contextMapper.insert(context);
        } else {
            contextMapper.updateById(context);
        }

        if (hasConversation(conversationId)) {
            upsertConversation(user, appId, version.getId(), conversationId);
            if (messageId != null && !messageId.isBlank()) {
                appendMessage(user.getTenantId(), conversationId, messageId, executionId, "USER", input);
            }
        }

        GraphDefinition graphDefinition = new com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser(objectMapper)
                .read(runtimeGraphJson);
        WorkflowDependencySnapshot dependencies = draftGraphJson == null
                ? readReleaseDependencies(context.getRuntimeSnapshotJson())
                : requireDraftDependencies(user.getTenantId(), graphDefinition);
        Map<String, Object> executionContext = executionContextAssembler.initialize(graphDefinition, input);
        Map<String, Object> values = new LinkedHashMap<>(executionContextAssembler.variables(executionContext));
        initializeConversationVariables(values, conversationId);
        if (hasConversation(conversationId)) {
            List<PlatformConversationMessageEntity> history = loadConversationHistory(user.getTenantId(), conversationId);
            values.put(WorkflowVariableNames.CONTEXT_CONVERSATION_HISTORY, history);
            executionContextAssembler.variables(executionContext)
                    .put(WorkflowVariableNames.CONTEXT_CONVERSATION_HISTORY, history);
        }
        JsonNode runtimeGraph = parseGraphJson(runtimeGraphJson);
        ResumePoint resumePoint = resolveResumePoint(context, runtimeGraph);
        if (resumePoint != null) {
            values.clear();
            values.putAll(resumePoint.values());
            Map<String, Object> contextValues = executionContextAssembler.variables(executionContext);
            contextValues.clear();
            contextValues.putAll(values);
            appendEvent(user.getTenantId(), executionId, resumePoint.approvalNodeId(), "APPROVAL_RESUMED",
                    resumePoint.sequence(), Map.of("decision", context.getApprovalDecision()));
        }
        return new PreparedExecution(context, executionId, startedAt, values, executionContext, false,
                runtimeGraph, dependencies, resumePoint == null ? null : resumePoint.nextNodeId(),
                resumePoint == null ? Set.of() : resumePoint.visitedNodes(),
                resumePoint == null ? 0L : resumePoint.sequence());
    }

    private ResumePoint resolveResumePoint(PlatformExecutionContextEntity context, JsonNode graph) {
        if (context.getApprovalDecision() == null || context.getApprovalDecision().isBlank()
                || context.getStateJson() == null || context.getStateJson().isBlank()) return null;
        try {
            JsonNode state = objectMapper.readTree(context.getStateJson());
            String approvalNodeId = state.path("currentNodeId").asText("");
            String decision = context.getApprovalDecision().toUpperCase(Locale.ROOT);
            String port = "APPROVE".equals(decision) ? "approved" : "rejected";
            String nextNodeId = null;
            for (JsonNode edge : graph.path("edges")) {
                if (approvalNodeId.equals(edge.path("sourceNodeId").asText())
                        && (port.equalsIgnoreCase(edge.path("sourcePort").asText())
                        || decision.equalsIgnoreCase(edge.path("sourcePort").asText()))) {
                    nextNodeId = edge.path("targetNodeId").asText(null);
                    break;
                }
            }
            if (nextNodeId == null && "APPROVE".equals(decision)) {
                nextNodeId = state.path("nextNodeIds").path(0).asText(null);
            }
            if (nextNodeId == null) throw new IllegalStateException("找不到审批决定对应的后续流程分支。");
            Map<String, Object> values = objectMapper.convertValue(state.path("values"), new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            Set<String> visited = new HashSet<>();
            state.path("visited").forEach(item -> visited.add(item.asText()));
            return new ResumePoint(approvalNodeId, nextNodeId, values, visited,
                    latestSequence(context.getTenantId(), context.getExecutionId()) + 1);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("人工审批恢复状态无法解析。", exception);
        }
    }

    private WorkflowDependencySnapshot requireDraftDependencies(Long tenantId, GraphDefinition graph) {
        WorkflowDependencyResolver.Resolution resolution = dependencyResolver.resolve(tenantId, graph);
        if (!resolution.valid()) {
            throw new IllegalArgumentException(resolution.issues().get(0).message());
        }
        return resolution.snapshot();
    }

    private WorkflowDependencySnapshot readReleaseDependencies(String bundleJson) {
        try {
            return objectMapper.readValue(bundleJson, WorkflowReleaseBundle.class).dependencies();
        } catch (Exception exception) {
            throw new IllegalStateException("运行快照中的依赖引用无法解析。", exception);
        }
    }

    /**
     * 生产 Run 只读取启动时复制到执行上下文的发布包，草稿测试只读取草稿快照。
     */
    private String resolveRuntimeGraphJson(PlatformExecutionContextEntity existing,
                                           OrchestrationVersionEntity version,
                                           String draftGraphJson) {
        if (draftGraphJson != null) return draftGraphJson;
        String bundleJson = existing != null && existing.getRuntimeSnapshotJson() != null
                ? existing.getRuntimeSnapshotJson() : requireReleaseBundle(version);
        String expectedHash = existing != null && existing.getRuntimeSnapshotHash() != null
                ? existing.getRuntimeSnapshotHash() : version.getReleaseBundleHash();
        if (!sha256(bundleJson).equals(expectedHash)) {
            throw new IllegalStateException("运行快照完整性校验失败，已停止执行。");
        }
        try {
            WorkflowReleaseBundle bundle = objectMapper.readValue(bundleJson, WorkflowReleaseBundle.class);
            return write(bundle.graph());
        } catch (Exception exception) {
            throw new IllegalStateException("运行快照无法解析，已停止执行。", exception);
        }
    }

    private String requireReleaseBundle(OrchestrationVersionEntity version) {
        if (version.getReleaseBundleJson() == null || version.getReleaseBundleJson().isBlank()
                || version.getReleaseBundleHash() == null || version.getReleaseBundleHash().isBlank()) {
            throw new IllegalStateException("发布版本缺少完整运行快照，请重新发布后再运行。");
        }
        if (!sha256(version.getReleaseBundleJson()).equals(version.getReleaseBundleHash())) {
            throw new IllegalStateException("发布版本快照完整性校验失败，请停止使用该版本。");
        }
        return version.getReleaseBundleJson();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("系统无法校验运行快照摘要。", exception);
        }
    }

    private boolean hasConversation(String conversationId) {
        return conversationId != null && !conversationId.isBlank();
    }

    /**
     * 只读取当前有效会话消息，归档消息仅用于审计，不参与模型上下文。
     */
    private List<PlatformConversationMessageEntity> loadConversationHistory(Long tenantId, String conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, tenantId)
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .ne(PlatformConversationMessageEntity::getStatus, BusinessStatus.ARCHIVED)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt));
    }

    private JsonNode parseGraphJson(String graphJson) {
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            if (root != null && root.isObject()) {
                JsonNode nodes = root.path("nodes");
                if (nodes.isArray()) {
                    for (JsonNode item : nodes) {
                        if (item instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
                            if (!objectNode.hasNonNull("nodeId") && objectNode.hasNonNull("id")) {
                                objectNode.put("nodeId", objectNode.get("id").asText());
                            }
                            if (!objectNode.hasNonNull("nodeType") && objectNode.hasNonNull("type")) {
                                objectNode.put("nodeType", objectNode.get("type").asText());
                            }
                            if (!objectNode.hasNonNull("title") && objectNode.hasNonNull("name")) {
                                objectNode.put("title", objectNode.get("name").asText());
                            }
                        }
                    }
                }
                JsonNode edges = root.path("edges");
                if (edges.isArray()) {
                    for (JsonNode item : edges) {
                        if (item instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
                            if (!objectNode.hasNonNull("sourceNodeId") && objectNode.hasNonNull("source")) {
                                objectNode.put("sourceNodeId", objectNode.get("source").asText());
                            }
                            if (!objectNode.hasNonNull("targetNodeId") && objectNode.hasNonNull("target")) {
                                objectNode.put("targetNodeId", objectNode.get("target").asText());
                            }
                        }
                    }
                }
            }
            return root;
        } catch (Exception exception) {
            throw new IllegalArgumentException("工作流图配置不是合法 JSON", exception);
        }
    }

    /**
     * 将图声明预编译为调度器需要的邻接关系，主循环不再处理图结构解析细节。
     */
    private GraphPlan buildGraphPlan(JsonNode graph) {
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        for (JsonNode node : graph.path("nodes")) {
            String nodeId = node.path("nodeId").asText(node.path("id").asText(""));
            if (!nodeId.isBlank()) {
                nodes.put(nodeId, node);
            }
        }
        Map<String, List<JsonNode>> next = new HashMap<>();
        Map<String, List<String>> incoming = new HashMap<>();
        for (JsonNode edge : graph.path("edges")) {
            String sourceNodeId = edge.path("sourceNodeId").asText(edge.path("source").asText(""));
            String targetNodeId = edge.path("targetNodeId").asText(edge.path("target").asText(""));
            if (!sourceNodeId.isBlank()) {
                next.computeIfAbsent(sourceNodeId, key -> new ArrayList<>()).add(edge);
            }
            if (!targetNodeId.isBlank() && !sourceNodeId.isBlank()) {
                incoming.computeIfAbsent(targetNodeId, key -> new ArrayList<>()).add(sourceNodeId);
            }
        }
        String startNodeId = nodes.entrySet().stream()
                .filter(entry -> "START".equalsIgnoreCase(entry.getValue().path("nodeType").asText(entry.getValue().path("type").asText(""))))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("工作流缺少开始节点"));
        long timeoutMs = graph.path("executionPolicy").path("timeoutMs").asLong(0);
        long deadline = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : Long.MAX_VALUE;
        return new GraphPlan(nodes, next, incoming, startNodeId, deadline);
    }

    private record GraphPlan(Map<String, JsonNode> nodes,
                             Map<String, List<JsonNode>> next,
                             Map<String, List<String>> incoming,
                             String startNodeId,
                             long deadline) {
    }

    private record PreparedExecution(PlatformExecutionContextEntity context,
                                     String executionId,
                                     LocalDateTime startedAt,
                                     Map<String, Object> values,
                                     Map<String, Object> executionContext,
                                     boolean completed,
                                     JsonNode graph,
                                     WorkflowDependencySnapshot dependencies,
                                     String currentNodeId,
                                     Set<String> visitedNodes,
                                     long initialSequence) {
        private static PreparedExecution completed(PlatformExecutionContextEntity context) {
            return new PreparedExecution(context, context.getExecutionId(), context.getStartedAt(),
                    Map.of(), Map.of(), true, null, WorkflowDependencySnapshot.empty(), null, Set.of(), 0L);
        }
    }

    private record ResumePoint(String approvalNodeId, String nextNodeId, Map<String, Object> values,
                               Set<String> visitedNodes, long sequence) {
    }

    /**
     * 用内存对象承载草稿图，避免把未发布内容写入正式版本表。
     */
    private OrchestrationVersionEntity draftVersion(String graphJson) {
        OrchestrationVersionEntity version = new OrchestrationVersionEntity();
        version.setGraphJson(graphJson);
        return version;
    }

    /**
     * 查询指定工作流执行上下文的明细及事件 Event 列表
     *
     * @param user        当前操作用户
     * @param executionId 工作流执行实例 ID
     * @return 包含事件序列列表的执行详情 Map
     */
    public Map<String, Object> get(SecurityUser user, String executionId) {
        // 校验身份
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) {
            throw new IllegalArgumentException("未找到工作流执行上下文：" + executionId);
        }
        Map<String, Object> result = result(context);
        // 原始事件保留给审计；页面展示使用与发布图一一对应的节点摘要，避免把开始/成功事件重复画成节点。
        List<PlatformExecutionEventEntity> events = eventMapper.selectList(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionEventEntity::getExecutionId, executionId)
                .orderByAsc(PlatformExecutionEventEntity::getSequenceNo));
        result.put("events", events);
        result.put("nodes", summarizeExecutionNodes(context, events));
        return result;
    }

    /**
     * latestConversationExecution 方法。
     *
     * @param user           user 参数
     * @param conversationId conversationId 参数
     * @return Map<String, Object> 返回对象
     */
    public Map<String, Object> latestConversationExecution(SecurityUser user, String conversationId) {
        requireUser(user);
        PlatformConversationMessageEntity message = messageMapper.selectOne(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, user.getTenantId())
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .isNotNull(PlatformConversationMessageEntity::getExecutionId)
                .orderByDesc(PlatformConversationMessageEntity::getCreatedAt)
                .last("LIMIT 1"));
        if (message == null || message.getExecutionId() == null || message.getExecutionId().isBlank()) return null;
        return get(user, message.getExecutionId());
    }

    /**
     * 返回 WebSocket 客户端断线重连后的持久化事件补发窗口。
     */
    public List<PlatformExecutionEventEntity> eventsAfter(SecurityUser user, String executionId, long afterSequence) {
        requireUser(user);
        get(user, executionId); // Enforce tenant ownership before returning audit events.
        return eventMapper.selectList(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionEventEntity::getExecutionId, executionId)
                .gt(PlatformExecutionEventEntity::getSequenceNo, Math.max(0L, afterSequence))
                .orderByAsc(PlatformExecutionEventEntity::getSequenceNo));
    }

    /**
     * 按运行链路维度查询持久化事件，供运行诊断页和审计查询共用。
     */
    public List<PlatformExecutionEventEntity> queryEvents(SecurityUser user, Long applicationId, Long versionId,
                                                          String executionId, Long taskId, String nodeId,
                                                          String errorCode, LocalDateTime from, LocalDateTime to) {
        requireUser(user);
        LambdaQueryWrapper<PlatformExecutionEventEntity> query = new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                .eq(applicationId != null, PlatformExecutionEventEntity::getApplicationId, applicationId)
                .eq(versionId != null, PlatformExecutionEventEntity::getVersionId, versionId)
                .eq(executionId != null && !executionId.isBlank(), PlatformExecutionEventEntity::getExecutionId, executionId)
                .eq(taskId != null, PlatformExecutionEventEntity::getTaskId, taskId)
                .eq(nodeId != null && !nodeId.isBlank(), PlatformExecutionEventEntity::getNodeId, nodeId)
                .eq(errorCode != null && !errorCode.isBlank(), PlatformExecutionEventEntity::getErrorCode, errorCode)
                .ge(from != null, PlatformExecutionEventEntity::getCreatedAt, from)
                .le(to != null, PlatformExecutionEventEntity::getCreatedAt, to)
                .orderByDesc(PlatformExecutionEventEntity::getCreatedAt)
                .last("LIMIT " + MAX_EVENT_QUERY_SIZE);
        return eventMapper.selectList(query);
    }

    /**
     * 受控读取单条事件原文，调用方必须具备租户管理员角色。
     */
    public Map<String, Object> rawEvent(SecurityUser user, Long eventId) {
        requireUser(user);
        if (!user.hasRole("ADMIN") && !user.hasRole("SUPER_ADMIN")) {
            throw new IllegalArgumentException("当前账号没有查看运行原文的审计权限。");
        }
        PlatformExecutionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionEventEntity::getId, eventId));
        if (event == null) throw new IllegalArgumentException("未找到指定运行事件。");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.getId());
        result.put("runId", event.getExecutionId());
        result.put("nodeId", event.getNodeId());
        result.put("eventType", event.getEventType());
        result.put("payloadJson", event.getPayloadJson());
        result.put("auditedRawAccess", true);
        return result;
    }

    /**
     * 人工审批只恢复持久化状态，后续节点仍由主执行引擎统一调度。
     */
    public Map<String, Object> resume(SecurityUser user, String executionId, String decision) {
        requireUser(user);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context == null) throw new IllegalArgumentException("未找到工作流执行上下文：" + executionId);
        String normalizedDecision = decision == null ? "" : decision.toUpperCase(Locale.ROOT);
        if (!ExecutionStatus.QUEUED.equals(context.getStatus())
                || !Set.of("APPROVE", "REJECT").contains(normalizedDecision)) {
            throw new IllegalStateException("当前工作流状态或审批决定不允许恢复执行。");
        }
        context.setApprovalDecision(normalizedDecision);
        contextMapper.updateById(context);
        try {
            return recover(user, executionId);
        } catch (Exception ex) {
            context.setStatus(ExecutionStatus.FAILED);
            context.setErrorCode("RESUME_FAILED");
            context.setErrorMessage(ex.getMessage() == null ? "人工审批恢复执行失败。" : ex.getMessage());
            context.setFinishedAt(LocalDateTime.now());
            contextMapper.updateById(context);
            appendEvent(user.getTenantId(), executionId, context.getCurrentNodeId(), "EXECUTION_FAILED",
                    latestSequence(user.getTenantId(), executionId) + 1,
                    Map.of("error", context.getErrorMessage(), "resumed", true));
            throw new IllegalStateException(context.getErrorMessage(), ex);
        }
    }

    /**
     * 单节点独立单步调试（不依赖完整图执行记录）
     *
     * @param user     当前操作用户
     * @param appId    应用 ID
     * @param nodeType 调试节点类型
     * @param config   节点配置 JSON
     * @param input    输入变量 Map
     * @return 调试输出结果与状态 Map
     */
    public NodeDebugResult debugNode(SecurityUser user, Long appId, String nodeType, JsonNode config, Map<String, Object> input) {
        requireUser(user);
        if (appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationAppEntity::getId, appId)) == null)
            throw new IllegalArgumentException("未找到编排应用。");
        PlatformExecutionContextEntity context = new PlatformExecutionContextEntity();
        context.setTenantId(user.getTenantId());
        context.setExecutionId(UUID.randomUUID().toString());
        context.setRequestId(RequestTraceContext.currentRequestId());
        context.setTraceId(RequestTraceContext.currentTraceId());
        context.setSpanId(UUID.randomUUID().toString());
        context.setAppId(appId);
        context.setRunType(RunType.NODE_DEBUG.name());
        context.setExecutionType(ExecutionType.APPLICATION_WORKFLOW);
        context.setIdempotencyKey("node-debug:" + context.getExecutionId());
        context.setStatus(STATUS_RUNNING);
        context.setInputJson(write(input == null ? Map.of() : input));
        context.setStartedAt(LocalDateTime.now());
        contextMapper.insert(context);
        try {
            WorkflowVariableStore variables = new WorkflowVariableStore(canonicalInput(input));
            JsonNode nodeConfig = config == null ? objectMapper.createObjectNode() : config;
            GraphDefinition debugGraph = new GraphDefinition("APPLICATION_WORKFLOW", "1", null, null,
                    List.of(new com.acme.agentstudio.domain.workflow.model.GraphNode(
                            context.getExecutionId(), nodeType, "节点调试", 0D, 0D, nodeConfig, null, null)),
                    List.of(), List.of());
            WorkflowDependencySnapshot dependencies = requireDraftDependencies(user.getTenantId(), debugGraph);
            NodeExecutionOutcome executionOutcome = executeRegisteredNode(new NodeExecutionRequest(
                    user, context.getExecutionId(), nodeType, nodeConfig, variables.snapshot(), 1,
                    dependencies, LlmNodeExecutor.ModelDeltaListener.NOOP));
            variables.apply(executionOutcome.variablePatch());
            Object output = executionOutcome.output();
            context.setStatus(STATUS_SUCCEEDED);
            context.setOutputJson(write(output));
            context.setFinishedAt(LocalDateTime.now());
            contextMapper.updateById(context);
            appendEvent(user.getTenantId(), context.getExecutionId(), null, EVENT_NODE_SUCCEEDED, 1,
                    Map.of("nodeType", nodeType));
            return new NodeDebugResult(context.getExecutionId(), RunType.NODE_DEBUG,
                    STATUS_SUCCEEDED, nodeType, output, null);
        } catch (Exception ex) {
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "节点调试执行失败。" : ex.getMessage();
            context.setStatus(ExecutionStatus.FAILED);
            context.setErrorCode("NODE_DEBUG_FAILED");
            context.setErrorMessage(message);
            context.setFinishedAt(LocalDateTime.now());
            contextMapper.updateById(context);
            appendEvent(user.getTenantId(), context.getExecutionId(), null, "EXECUTION_FAILED", 1,
                    Map.of("nodeType", nodeType, "error", message));
            return new NodeDebugResult(context.getExecutionId(), RunType.NODE_DEBUG,
                    ExecutionStatus.FAILED, nodeType, null, message);
        }
    }

    /**
     * 查询指定租户的最新 100 条工作流执行历史记录
     *
     * @param user 当前操作用户
     * @return 执行记录 Map 列表
     */
    public List<Map<String, Object>> list(SecurityUser user) {
        return list(user, DEFAULT_EXECUTION_PAGE_SIZE, 0);
    }

    /**
     * 只返回运行摘要，详情和事件由用户选择记录后按需读取。
     */
    public List<Map<String, Object>> list(SecurityUser user, int limit, int offset) {
        requireUser(user);
        int pageSize = Math.max(1, Math.min(MAX_EXECUTION_PAGE_SIZE, limit));
        int pageOffset = Math.max(0, offset);
        long currentPage = (pageOffset / pageSize) + 1L;
        return contextMapper.selectPage(new Page<>(currentPage, pageSize), new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                        .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                        .orderByDesc(PlatformExecutionContextEntity::getStartedAt))
                .getRecords()
                .stream().map(this::result).toList();
    }

    private Object executeIterationBody(SecurityUser user, Map<String, JsonNode> nodes, String bodyNodeId,
                                        Map<String, Object> values, WorkflowDependencySnapshot dependencies) {
        JsonNode body = nodes.get(bodyNodeId);
        if (body == null) throw new IllegalArgumentException("未找到迭代循环体节点：" + bodyNodeId);
        String type = body.path("nodeType").asText(body.path("type").asText(""));
        // 迭代体与普通节点使用同一策略，避免循环内部再维护一套节点分支。
        WorkflowVariableStore variables = new WorkflowVariableStore(values);
        NodeExecutionOutcome outcome = executeRegisteredNode(new NodeExecutionRequest(
                user, bodyNodeId, type, body.path("config"), variables.snapshot(), 1,
                dependencies, LlmNodeExecutor.ModelDeltaListener.NOOP));
        variables.apply(outcome.variablePatch());
        return outcome.output();
    }

    private Map<String, Object> canonicalInput(Map<String, Object> input) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (input != null) input.forEach((key, value) -> values.put("input." + key, value));
        return values;
    }

    private long latestSequence(Long tenantId, String executionId) {
        PlatformExecutionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                .eq(PlatformExecutionEventEntity::getTenantId, tenantId)
                .eq(PlatformExecutionEventEntity::getExecutionId, executionId)
                .orderByDesc(PlatformExecutionEventEntity::getSequenceNo)
                .last("LIMIT 1"));
        return event == null || event.getSequenceNo() == null ? 0L : event.getSequenceNo();
    }

    private boolean runCompensation(SecurityUser user, PlatformExecutionContextEntity context) {
        if (context.getCompensationJson() == null || context.getCompensationJson().isBlank()) return true;
        try {
            JsonNode spec = objectMapper.readTree(context.getCompensationJson());
            String type = spec.path("nodeType").asText(spec.path("type").asText(""));
            JsonNode config = spec.path("config").isObject() ? spec.path("config") : spec;
            NodeExecutionOutcome outcome = handlerRegistry.execute(new NodeExecutionRequest(
                    user, context.getCurrentNodeId() + ":compensation", type, config,
                    new NodeInputValues(Map.of()), 1, WorkflowDependencySnapshot.empty(),
                    LlmNodeExecutor.ModelDeltaListener.NOOP));
            appendEvent(user.getTenantId(), context.getExecutionId(), context.getCurrentNodeId(), "COMPENSATION_SUCCEEDED",
                    latestSequence(user.getTenantId(), context.getExecutionId()) + 1,
                    compensationOutputPayload(type, outcome.output()));
            context.setCompensationJson(write(Map.of("status", ExecutionStatus.SUCCEEDED)));
            return true;
        } catch (Exception ex) {
            appendEvent(user.getTenantId(), context.getExecutionId(), context.getCurrentNodeId(), "COMPENSATION_FAILED",
                    latestSequence(user.getTenantId(), context.getExecutionId()) + 1,
                    Map.of("error", ex.getMessage() == null ? "补偿节点执行失败。" : ex.getMessage()));
            return false;
        }
    }

    private void appendEvent(Long tenantId, String executionId, String nodeId, String type, long sequence, Object payload) {
        PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
        event.setTenantId(tenantId);
        event.setExecutionId(executionId);
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, tenantId)
                .eq(PlatformExecutionContextEntity::getExecutionId, executionId));
        if (context != null) {
            event.setRequestId(context.getRequestId());
            event.setTraceId(context.getTraceId());
            event.setTaskId(context.getTaskId());
            event.setApplicationId(context.getAppId());
            event.setVersionId(context.getVersionId());
            event.setDurationMs(context.getStartedAt() == null ? null
                    : java.time.Duration.between(context.getStartedAt(), LocalDateTime.now()).toMillis());
        }
        event.setSpanId(RequestTraceContext.currentSpanId());
        event.setNodeId(nodeId);
        event.setEventType(type);
        long latest = latestSequence(tenantId, executionId);
        long actualSequence = sequence <= latest ? latest + 1 : sequence;
        event.setSequenceNo(actualSequence);
        String payloadJson = write(payload);
        event.setPayloadJson(payloadJson);
        event.setSummaryJson(dataMasker.maskJson(payloadJson));
        event.setStatus(eventStatus(type));
        if (type.contains("FAILED")) {
            event.setErrorCode(type);
            event.setErrorMessage(extractEventError(payload));
        }
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
        eventPublisher.publish(event);
    }

    private LlmNodeExecutor.ModelDeltaListener modelDeltaListener(SecurityUser user, String executionId,
                                                                  String nodeId, boolean enabled) {
        if (!enabled) {
            return LlmNodeExecutor.ModelDeltaListener.NOOP;
        }
        return delta -> appendEvent(user.getTenantId(), executionId, nodeId, EVENT_MODEL_DELTA,
                latestSequence(user.getTenantId(), executionId) + 1, Map.of("delta", delta));
    }

    /**
     * 将模型策略返回的追踪信息转换为统一 Run 事件，并返回最新序号。
     */
    private long appendModelTrace(SecurityUser user, String executionId, String nodeId,
                                  NodeExecutionTrace trace, long sequence) {
        if (trace == null) return sequence;
        appendEvent(user.getTenantId(), executionId, nodeId, "MODEL_ATTEMPTS", ++sequence,
                Map.of("modelKey", trace.modelKey(), "attempt", trace.attempt()));
        appendEvent(user.getTenantId(), executionId, nodeId, EVENT_LLM_CONTEXT_RESOLVED, ++sequence,
                Map.of("contextUsage", trace.contextUsage()));
        return sequence;
    }

    private Map<String, Object> nodeOutputPayload(Object output) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("output", output);
        return payload;
    }

    private Map<String, Object> compensationOutputPayload(String nodeType, Object output) {
        Map<String, Object> payload = nodeOutputPayload(output);
        payload.put("nodeType", nodeType);
        return payload;
    }

    /**
     * 普通节点统一经过注册表执行，并在同一处处理模型追踪事件。
     */
    private RegisteredNodeExecution executeRegisteredNode(SecurityUser user, String executionId,
                                                          String nodeId, String nodeType, JsonNode config,
                                                          WorkflowVariableStore variables, int visitCount,
                                                          WorkflowDependencySnapshot dependencies,
                                                          boolean streamOutput,
                                                          long sequence) {
        NodeExecutionRequest request = new NodeExecutionRequest(
                user, nodeId, nodeType, config, variables.snapshot(), visitCount, dependencies,
                modelDeltaListener(user, executionId, nodeId, streamOutput));
        NodeExecutionOutcome outcome = executeRegisteredNode(request);
        variables.apply(outcome.variablePatch());
        long latestSequence = appendModelTrace(user, executionId, nodeId, outcome.trace(), sequence);
        return new RegisteredNodeExecution(outcome, latestSequence);
    }

    private NodeExecutionOutcome executeRegisteredNode(NodeExecutionRequest request) {
        return handlerRegistry.execute(request);
    }

    private record RegisteredNodeExecution(NodeExecutionOutcome outcome, long sequence) {
    }

    /**
     * 将事件类型映射为稳定运行状态，供列表筛选和故障诊断使用。
     */
    private String eventStatus(String type) {
        if (type == null) return "UNKNOWN";
        if (type.contains("FAILED")) return ExecutionStatus.FAILED;
        if (type.contains("RETRY")) return "RETRYING";
        if (type.contains("SUCCEEDED")) return ExecutionStatus.SUCCEEDED;
        if (type.contains("CANCELLED")) return ExecutionStatus.CANCELLED;
        return ExecutionStatus.RUNNING;
    }

    /**
     * 从结构化事件载荷提取中文错误，不把堆栈直接暴露给前端。
     */
    private String extractEventError(Object payload) {
        if (payload instanceof Map<?, ?> map && map.get("error") != null) {
            return String.valueOf(map.get("error"));
        }
        return "运行节点执行失败，请查看关联运行详情。";
    }

    private void upsertConversation(SecurityUser user, Long appId, Long versionId, String conversationId) {
        PlatformConversationEntity conversation = conversationMapper.selectOne(new LambdaQueryWrapper<PlatformConversationEntity>()
                .eq(PlatformConversationEntity::getTenantId, user.getTenantId())
                .eq(PlatformConversationEntity::getConversationId, conversationId));
        if (conversation == null) {
            conversation = new PlatformConversationEntity();
            conversation.setTenantId(user.getTenantId());
            conversation.setConversationId(conversationId);
            conversation.setAppId(appId);
            conversation.setVersionId(versionId);
            conversation.setStatus(BusinessStatus.ACTIVE);
            conversation.setCreatedBy(user.getUserId());
            conversation.setCreatedAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.insert(conversation);
            return;
        }
        if (!appId.equals(conversation.getAppId())) {
            throw new IllegalArgumentException("会话不属于指定应用，无法继续运行");
        }
        if (conversation.getVersionId() != null && !versionId.equals(conversation.getVersionId())) {
            throw new IllegalArgumentException("会话已绑定其他发布版本，请开启新会话后继续对话");
        }
        if (conversation.getVersionId() == null) {
            conversation.setVersionId(versionId);
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    private void appendMessage(Long tenantId, String conversationId, String messageId, String executionId, String role, Object content) {
        PlatformConversationMessageEntity message = new PlatformConversationMessageEntity();
        message.setTenantId(tenantId);
        message.setConversationId(conversationId);
        message.setMessageId(messageId);
        message.setExecutionId(executionId);
        message.setRoleCode(role);
        message.setContentJson(write(content));
        message.setStatus("COMPLETED");
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private void initializeConversationVariables(Map<String, Object> values, String conversationId) {
        Object message = values.get(WorkflowVariableNames.VARIABLE_USER_MESSAGE);
        if (message == null) message = values.get(WorkflowVariableNames.VARIABLE_INPUT);
        if (message == null) message = values.get(WorkflowVariableNames.VARIABLE_QUERY);
        if (message != null) {
            values.putIfAbsent(WorkflowVariableNames.VARIABLE_INPUT, message);
            values.putIfAbsent(WorkflowVariableNames.VARIABLE_USER_MESSAGE, message);
            values.putIfAbsent(WorkflowVariableNames.VARIABLE_QUERY, message);
        }
        if (conversationId != null && !conversationId.isBlank()) values.putIfAbsent("conversationId", conversationId);
    }

    /**
     * A HUMAN node is represented by the same approval work order used by the organization approval center.
     * The execution ID is the compatibility boundary: new work orders resume this engine, old work orders stay legacy.
     */
    private Long createApprovalTask(SecurityUser user, String executionId, String nodeId, JsonNode config,
                                    Map<String, Object> values) {
        String approvalGroup = config.path("approvalGroup").asText("").trim();

        String input = String.valueOf(values.getOrDefault(WorkflowVariableNames.VARIABLE_USER_MESSAGE,
                values.getOrDefault(WorkflowVariableNames.VARIABLE_INPUT, "")));
        String title = config.path("approvalTitle").asText("").trim();
        ApprovalTaskEntity task = new ApprovalTaskEntity();
        task.setTenantId(user.getTenantId());
        task.setTitle(title.isBlank() ? "工作流人工审批" : title);
        task.setOwnerTeam(approvalGroup.isBlank() ? "管理员审批" : approvalGroup);
        task.setRiskLevel(config.path("riskLevel").asText("HIGH"));
        task.setApprovalStatus("PENDING");
        task.setPayloadJson(write(Map.of(
                "engine", "ORCHESTRATION",
                "executionId", executionId,
                "nodeId", nodeId,
                "businessRequest", input,
                "approvalDescription", config.path("approvalDescription").asText("")
        )));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        approvalTaskMapper.insert(task);
        return task.getId();
    }

    private List<Map<String, Object>> summarizeExecutionNodes(PlatformExecutionContextEntity context,
                                                              List<PlatformExecutionEventEntity> events) {
        try {
            OrchestrationVersionEntity version = context.getVersionId() == null ? null : versionMapper.selectById(context.getVersionId());
            String graphJson = null;
            try {
                graphJson = resolveRuntimeGraphJson(context, version, context.getDraftGraphJson());
            } catch (Exception ignored) {
                // 快照未找到时尝试回退到应用当前草稿图
            }
            if ((graphJson == null || graphJson.isBlank()) && context.getAppId() != null) {
                OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                        .eq(OrchestrationDraftRevisionEntity::getTenantId, context.getTenantId())
                        .eq(OrchestrationDraftRevisionEntity::getAppId, context.getAppId())
                        .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
                if (draft != null) {
                    graphJson = draft.getGraphJson();
                }
            }
            Map<String, Map<String, Object>> summaries = new LinkedHashMap<>();
            if (graphJson != null && !graphJson.isBlank()) {
                for (JsonNode node : objectMapper.readTree(graphJson).path("nodes")) {
                    String nodeId = node.path("nodeId").asText(node.path("id").asText(""));
                    if (nodeId.isBlank()) continue;
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("id", nodeId);
                    summary.put("nodeId", nodeId);
                    summary.put("nodeType", node.path("nodeType").asText(node.path("type").asText("")));
                    summary.put("name", node.path("title").asText(node.path("name").asText(nodeId)));
                    summary.put("status", "PENDING");
                    summary.put("attempt", 0);
                    summaries.put(nodeId, summary);
                }
            }

            // 若无图结构，从实际记录的事件中回溯节点执行序列，保证节点数量不为 0
            if (events != null) {
                for (PlatformExecutionEventEntity event : events) {
                    String nid = event.getNodeId();
                    if (nid == null || nid.isBlank() || "graph".equals(nid)) continue;
                    if (!summaries.containsKey(nid)) {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("id", nid);
                        s.put("nodeId", nid);
                        String nodeType = "NODE";
                        try {
                            JsonNode p = objectMapper.readTree(event.getPayloadJson());
                            if (p.hasNonNull("nodeType")) nodeType = p.get("nodeType").asText();
                        } catch (Exception ignored) {}
                        s.put("nodeType", nodeType);
                        s.put("name", nid);
                        s.put("status", "PENDING");
                        s.put("attempt", 0);
                        summaries.put(nid, s);
                    }
                }
            }

            for (PlatformExecutionEventEntity event : (events == null ? List.<PlatformExecutionEventEntity>of() : events)) {
                Map<String, Object> summary = summaries.get(event.getNodeId());
                if (summary == null) continue;
                String type = event.getEventType();
                if ("NODE_STARTED".equals(type)) {
                    summary.put("status", "RUNNING");
                    summary.put("startedAt", event.getCreatedAt());
                    summary.put("attempt", ((Number) summary.get("attempt")).intValue() + 1);
                } else if ("NODE_SUCCEEDED".equals(type)) {
                    summary.put("status", "COMPLETED");
                    summary.put("finishedAt", event.getCreatedAt());
                    if (!Set.of("START", "END", "USER_INPUT").contains(String.valueOf(summary.get("nodeType")))) {
                        summary.put("outputSummary", eventPayloadValue(event, "output"));
                    }
                } else if ("WAITING_APPROVAL".equals(type)) {
                    summary.put("status", "WAITING_APPROVAL");
                    summary.put("outputSummary", event.getPayloadJson());
                } else if ("NODE_RETRY_SCHEDULED".equals(type)) {
                    summary.put("status", "RETRYING");
                    summary.put("traceJson", event.getPayloadJson());
                }
            }
            if (context.getCurrentNodeId() != null && summaries.containsKey(context.getCurrentNodeId())
                    && "RUNNING".equals(context.getStatus())) {
                summaries.get(context.getCurrentNodeId()).put("status", "RUNNING");
            }
            Map<String, Object> inputNode = summaries.values().stream()
                    .filter(node -> "USER_INPUT".equals(node.get("nodeType"))).findFirst().orElse(null);
            if (inputNode != null) inputNode.put("inputSummary", context.getInputJson());
            return new ArrayList<>(summaries.values());
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String eventPayloadValue(PlatformExecutionEventEntity event, String field) {
        try {
            JsonNode value = objectMapper.readTree(event.getPayloadJson()).path(field);
            return value.isMissingNode() || value.isNull() ? "" : value.isTextual() ? value.asText() : value.toString();
        } catch (Exception ex) {
            return event.getPayloadJson();
        }
    }

    /**
     * Keep platform safety limits in one place so node configuration cannot create an unbounded run.
     */
    private int boundedLoopIterations(int requested) {
        return Math.min(MAX_LOOP_ITERATIONS, Math.max(1, requested));
    }

    private ControlFlowResult applyControlSignal(NodeExecutionOutcome outcome, NodeFlowState state) {
        return switch (outcome.controlSignal()) {
            case FORK -> forkBranches(state);
            case ITERATE -> runIteration(state);
            case JOIN_WAIT -> joinBranches(state);
            case CHILD_RUN -> runChildWorkflow(state);
            case WAIT_APPROVAL -> waitForApproval(state);
            case COMPLETE -> new ControlFlowResult(outcome.output(), null, false, true, state.sequence());
            case CONTINUE, SELECT_PORT -> new ControlFlowResult(
                    outcome.output(), null, false, false, state.sequence());
        };
    }

    private ControlFlowResult forkBranches(NodeFlowState state) {
        long sequence = state.sequence();
        for (JsonNode edge : state.next().getOrDefault(state.nodeId(), List.of())) {
            String target = edge.path("targetNodeId").asText();
            state.pending().addLast(target);
            state.branchValues().put(target, new LinkedHashMap<>(state.values()));
            appendEvent(state.user().getTenantId(), state.executionId(), target,
                    "PARALLEL_BRANCH_STARTED", ++sequence, Map.of("parallelNodeId", state.nodeId()));
        }
        return new ControlFlowResult(Map.of("branchCount", state.pending().size()),
                state.pending().pollFirst(), false, false, sequence);
    }

    private ControlFlowResult runIteration(NodeFlowState state) {
        JsonNode config = state.node().path("config");
        Object raw = state.values().get(config.path("inputReference").asText(""));
        if (!(raw instanceof List<?> items)) throw new IllegalArgumentException("迭代输入必须是数组。");
        String bodyNodeId = config.path("bodyNodeId").asText("");
        if (bodyNodeId.isBlank()) throw new IllegalArgumentException("迭代节点必须配置循环体节点。");
        int maxItems = Math.min(MAX_LOOP_ITERATIONS, Math.max(1, config.path("maxItems").asInt(items.size())));
        IterationErrorStrategy errorStrategy = IterationErrorStrategy.parse(config.path("errorStrategy").asText());
        IterationResult iteration = executeIterationItems(state, items, bodyNodeId, maxItems, errorStrategy);
        state.values().put("nodes." + state.nodeId() + ".output", iteration.outputs());
        return new ControlFlowResult(iteration.outputs(), null, false, false, iteration.sequence());
    }

    private IterationResult executeIterationItems(NodeFlowState state, List<?> items, String bodyNodeId,
                                                  int maxItems, IterationErrorStrategy errorStrategy) {
        List<Object> outputs = new ArrayList<>();
        long sequence = state.sequence();
        for (int index = 0; index < Math.min(items.size(), maxItems); index++) {
            Map<String, Object> itemValues = new LinkedHashMap<>(state.values());
            String itemVariable = state.node().path("config").path("itemVariable").asText("item");
            itemValues.put("variables." + itemVariable, items.get(index));
            appendEvent(state.user().getTenantId(), state.executionId(), state.nodeId(),
                    "ITERATION_ITEM_STARTED", ++sequence, Map.of("index", index));
            try {
                outputs.add(executeIterationBody(state.user(), state.nodes(), bodyNodeId, itemValues,
                        state.dependencies()));
                appendEvent(state.user().getTenantId(), state.executionId(), state.nodeId(),
                        "ITERATION_ITEM_SUCCEEDED", ++sequence, Map.of("index", index));
            } catch (Exception exception) {
                appendEvent(state.user().getTenantId(), state.executionId(), state.nodeId(),
                        "ITERATION_ITEM_FAILED", ++sequence,
                        Map.of("index", index, "error", errorMessage(exception, "迭代项执行失败。")));
                if (errorStrategy == IterationErrorStrategy.FAIL_FAST) {
                    throw new IllegalStateException(errorMessage(exception, "迭代项执行失败。"), exception);
                }
                outputs.add(null);
            }
        }
        return new IterationResult(outputs, sequence);
    }

    private ControlFlowResult joinBranches(NodeFlowState state) {
        List<String> predecessors = state.incoming().getOrDefault(state.nodeId(), List.of());
        if (!state.visited().containsAll(predecessors)) {
            String forcedNext = state.pending().pollFirst();
            if (forcedNext == null) throw new IllegalStateException("并行汇聚分支无法恢复执行。");
            Object waiting = Map.of("waitingFor",
                    predecessors.stream().filter(item -> !state.visited().contains(item)).toList());
            return new ControlFlowResult(waiting, forcedNext, false, false, state.sequence());
        }
        Map<String, Object> output = new LinkedHashMap<>();
        for (String predecessor : predecessors) {
            output.put(predecessor, state.branchResults().getOrDefault(predecessor, Map.of()));
        }
        state.values().put("nodes." + state.nodeId() + ".branches", output);
        state.values().put("nodes." + state.nodeId() + ".output", output);
        return new ControlFlowResult(output, null, false, false, state.sequence());
    }

    private ControlFlowResult runChildWorkflow(NodeFlowState state) {
        String nestedVersion = state.node().path("config").path("versionId").asText("");
        if (nestedVersion.isBlank()) throw new IllegalArgumentException("子流程节点必须提供发布版本引用。");
        Object output = execute(state.user(), state.appId(), nestedVersion, state.executionType(),
                state.executionId() + ":" + state.nodeId(), null, null, state.values());
        state.values().put("nodes." + state.nodeId() + ".output", output);
        return new ControlFlowResult(output, null, false, false, state.sequence());
    }

    private ControlFlowResult waitForApproval(NodeFlowState state) {
        Long taskId = createApprovalTask(state.user(), state.executionId(), state.nodeId(),
                state.node().path("config"), state.values());
        Map<String, Object> output = Map.of(
                "approvalTaskId", taskId,
                "approvalTitle", state.node().path("config").path("approvalTitle").asText("待审批"),
                "status", STATUS_WAITING_APPROVAL);
        List<String> nextNodeIds = state.next().getOrDefault(state.nodeId(), List.of()).stream()
                .map(edge -> edge.path("targetNodeId").asText()).toList();
        state.context().setStateJson(write(Map.of("currentNodeId", state.nodeId(), "nextNodeIds", nextNodeIds,
                "approvalTaskId", taskId, "visited", state.visited(), "values", state.values())));
        state.context().setStatus(STATUS_WAITING_APPROVAL);
        state.context().setOutputJson(write(output));
        state.context().setHeartbeatAt(LocalDateTime.now());
        contextMapper.updateById(state.context());
        long sequence = state.sequence() + 1;
        appendEvent(state.user().getTenantId(), state.executionId(), state.nodeId(),
                "WAITING_APPROVAL", sequence, output);
        return new ControlFlowResult(output, null, true, false, sequence);
    }

    private String errorMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? fallback : exception.getMessage();
    }

    private record NodeFlowState(SecurityUser user, Long appId, String executionType, String executionId,
                                 String nodeId, JsonNode node, Map<String, JsonNode> nodes,
                                 Map<String, List<JsonNode>> next, Map<String, List<String>> incoming,
                                 Map<String, Object> values, Set<String> visited, Deque<String> pending,
                                 Map<String, Map<String, Object>> branchValues,
                                 Map<String, Map<String, Object>> branchResults,
                                 WorkflowDependencySnapshot dependencies,
                                 PlatformExecutionContextEntity context, long sequence) {
    }

    private record ControlFlowResult(Object output, String forcedNext, boolean waiting,
                                     boolean completed, long sequence) {
    }

    private record IterationResult(List<Object> outputs, long sequence) {
    }

    private enum IterationErrorStrategy {
        FAIL_FAST, CONTINUE_ON_ERROR;

        private static IterationErrorStrategy parse(String value) {
            return "CONTINUE_ON_ERROR".equalsIgnoreCase(value) ? CONTINUE_ON_ERROR : FAIL_FAST;
        }
    }

    private int boundedRetryAttempts(int requested) {
        return Math.min(MAX_NODE_RETRY_ATTEMPTS, Math.max(1, requested));
    }

    private long boundedRetryBackoff(long configuredBackoffMillis, int retryAttempt) {
        return Math.min(MAX_RETRY_BACKOFF_MILLIS, Math.max(0L, configuredBackoffMillis) * retryAttempt);
    }

    private Map<String, Object> result(PlatformExecutionContextEntity c) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", c.getExecutionId());
        r.put("executionId", c.getExecutionId());
        r.put("requestId", c.getRequestId());
        r.put("traceId", c.getTraceId());
        r.put("spanId", c.getSpanId());
        r.put("taskId", c.getTaskId());
        r.put("retryOfExecutionId", c.getRetryOfExecutionId());
        r.put("retryAttempt", c.getRetryAttempt() == null ? 1 : c.getRetryAttempt());
        r.put("retryCount", c.getRetryCount() == null ? 0 : c.getRetryCount());
        r.put("status", c.getStatus());
        r.put("applicationId", c.getAppId());
        r.put("entrypointId", c.getEntrypointId());
        r.put("entrypointType", c.getEntrypointType());
        r.put("deliveryMode", c.getDeliveryMode());
        r.put("triggerSource", c.getTriggerSource());
        r.put("scheduledFireTime", c.getScheduledFireTime());
        r.put("draftRevisionId", c.getDraftRevisionId());
        r.put("draftRevisionNo", c.getDraftRevisionNo());
        r.put("runType", c.getRunType());
        r.put("productionStatistics", RunType.PRODUCTION.name().equals(c.getRunType()));
        r.put("testRun", !RunType.PRODUCTION.name().equals(c.getRunType()));
        OrchestrationVersionEntity release = c.getVersionId() == null ? null : versionMapper.selectById(c.getVersionId());
        // 内部用数据库主键关联版本，运行合同只暴露发布标识和版本号，避免把存储细节泄漏到入口和前端。
        r.put("releaseId", release == null ? null : release.getVersionId());
        r.put("versionNo", release == null ? null : release.getVersionNo());
        r.put("releaseBundleHash", c.getRuntimeSnapshotHash());
        r.put("executionType", c.getExecutionType());
        r.put("output", c.getOutputJson());
        r.put("input", c.getInputJson());
        r.put("executionContext", c.getStateJson());
        r.put("errorCode", c.getErrorCode());
        r.put("errorMessage", c.getErrorMessage());
        r.put("startedAt", c.getStartedAt());
        r.put("finishedAt", c.getFinishedAt());
        return r;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null)
            throw new IllegalArgumentException("缺少身份信息，请重新登录。");
    }
}
