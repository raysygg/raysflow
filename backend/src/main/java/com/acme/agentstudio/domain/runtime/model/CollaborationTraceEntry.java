package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 多智能体 Agent 协作过程中的全链路 Trace 追踪明细条目 Record（Collaboration Trace Entry）。
 * 记录关联的 Run ID、当前 Agent ID、上游 Parent Agent ID、消息 ID、Token 消耗 tokenCost、微元计费成本 costMicros、
 * 调用的工具 toolId、共享状态变更 Map 以及发生时间 occurredAt。
 *
 * @param runId 关联的运行任务 Run ID
 * @param agentId 当前 Agent 标识 ID
 * @param parentAgentId 上游发起的 Parent Agent 标识 ID
 * @param messageId 产生的消息 ID
 * @param tokenCost Token 消耗量
 * @param costMicros 微元微单位用量成本
 * @param toolId 调用的外部工具标识 ID
 * @param sharedStateChanges 共享团队上下文 Key-Value 增量变更表
 * @param occurredAt Trace 日志产生时间
 */
public record CollaborationTraceEntry(
        String runId,
        String agentId,
        String parentAgentId,
        String messageId,
        long tokenCost,
        long costMicros,
        String toolId,
        Map<String, Object> sharedStateChanges,
        Instant occurredAt
) {
    /** 紧凑构造函数做输入属性断言 */
    public CollaborationTraceEntry {
        if (runId == null || runId.isBlank() || agentId == null || agentId.isBlank()
                || messageId == null || messageId.isBlank() || tokenCost < 0 || costMicros < 0) {
            throw new IllegalArgumentException("协作 Trace 标识和预算参数无效");
        }
        sharedStateChanges = (sharedStateChanges == null) ? Map.of() : Map.copyOf(sharedStateChanges);
        occurredAt = (occurredAt == null) ? Instant.now() : occurredAt;
    }
}

