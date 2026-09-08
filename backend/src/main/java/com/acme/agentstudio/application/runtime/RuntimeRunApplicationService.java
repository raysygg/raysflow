package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.application.saas.TenantEntitlementService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.RunDeliveryMode;
import com.acme.agentstudio.domain.application.RunTriggerSource;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeSubscriptionDescriptor;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunDetail;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunEvent;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunListItem;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunRequest;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunSubmission;
import com.acme.agentstudio.domain.runtime.model.RuntimeApiContract;
import com.acme.agentstudio.domain.runtime.model.RuntimeDeliveryContract;
import com.acme.agentstudio.domain.workflow.model.NodeDebugResult;
import com.acme.agentstudio.domain.workflow.model.RunType;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * 运行时 Run 应用生命周期入口服务（Runtime Run Application Service）。
 * 统一处理 API / Webhook / UI / 定时任务入口发起的 Agent 运行请求提交（submit）、分配与记录 DeliveryMode 模式（IMMEDIATE / REALTIME / BATCH）、
 * 支持运行详情查询（detail）、增量事件流抓取（eventsAfter）、历史版本重放（replayHistorical）以及单节点调试（debugNode）。
 */
@Service
public class RuntimeRunApplicationService {

    /** 默认 Run 列表查询分页大小 */
    private static final int DEFAULT_RUN_PAGE_SIZE = 20;

    /** 最大 Run 列表查询分页大小 */
    private static final int MAX_RUN_PAGE_SIZE = 50;

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(RuntimeRunApplicationService.class);

    /** 执行 ID 结果属性键 */
    private static final String RESULT_EXECUTION_ID = "executionId";

    /** 运行 ID 结果属性键 */
    private static final String RESULT_RUN_ID = "runId";

    /** 应用 ID 结果属性键 */
    private static final String RESULT_APPLICATION_ID = "applicationId";

    /** 发布版本 ID 结果属性键 */
    private static final String RESULT_RELEASE_ID = "releaseId";

    /** 原始状态结果属性键 */
    private static final String RESULT_STATUS = "status";

    /** 规范状态结果属性键 */
    private static final String RESULT_CANONICAL_STATUS = "canonicalStatus";

    /** 事件游标结果属性键 */
    private static final String RESULT_EVENT_CURSOR = "eventCursor";

    /** 持久化编排执行适配服务 */
    private final PersistentOrchestrationExecutionService executionAdapter;

    /** 异步实时运行执行线程池 Executor */
    private final Executor runtimeExecutionExecutor;

    /** 运行上下文 Mapper */
    private final PlatformExecutionContextMapper contextMapper;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 统一租户权益准入服务 */
    private final TenantEntitlementService entitlementService;

