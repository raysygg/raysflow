package com.acme.agentstudio.application.task;

import com.acme.agentstudio.application.knowledge.KnowledgeApplicationService;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationExecutor;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService.ApplicationEvaluationTaskPayload;
import com.acme.agentstudio.application.runtime.RuntimeRetryPolicy;
import com.acme.agentstudio.application.runtime.RuntimeRunApplicationService;
import com.acme.agentstudio.application.runtime.RuntimeTaskAttemptService;
import com.acme.agentstudio.application.runtime.RuntimeTelemetryService;
import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.config.RequestTraceContext;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts;
import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeTaskAttemptEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 平台持久化后台任务消费者（Persistent Async Task Worker）。
 * 负责定时拉取队列（claim）、接管失联超时任务（reclaimExpired）、分发至相应处理器（Knowledge, Orchestration, Evaluation）并维持心跳续租与指标遥测。
 */
@Component
public class PersistentTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(PersistentTaskWorker.class);

    /** 持久化任务队列服务 */
    private final PersistentTaskQueueService queue;

    /** 知识库应用服务 */
    private final KnowledgeApplicationService knowledgeService;

    /** Runtime 运行服务 */
    private final RuntimeRunApplicationService runService;

    /** 工作流执行持久化服务 */
    private final PersistentOrchestrationExecutionService executionService;

    /** 应用候选版本评测执行器 */
    private final ApplicationEvaluationExecutor evaluationExecutor;

    /** 运行重试与尝试记录服务 */
    private final RuntimeTaskAttemptService attemptService;

    /** 平台运行遥测服务 */
    private final RuntimeTelemetryService telemetryService;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /** 心跳超时秒数 */
    private final int heartbeatTimeoutSeconds;

    /** 异常退避重试延迟秒数 */
    private final int retryDelaySeconds;

    /** 当前 Worker 实例的唯一随机 UUID 标识 */
    private final String workerId = UUID.randomUUID().toString();

    /** Worker 是否就绪标记 */
    private volatile boolean ready;

    /** 单线程心跳保活调度器 */
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "task-heartbeat-" + UUID.randomUUID());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 构造函数注入任务消费者各组件依赖与配置参数。
     */
    public PersistentTaskWorker(PersistentTaskQueueService queue,
                                 KnowledgeApplicationService knowledgeService,
                                 RuntimeRunApplicationService runService,
                                 PersistentOrchestrationExecutionService executionService,
                                 ApplicationEvaluationExecutor evaluationExecutor,
                                 RuntimeTaskAttemptService attemptService,
                                 RuntimeTelemetryService telemetryService,
                                 ObjectMapper objectMapper,
                                 @Value("${app.task-queue.heartbeat-timeout-seconds:120}") int heartbeatTimeoutSeconds,
                                 @Value("${app.task-queue.retry-delay-seconds:30}") int retryDelaySeconds) {
        this.queue = queue;
        this.knowledgeService = knowledgeService;
        this.runService = runService;
        this.executionService = executionService;
        this.evaluationExecutor = evaluationExecutor;
        this.attemptService = attemptService;
        this.telemetryService = telemetryService;
        this.objectMapper = objectMapper;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    /**
     * 应用容器加载完 Bean 依赖后标记 Worker 已就绪。
     */
    @PostConstruct
    public void markReady() {
        ready = true;
    }

    /**
     * 检查 Worker 节点是否具备消费就绪状态（用于平台诊断/Readiness Endpoint）。
     */
    public boolean isReady() {
        return ready && !heartbeatExecutor.isShutdown();
    }

    /**
     * 定时轮询消费主循环（Poller Loop）。
     */
    @Scheduled(fixedDelayString = "${app.task-queue.poll-delay-ms:1000}")
    public void consume() {
        if (!ready) {
            return;
        }

        // 1. 接管失联 Worker 超时的任务
        for (AsyncTaskEntity reclaimed : queue.reclaimExpired(heartbeatTimeoutSeconds, retryDelaySeconds)) {
            if (reclaimed.getRunId() != null && !reclaimed.getRunId().isBlank()) {
                executionService.recordWorkerFailure(
                        taskTenant(reclaimed),
                        reclaimed.getRunId(),
                        reclaimed.getId(),
                        reclaimed.getStatus(),
                        reclaimed.getRetryCount(),
                        reclaimed.getErrorMessage()
                );
            }
        }

        // 2. 提取下一个排队中的任务
        AsyncTaskEntity task = queue.nextQueued();
        if (task == null || !queue.claim(task, workerId)) {
            return;
        }

        RuntimeTaskAttemptEntity attempt = attemptService.claim(task, workerId);
        log.info("后台任务认领成功，开始执行。taskId={}, runId={}, taskType={}, tenantId={}",
                task.getId(), task.getRunId(), task.getTaskType(), task.getTenantId());

        ScheduledFuture<?> heartbeat = startHeartbeat(task, attempt.getId());
        Map<String, Object> payload = Map.of();
        long executionStartedAt = System.nanoTime();
        RequestTraceContext.TraceScope traceScope = null;

        try {
            payload = objectMapper.readValue(task.getPayloadJson(), new TypeReference<>() {});
            recordQueueTelemetry(task, payload);

            // 恢复异步链路上下文 TraceScope
            traceScope = RequestTraceContext.open(
                    task.getRequestId(),
                    task.getTraceId(),
                    task.getRunId(),
                    String.valueOf(task.getId()),
                    null,
                    UUID.randomUUID().toString()
            );

            // 分派实际业务 TaskType 处理器
            switch (task.getTaskType()) {
                case TaskType.KNOWLEDGE_REINDEX -> knowledgeService.reindexDocument(
                        ((Number) payload.get("tenantId")).longValue(),
                        ((Number) payload.get("documentId")).longValue(),
                        modelSelection(payload)
                );
                case TaskType.KNOWLEDGE_REINDEX_BATCH -> knowledgeService.rebuildTenantIndex(
                        ((Number) payload.get("tenantId")).longValue(),
                        modelSelection(payload)
                );
                case TaskType.ORCHESTRATION_EXECUTION -> runService.execute(
                        new SecurityUser(
                                ((Number) payload.get("userId")).longValue(),
                                task.getTenantId(),
                                String.valueOf(payload.get("username")),
                                String.valueOf(payload.get("role"))
                        ),
                        ((Number) payload.get("appId")).longValue(),
                        String.valueOf(payload.get("versionId")),
                        String.valueOf(payload.get("executionType")),
                        String.valueOf(payload.get("idempotencyKey")),
                        payload.get("conversationId") == null ? null : String.valueOf(payload.get("conversationId")),
                        payload.get("messageId") == null ? null : String.valueOf(payload.get("messageId")),
                        (Map<String, Object>) payload.getOrDefault("input", Map.of())
                );
                case TaskType.ORCHESTRATION_DRAFT_TEST -> runService.executeDraftTest(
                        new SecurityUser(
                                ((Number) payload.get("userId")).longValue(),
                                task.getTenantId(),
                                String.valueOf(payload.get("username")),
                                String.valueOf(payload.get("role"))
                        ),
                        ((Number) payload.get("appId")).longValue(),
                        String.valueOf(payload.get("executionId")),
                        String.valueOf(payload.get("executionType")),
                        String.valueOf(payload.get("idempotencyKey")),
                        payload.get("conversationId") == null ? null : String.valueOf(payload.get("conversationId")),
                        payload.get("messageId") == null ? null : String.valueOf(payload.get("messageId")),
                        (Map<String, Object>) payload.getOrDefault("input", Map.of())
                );
                case TaskType.ORCHESTRATION_RESUME -> runService.resume(
                        new SecurityUser(
                                ((Number) payload.get("userId")).longValue(),
                                task.getTenantId(),
                                String.valueOf(payload.get("username")),
                                String.valueOf(payload.get("role"))
                        ),
                        String.valueOf(payload.get("executionId")),
                        String.valueOf(payload.get("decision"))
                );
                case TaskType.ORCHESTRATION_RECOVERY -> runService.recover(
                        new SecurityUser(
                                ((Number) payload.get("userId")).longValue(),
                                task.getTenantId(),
                                String.valueOf(payload.get("username")),
                                String.valueOf(payload.get("role"))
                        ),
                        String.valueOf(payload.get("executionId"))
                );
                case TaskType.APPLICATION_EVALUATION -> evaluationExecutor.execute(
                        objectMapper.convertValue(payload, ApplicationEvaluationTaskPayload.class),
                        task.getTenantId()
                );
                default -> throw new IllegalArgumentException("不支持的后台异步任务类型：" + task.getTaskType());
            }

            queue.complete(task.getId(), workerId);
            attemptService.finish(attempt.getId(), task.getId(), workerId, true, null, null);
            recordTaskTelemetry(task, payload, true, "任务执行完成", executionStartedAt);
            log.info("后台任务顺利执行完成。taskId={}, runId={}, taskType={}",
                    task.getId(), task.getRunId(), task.getTaskType());
        } catch (Exception exception) {
            String errorMessage = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "异步任务执行处理失败。"
                    : exception.getMessage();

            com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory errorCategory = RuntimeRetryPolicy.classify(exception);
            AsyncTaskEntity failedTask = RuntimeRetryPolicy.retryable(errorCategory)
                    ? queue.failOrRetry(task.getId(), workerId, retryDelaySeconds, errorMessage)
                    : queue.failPermanently(task.getId(), workerId, errorMessage);

            attemptService.finish(attempt.getId(), task.getId(), workerId, false, errorCategory, errorMessage);
            recordTaskTelemetry(task, payload, false, errorMessage, executionStartedAt);

            log.error("异步任务执行失败异常：taskId={}, runId={}, taskType={}, retryCount={}",
                    task.getId(), task.getRunId(), task.getTaskType(),
                    failedTask == null ? task.getRetryCount() : failedTask.getRetryCount(), exception);

            Object executionId = payload == null ? null : payload.get("executionId");
            if (executionId != null && failedTask != null) {
                executionService.recordWorkerFailure(
                        task.getTenantId(),
                        String.valueOf(executionId),
                        failedTask.getId(),
                        failedTask.getStatus(),
                        failedTask.getRetryCount(),
                        errorMessage
                );
            }
        } finally {
            if (traceScope != null) {
                traceScope.close();
            }
            heartbeat.cancel(false);
        }
    }

    /**
     * 记录排队延缓指标遥测。
     */
    private void recordQueueTelemetry(AsyncTaskEntity task, Map<String, Object> payload) {
        long queueLatency = task.getCreatedAt() == null
                ? 0L
                : Math.max(0L, Duration.between(
                task.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                Instant.now()
        ).toMillis());

        telemetryService.record(new RuntimeTelemetryContracts.RuntimeTelemetryEvent(
                correlation(task, payload),
                RuntimeTelemetryContracts.StageType.QUEUE,
                "TASK_DEQUEUED",
                queueLatency,
                true,
                "任务已出队开始执行",
                Instant.now()
        ));
    }

    /**
     * 记录任务执行遥测。
     */
    private void recordTaskTelemetry(AsyncTaskEntity task, Map<String, Object> payload, boolean success,
                                      String summary, long startedAt) {
        telemetryService.record(new RuntimeTelemetryContracts.RuntimeTelemetryEvent(
                correlation(task, payload),
                RuntimeTelemetryContracts.StageType.TASK,
                success ? "TASK_COMPLETED" : "TASK_FAILED",
                Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L),
                success,
                summary,
                Instant.now()
        ));
    }

    /**
     * 构建 Correlation 上下文。
     */
    private RuntimeTelemetryContracts.Correlation correlation(AsyncTaskEntity task, Map<String, Object> payload) {
        Long applicationId = payload.get("appId") instanceof Number number ? number.longValue() : null;
        String releaseId = payload.get("versionId") == null ? null : String.valueOf(payload.get("versionId"));
        return new RuntimeTelemetryContracts.Correlation(
                task.getTenantId(),
                applicationId,
                releaseId,
                task.getRunId(),
                task.getTraceId(),
                null
        );
    }

    /**
     * 启动心跳保活续租线程。
     */
    private ScheduledFuture<?> startHeartbeat(AsyncTaskEntity task, Long attemptId) {
        long intervalSeconds = Math.max(1, heartbeatTimeoutSeconds / 3L);
        return heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    queue.heartbeat(task.getId(), workerId);
                    attemptService.heartbeat(attemptId, task.getId(), workerId);
                },
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );
    }

    /**
     * 获取任务租户 ID。
     */
    private Long taskTenant(AsyncTaskEntity task) {
        return task.getTenantId();
    }

    /**
     * 从 Payload 解析 RAG 模型选择策略。
     */
    private RagModelSelection modelSelection(Map<String, Object> payload) {
        Object source = payload.get("modelSource");
        if (source == null) {
            return null;
        }
        RagModelSource modelSource = RagModelSource.valueOf(String.valueOf(source));
        Object modelId = payload.get("modelId");
        return new RagModelSelection(
                modelSource,
                modelId instanceof Number number ? number.longValue() : null,
                payload.get("modelKey") == null ? null : String.valueOf(payload.get("modelKey"))
        );
    }

    /**
     * 容器销毁时优雅下线与停止心跳保活线程。
     */
    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
        ready = false;
    }
}
