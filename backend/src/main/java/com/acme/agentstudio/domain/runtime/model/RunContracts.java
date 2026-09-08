package com.acme.agentstudio.domain.runtime.model;

import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.domain.application.RunDeliveryMode;
import com.acme.agentstudio.domain.application.RunTriggerSource;
import com.acme.agentstudio.domain.workflow.model.RunType;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeSubscriptionDescriptor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime 任务发起与查询对外调用的稳定强类型规范契约集中类（Run Contracts）。
 * 屏蔽数据库底层动态实体扩展字典，对外只暴露标准接口 Record（RunRequest, RunSubmission, RunEvent, RunDetail, RunListItem）。
 */
public final class RunContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private RunContracts() {
    }

    /** 触发 Agent/Workflow 调用的请求参数 Record */
    public record RunRequest(
            Long tenantId,
            Long actorId,
            Long applicationId,
            Long entrypointId,
            ApplicationEntrypointType entrypointType,
            String releaseId,
            RunType runType,
            RunDeliveryMode deliveryMode,
            RunTriggerSource triggerSource,
            String idempotencyKey,
            String conversationId,
            String messageId,
            Map<String, Object> input,
            LocalDateTime scheduledFireTime
    ) {
        public RunRequest {
            input = (input == null) ? Map.of() : Map.copyOf(input);
        }
    }

    /** 提交触发成功后的响应句柄实体 Record */
    public record RunSubmission(
            String runId,
            Long applicationId,
            String releaseId,
            String status,
            RunDeliveryMode deliveryMode,
            long eventCursor,
            RealtimeSubscriptionDescriptor realtimeSubscription,
            Object output
    ) {
    }

    /** Run 执行过程中的节点明细事件 Record */
    public record RunEvent(
            Long id,
            String runId,
            Long sequence,
            String nodeId,
            String eventType,
            String status,
            String payload,
            String errorCode,
            String errorMessage,
            LocalDateTime createdAt
    ) {
    }

    /** Run 任务全量事件与明细聚合 Record */
    public record RunDetail(
            Map<String, Object> run,
            List<RunEvent> events,
            long eventCursor
    ) {
        public RunDetail {
            run = (run == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(run);
            events = (events == null) ? List.of() : List.copyOf(events);
        }
    }

    /** 分页查询列表项描述符 Record */
    public record RunListItem(
            String runId,
            String status,
            Long applicationId,
            Long entrypointId,
            String entrypointType,
            String deliveryMode,
            String triggerSource,
            String runType,
            String releaseId,
            Object taskId,
            int retryCount,
            int retryAttempt,
            Object startedAt,
            Object finishedAt,
            String errorCode,
            String errorMessage
    ) {
    }
}

