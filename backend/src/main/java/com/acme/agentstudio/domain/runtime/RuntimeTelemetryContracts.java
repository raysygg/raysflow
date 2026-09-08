package com.acme.agentstudio.domain.runtime;

import java.time.Instant;

/**
 * Agent Runtime 运行时遥测（Telemetry）、SLI/SLO 监控指标与全链路 Trace 追踪强类型契约类（Runtime Telemetry Contracts）。
 * 约定运行阶段 StageType、服务质量指标类型 SliType、SLO 告警状态 AlertStatus 以及 Trace 上下文关联 Record（Correlation）。
 */
public final class RuntimeTelemetryContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private RuntimeTelemetryContracts() {
    }

    /** 运行时链路执行阶段枚举 */
    public enum StageType {
        /** 队列等待阶段 */
        QUEUE,

        /** 任务调度阶段 */
        TASK,

        /** 单次尝试阶段 */
        ATTEMPT,

        /** 工作流节点执行阶段 */
        NODE,

        /** 模型调用阶段 */
        MODEL,

        /** RAG 知识检索阶段 */
        RAG,

        /** 工具调用阶段 */
        TOOL,

        /** 外部连接器阶段 */
        CONNECTOR,

        /** 流程整体运行阶段 */
        RUN
    }

    /** 服务质量 SLI 指标类型枚举 */
    public enum SliType {
        /** 队列等待延迟 */
        QUEUE_LATENCY,

        /** 任务成功率 */
        SUCCESS_RATE,

        /** 任务失败率 */
        FAILURE_RATE,

        /** P95 响应时延 */
        P95_LATENCY,

        /** 自动恢复成功率 */
        RECOVERY_RATE,

        /** 死信队列积压量 */
        DLQ_BACKLOG,

        /** Worker 节点健康度 */
        WORKER_HEALTH
    }

    /** SLO 告警状态枚举 */
    public enum AlertStatus {
        /** 告警触发 */
        OPEN,

        /** 已确认 */
        ACKNOWLEDGED,

        /** 已自动/人工恢复 */
        RECOVERED
    }

    /** 链路 Trace 追踪上下文关联 Record */
    public record Correlation(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String runId,
            String traceId,
            String spanId
    ) {
    }

    /** 运行时遥测事件 Record */
    public record RuntimeTelemetryEvent(
            Correlation correlation,
            StageType stage,
            String eventType,
            long durationMs,
            boolean success,
            String safeSummary,
            Instant occurredAt
    ) {
    }

    /** SLI 指标快照 Record */
    public record SliSnapshot(
            SliType type,
            double value,
            int sampleCount,
            String window,
            Instant calculatedAt
    ) {
    }

    /** SLO 告警记录 Record */
    public record SloAlert(
            String alertKey,
            SliType sliType,
            AlertStatus status,
            double observedValue,
            double targetValue,
            String safeSummary,
            Instant openedAt,
            Instant recoveredAt
    ) {
    }
}

