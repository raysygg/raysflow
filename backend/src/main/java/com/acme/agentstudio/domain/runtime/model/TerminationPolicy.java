package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent/Workflow 运行任务终止条件与转人工接入控制策略 Record（Termination Policy）。
 * 控制单次运行允许调用的最大工具总次数 maxToolCalls、出现困难或需要审批时是否允许接管转人工客服/专家 allowHumanHandoff
 * 以及运行预算（Token/时长/步骤）耗尽时是否强制收敛终止 stopOnBudgetExhaustion。
 *
 * @param maxToolCalls 工具调用的最大允许次数上限（0 表示不限制）
 * @param allowHumanHandoff 是否允许 Human-in-the-loop 人工介入接管
 * @param stopOnBudgetExhaustion 运行预算消耗完时是否强制终止流程
 */
public record TerminationPolicy(
        int maxToolCalls,
        boolean allowHumanHandoff,
        boolean stopOnBudgetExhaustion
) {
    /** 默认最大工具调用次数：16 */
    public static final int DEFAULT_MAX_TOOL_CALLS = 16;

    /** 默认允许人工接管：true */
    public static final boolean DEFAULT_ALLOW_HUMAN_HANDOFF = true;

    /** 默认预算耗尽强制终止：true */
    public static final boolean DEFAULT_STOP_ON_BUDGET_EXHAUSTION = true;

    /** 紧凑构造函数做输入校验 */
    public TerminationPolicy {
        if (maxToolCalls < 0) {
            throw new IllegalArgumentException("工具调用次数上限不能为负数。");
        }
    }

    /**
     * 构建默认运行终止策略。
     *
     * @return 默认的 TerminationPolicy 实例
     */
    public static TerminationPolicy defaults() {
        return new TerminationPolicy(DEFAULT_MAX_TOOL_CALLS, DEFAULT_ALLOW_HUMAN_HANDOFF,
                DEFAULT_STOP_ON_BUDGET_EXHAUSTION);
    }
}

