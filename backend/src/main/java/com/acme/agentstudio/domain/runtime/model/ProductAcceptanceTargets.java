package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台产品采用（Product Adoption）与运行质量的量化验收目标指标 Record（Product Acceptance Targets）。
 * 包含首次价值时间 maxMinutesToFirstApp、最小任务完成率 minimumTaskCompletionRate、
 * 最小回答忠实度 minimumGroundedAnswerRate、最小故障恢复率 minimumRecoveryRate、最大允许延迟 maxLatencyMillis 与最大成本 maxCostMicros。
 *
 * @param maxMinutesToFirstApp 创建第一个 Agent 应用的最大首次价值分钟数
 * @param minimumTaskCompletionRate 最小任务成功完成率（0.0 ~ 1.0）
 * @param minimumGroundedAnswerRate 最小基于事实的忠实回答率（0.0 ~ 1.0）
 * @param minimumRecoveryRate 最小故障恢复成功率（0.0 ~ 1.0）
 * @param maxLatencyMillis 最大允许全链路响应延迟毫秒数
 * @param maxCostMicros 最大允许单次运行成本微元
 */
public record ProductAcceptanceTargets(
        int maxMinutesToFirstApp,
        double minimumTaskCompletionRate,
        double minimumGroundedAnswerRate,
        double minimumRecoveryRate,
        long maxLatencyMillis,
        long maxCostMicros
) {
    /** 紧凑构造函数做输入属性合规校验 */
    public ProductAcceptanceTargets {
        if (maxMinutesToFirstApp < 1 || minimumTaskCompletionRate < 0D || minimumTaskCompletionRate > 1D
                || minimumGroundedAnswerRate < 0D || minimumGroundedAnswerRate > 1D
                || minimumRecoveryRate < 0D || minimumRecoveryRate > 1D || maxLatencyMillis < 1 || maxCostMicros < 0) {
            throw new IllegalArgumentException("产品验收目标参数无效");
        }
    }
}

