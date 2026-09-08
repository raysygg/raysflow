package com.acme.agentstudio.domain.runtime.model;

/**
 * 拓扑调度器在多 Agent 协作时生成的单步 Agent 执行步骤 Record（Collaboration Step）。
 *
 * @param sequence 步数全局序号
 * @param round 协作轮次序号 (从 1 开始)
 * @param agentId 当前被调度的 Agent ID
 * @param delegatedByAgentId 委派下发该步骤的上游 Agent ID
 * @param finalAnswerOwner 是否为最终响应汇总人 Owner
 */
public record CollaborationStep(
        int sequence,
        int round,
        String agentId,
        String delegatedByAgentId,
        boolean finalAnswerOwner
) {
    /** 紧凑构造函数做断言校验 */
    public CollaborationStep {
        if (sequence < 0 || round < 1 || agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("协作步骤序号、轮次和 Agent 不能为空。");
        }
    }
}