    /**
     * 构造函数注入所有依赖服务与线程池。
     */
    public RuntimeRunApplicationService(
            PersistentOrchestrationExecutionService executionAdapter,
            @Qualifier("runtimeExecutionExecutor") Executor runtimeExecutionExecutor,
            PlatformExecutionContextMapper contextMapper,
            ObjectMapper objectMapper,
            TenantEntitlementService entitlementService
    ) {
        this.executionAdapter = executionAdapter;
        this.runtimeExecutionExecutor = runtimeExecutionExecutor;
        this.contextMapper = contextMapper;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    /**
     * 校验并提交包含完整入口规约的 RunRequest。
     *
     * @param user 当前登录 SecurityUser
     * @param request 标准化运行请求 RunRequest
     * @return 运行提交结果对象 RunSubmission
     */
    public RunSubmission submit(SecurityUser user, RunRequest request) {
        requireApplicationContext(user, request.applicationId());
        var admission = entitlementService.admitForConsumption(new AdmissionRequest(
                user.getTenantId(), request.applicationId(), "WORKFLOW_RUN", 1,
                String.valueOf(request.triggerSource()), request.idempotencyKey()), true);
        if (admission.decision() == AdmissionDecision.DENY) {
            throw new IllegalStateException(admission.reason() + " " + admission.remediation());
        }
        Map<String, Object> submitted;

        if (request.runType() == RunType.DRAFT_TEST) {
            submitted = executionAdapter.enqueueDraftTest(
                    user,
                    request.applicationId(),
                    RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                    request.idempotencyKey(),
                    request.conversationId(),
                    request.messageId(),
                    request.input()
            );
        } else if (request.deliveryMode() == RunDeliveryMode.IMMEDIATE) {
            submitted = executionAdapter.execute(
                    user,
                    request.applicationId(),
                    request.releaseId(),
                    RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                    request.idempotencyKey(),
                    request.conversationId(),
                    request.messageId(),
                    request.input()
            );
        } else if (request.deliveryMode() == RunDeliveryMode.REALTIME) {
            submitted = executionAdapter.createRealtimeRun(
                    user,
                    request.applicationId(),
                    request.releaseId(),
                    RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                    request.idempotencyKey(),
                    request.input()
            );
            runtimeExecutionExecutor.execute(() -> executeRealtime(
                    user,
                    request.applicationId(),
                    request.releaseId(),
                    RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                    request.idempotencyKey(),
                    request.conversationId(),
                    request.messageId(),
                    request.input()
            ));
        } else {
            submitted = executionAdapter.enqueue(
                    user,
                    request.applicationId(),
                    request.releaseId(),
                    RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                    request.idempotencyKey(),
                    request.conversationId(),
                    request.messageId(),
                    request.input()
            );
        }

        String runId = stringValue(submitted.getOrDefault(RESULT_EXECUTION_ID, submitted.get(RESULT_RUN_ID)));
        bindEntrypointContext(user.getTenantId(), runId, request);

        String ticketEndpoint = (request.triggerSource() == RunTriggerSource.EXTERNAL_API)
                ? "/api/public/v1/runs/" + runId + "/realtime-ticket"
                : "/api/realtime/tickets";

        log.info("Runtime 运行请求已成功接收并提交，tenantId={}, applicationId={}, runId={}, deliveryMode={}, entrypointType={}",
                user.getTenantId(), request.applicationId(), runId, request.deliveryMode(), request.entrypointType());

        return new RunSubmission(
                runId,
                request.applicationId(),
                request.releaseId(),
                stringValue(submitted.get(RESULT_STATUS)),
                request.deliveryMode(),
                RuntimeDeliveryContract.FIRST_EVENT_CURSOR,
                new RealtimeSubscriptionDescriptor(RealtimeChannel.EXECUTION_EVENTS, runId, ticketEndpoint),
                submitted.get("output")
        );
    }

    /** 绑定入口调用的元数据上下文 */
    private void bindEntrypointContext(Long tenantId, String runId, RunRequest request) {
        if (runId == null) {
            throw new IllegalStateException("运行 Run 创建成功但未返回有效的运行标识 runId。");
        }
        PlatformExecutionContextEntity context = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, tenantId)
                .eq(PlatformExecutionContextEntity::getExecutionId, runId));

        if (context == null) {
            throw new IllegalStateException("运行上下文不存在，无法绑定入口来源元数据。");
        }

