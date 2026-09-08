package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 经过最大重试上限后无法自动恢复的 Runtime 任务死信队列记录实体 Record（Runtime Dead Letter）。
 * 包含任务 ID taskId、运行任务 ID runId、任务类型 taskType、死信入队原因 reason、原始消息载荷 payload 与入队时间 createdAt。
 *
 * @param taskId 异步任务唯一 ID
 * @param runId 关联的运行任务 Run ID
 * @param taskType 任务分类类型说明
 * @param reason 导致死信入队的具体错误分类原因
 * @param payload 原始任务入参及异常堆栈载荷 Map
 * @param createdAt 进入死信队列的时间
 */
public record RuntimeDeadLetter(
        String taskId,
        String runId,
        String taskType,
        String reason,
        Map<String, Object> payload,
        Instant createdAt
) {
    /** 紧凑构造函数做输入属性校验 */
    public RuntimeDeadLetter {
        if (taskId == null || taskId.isBlank() || runId == null || runId.isBlank()
                || taskType == null || taskType.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("死信任务标识、Run、类型和原因不能为空");
        }
        payload = (payload == null) ? Map.of() : Map.copyOf(payload);
        createdAt = (createdAt == null) ? Instant.now() : createdAt;
    }
}

