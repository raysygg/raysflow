package com.acme.agentstudio.domain.runtime.model;

/**
 * 多智能体 Agent 团队协作终止防死循环条件配置 Record（Collaboration Termination）。
 * 控制最大消息数 maxMessages、最大轮次数 maxRounds，避免无界委派与死循环争论。
 *
 * @param maxMessages 消息上限数
 * @param maxRounds 轮次上限数
 * @param requireFinalOwner 是否要求明确的最终响应 Owner 汇总人
 */
public record CollaborationTermination(
        int maxMessages,
        int maxRounds,
        boolean requireFinalOwner
) {
    /** 默认最大消息上限：64 条 */
    public static final int DEFAULT_MAX_MESSAGES = 64;

    /** 默认最大轮次上限：8 轮 */
    public static final int DEFAULT_MAX_ROUNDS = 8;

    /** 紧凑构造函数做输入校验 */
    public CollaborationTermination {
        if (maxMessages < 1 || maxRounds < 1) {
            throw new IllegalArgumentException("协作消息和轮次上限必须大于零。");
        }
    }

    /**
     * 构建默认终止条件配置。
     *
     * @return 默认的 CollaborationTermination 实例
     */
    public static CollaborationTermination defaults() {
        return new CollaborationTermination(DEFAULT_MAX_MESSAGES, DEFAULT_MAX_ROUNDS, true);
    }
}