        context.setEntrypointId(request.entrypointId());
        context.setEntrypointType(request.entrypointType().name());
        context.setDeliveryMode(request.deliveryMode().name());
        context.setTriggerSource(request.triggerSource().name());
        context.setScheduledFireTime(request.scheduledFireTime());
        contextMapper.updateById(context);
    }

    /** 安全字符串转换 */
    private String stringValue(Object value) {
        return (value == null) ? null : String.valueOf(value);
    }

    /** 在独立子线程异步触发实时运行执行 */
    private void executeRealtime(
            SecurityUser user,
            Long applicationId,
            String releaseId,
            String executionType,
            String idempotencyKey,
            String conversationId,
            String messageId,
            Map<String, Object> input
    ) {
        try {
            executionAdapter.execute(
                    user,
                    applicationId,
                    releaseId,
                    executionType,
                    idempotencyKey,
                    conversationId,
                    messageId,
                    input,
                    null,
                    true
            );
            log.info("实时 Agent 运行任务执行完成，applicationId={}, idempotencyKey={}", applicationId, idempotencyKey);
        } catch (Exception exception) {
            log.error("实时 Agent 运行任务执行失败，applicationId={}, idempotencyKey={}, error={}",
                    applicationId, idempotencyKey, exception.getMessage(), exception);
        }
    }

    /** 执行草稿调试试运行 */
    public Map<String, Object> executeDraftTest(
            SecurityUser user,
            Long applicationId,
            String executionId,
            String executionType,
            String idempotencyKey,
            String conversationId,
            String messageId,
            Map<String, Object> input
    ) {
        return executionAdapter.executeDraftTest(
                user,
                applicationId,
                executionId,
                executionType,
                idempotencyKey,
                conversationId,
                messageId,
                input
        );
    }

    /** 查询 Run 详情 Map 结构 */
    public Map<String, Object> detail(SecurityUser user, String runId) {
        Map<String, Object> result = new LinkedHashMap<>(executionAdapter.get(user, runId));
        Object eventList = result.get("events");
        long cursor = 0L;

        if (eventList instanceof Iterable<?> events) {
            for (Object event : events) {
                if (event instanceof PlatformExecutionEventEntity entity && entity.getSequenceNo() != null) {
                    cursor = Math.max(cursor, entity.getSequenceNo());
                }
            }
        }
        result.put(RESULT_RUN_ID, result.getOrDefault(RESULT_EXECUTION_ID, runId));
        result.put(RESULT_CANONICAL_STATUS, result.get(RESULT_STATUS));
        result.put(RESULT_EVENT_CURSOR, cursor);
        return result;
    }

    /** 查询 RunDetail 面向前端展示视图 */
    public RunDetail detailView(SecurityUser user, String runId, long afterSequence) {
        Map<String, Object> run = detail(user, runId);
        List<RunEvent> events = events(user, runId, afterSequence);
        long cursor = events.stream()
                .map(RunEvent::sequence)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(afterSequence);

        return new RunDetail(run, events, cursor);
    }

    /** 分页列表默认无参重载 */
    public List<RunListItem> list(SecurityUser user) {
        return list(user, DEFAULT_RUN_PAGE_SIZE, 0);
    }

    /** 分页按页读取应用下的运行列表 */
    public List<RunListItem> list(SecurityUser user, int limit, int offset) {
        int pageSize = Math.max(1, Math.min(MAX_RUN_PAGE_SIZE, limit));
        int pageOffset = Math.max(0, offset);
        return executionAdapter.list(user, pageSize, pageOffset).stream()
                .map(this::toRunListItem)
                .toList();
    }

    /** 转换为列表传输项 Record */
    private RunListItem toRunListItem(Map<String, Object> item) {
        return new RunListItem(
                stringValue(item.getOrDefault(RESULT_EXECUTION_ID, item.get("id"))),
                stringValue(item.get(RESULT_STATUS)),
                longValue(item.get(RESULT_APPLICATION_ID)),
                longValue(item.get("entrypointId")),
                stringValue(item.get("entrypointType")),
                stringValue(item.get("deliveryMode")),
                stringValue(item.get("triggerSource")),
                stringValue(item.get("runType")),
                stringValue(item.get(RESULT_RELEASE_ID)),
                item.get("taskId"),
                intValue(item.get("retryCount")),
                Math.max(1, intValue(item.get("retryAttempt"))),
                item.get("startedAt"),
                item.get("finishedAt"),
                stringValue(item.get("errorCode")),
                stringValue(item.get("errorMessage"))
        );
    }

    /** 安全转换 long 值 */
    private Long longValue(Object value) {
        return (value instanceof Number number) ? number.longValue() : null;
    }

    /** 安全转换 int 值 */
    private int intValue(Object value) {
        return (value instanceof Number number) ? number.intValue() : 0;
    }

    /** 查询特定序号之后的事件列表 */
    public List<PlatformExecutionEventEntity> eventsAfter(SecurityUser user, String runId, long afterSequence) {
        return executionAdapter.eventsAfter(user, runId, afterSequence);
    }

    /** 转换为面向前端的 RunEvent 列表 */
    public List<RunEvent> events(SecurityUser user, String runId, long afterSequence) {
        return eventsAfter(user, runId, afterSequence).stream()
                .map(this::toRunEvent)
                .toList();
    }

    /** 实体映射转换为 RunEvent Record */
    private RunEvent toRunEvent(PlatformExecutionEventEntity event) {
        return new RunEvent(
                event.getId(),
                event.getExecutionId(),
                event.getSequenceNo(),
                event.getNodeId(),
                event.getEventType(),
                event.getStatus(),
                event.getSummaryJson(),
                event.getErrorCode(),
                event.getErrorMessage(),
                event.getCreatedAt()
        );
    }

    /** 事件通用条件多维度检索 */
    public List<PlatformExecutionEventEntity> queryEvents(
            SecurityUser user,
            Long applicationId,
            Long versionId,
            String runId,
            Long taskId,
            String nodeId,
            String errorCode,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return executionAdapter.queryEvents(user, applicationId, versionId, runId, taskId, nodeId, errorCode, from, to);
    }

    /** 查询原始底层事件 Map */
    public Map<String, Object> rawEvent(SecurityUser user, Long eventId) {
        return executionAdapter.rawEvent(user, eventId);
    }

    /** Worker 执行具体入队 Run */
    public Map<String, Object> execute(
            SecurityUser user,
            Long applicationId,
            String releaseId,
            String executionType,
            String idempotencyKey,
            String conversationId,
            String messageId,
            Map<String, Object> input
    ) {
        return executionAdapter.execute(
                user,
                applicationId,
                releaseId,
                executionType,
                idempotencyKey,
                conversationId,
                messageId,
                input
        );
    }

    /** 执行评估评测快照中的固定 Candidate 图 JSON */
    public Map<String, Object> executeCandidateSnapshot(
            SecurityUser user,
            Long applicationId,
            String executionId,
            String graphJson,
            Map<String, Object> input
    ) {
        return executionAdapter.execute(
                user,
                applicationId,
                null,
                RuntimeApiContract.DEFAULT_EXECUTION_TYPE,
                executionId,
                null,
                null,
                (input == null) ? Map.of() : input,
                graphJson
        );
    }

    /** 恢复人工暂停或者待审批中的 Run */
    public Map<String, Object> resume(SecurityUser user, String runId, String decision) {
        return executionAdapter.resume(user, runId, decision);
    }

    /** 恢复租约失效断连的 Run */
    public Map<String, Object> recover(SecurityUser user, String runId) {
        return executionAdapter.recover(user, runId);
    }

    /**
     * 基于历史版本的运行快照重放启动新的 Run，并将其绑定到原始版本。
     *
     * @param user 当前登录 SecurityUser
     * @param runId 原始历史 Run ID
     * @param idempotencyKey 重放幂等 Key
     * @return 提交创建的新 Run 执行结果 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> replayHistorical(SecurityUser user, String runId, String idempotencyKey) {
        if (user == null || user.getTenantId() == null || runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("请求历史重放时，用户身份与原运行标识 runId 不能为空。");
        }

        PlatformExecutionContextEntity source = contextMapper.selectOne(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                .eq(PlatformExecutionContextEntity::getExecutionId, runId));

        if (source == null || source.getAppId() == null || source.getVersionId() == null) {
            throw new IllegalArgumentException("历史运行记录 [" + runId + "] 缺少绑定的应用或发布版本信息，无法执行重放。");
        }

        try {
            Map<String, Object> input = (source.getInputJson() == null || source.getInputJson().isBlank())
                    ? Map.of()
                    : objectMapper.readValue(source.getInputJson(), Map.class);

            String replayKey = (idempotencyKey == null || idempotencyKey.isBlank())
                    ? ("replay:" + runId + ":" + System.currentTimeMillis())
                    : idempotencyKey;

            Map<String, Object> replay = executionAdapter.enqueue(
                    user,
                    source.getAppId(),
                    String.valueOf(source.getVersionId()),
                    source.getExecutionType(),
                    replayKey,
                    null,
                    null,
                    input
            );

            String newRunId = stringValue(replay.get(RESULT_EXECUTION_ID));
            if (newRunId != null) {
                contextMapper.update(null, new LambdaUpdateWrapper<PlatformExecutionContextEntity>()
                        .eq(PlatformExecutionContextEntity::getTenantId, user.getTenantId())
                        .eq(PlatformExecutionContextEntity::getExecutionId, newRunId)
                        .set(PlatformExecutionContextEntity::getReplayOfExecutionId, runId));
            }
            return replay;
        } catch (Exception exception) {
            throw new IllegalStateException("读取或解析历史运行快照失败：" + exception.getMessage(), exception);
        }
    }

    /** 调试执行单个节点，调测结果隔离不纳入正式生产指标库 */
    public NodeDebugResult debugNode(
            SecurityUser user,
            Long applicationId,
            String nodeType,
            JsonNode config,
            Map<String, Object> input
    ) {
        if (applicationId == null || nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("调试节点时，应用标识与节点类型 nodeType 均不能为空。");
        }
        return executionAdapter.debugNode(user, applicationId, nodeType, config, input);
    }

    /** 安全校验上下文 */
    private void requireApplicationContext(SecurityUser user, Long applicationId) {
        if (user == null || user.getTenantId() == null || applicationId == null) {
            throw new IllegalArgumentException("应用运行上下文信息 user 与 applicationId 均不能为空。");
        }
    }
}

