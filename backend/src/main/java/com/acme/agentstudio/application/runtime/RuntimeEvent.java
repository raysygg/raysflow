package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RunStatus;

import java.time.Instant;
import java.util.Map;

/**
 * 平台统一运行时事件实体（Runtime Event Record）。
 * 承载 Agent 运行过程中的单条粒度事件，包含单调递增的序号（sequence），用于在单次 Run 内保持绝对顺序、断点续传恢复以及前端 SSE/WebSocket 实时推流。
 *
 * @param eventId 事件唯一 ID
 * @param runId 运行 ID
 * @param sequence 单调递增事件序号 (>= 0)
 * @param type 事件类型 RuntimeEventType
 * @param status 事件触发时的 Run 状态 RunStatus
 * @param payload 事件扩展载荷数据 Map
 * @param occurredAt 事件发生时间戳
 */
public record RuntimeEvent(
        String eventId,
        String runId,
        long sequence,
        RuntimeEventType type,
        RunStatus status,
        Map<String, Object> payload,
        Instant occurredAt
) {
    /**
     * 紧凑构造函数：校验必需属性非空，规范序号非负并初始化默认时间戳与不可变载荷。
     */
    public RuntimeEvent {
        if (eventId == null || eventId.isBlank() || runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("事件唯一标识 eventId 和运行标识 runId 均不能为空。");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("运行时事件序号 sequence 必须为大于等于零的单调递增长整型。");
        }
        if (type == null) {
            throw new IllegalArgumentException("运行时事件类型 RuntimeEventType 不能为空。");
        }
        payload = (payload == null) ? Map.of() : Map.copyOf(payload);
        occurredAt = (occurredAt == null) ? Instant.now() : occurredAt;
    }
}

