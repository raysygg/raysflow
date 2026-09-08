package com.acme.agentstudio.domain.runtime.model;

/**
 * 限制一次 Agent/Workflow Run 的最大并发规模与步数上限策略 Record（Concurrency Policy）。
 * 限制并行 Agent 数量 maxParallelAgents、并行工具调用数 maxParallelTools、单 Agent 步数 maxStepsPerAgent
 * 以及上下文并行冲突合并策略 contextMergeStrategy。
 *
 * @param maxParallelAgents 最大并行 Agent 数上限
 * @param maxParallelTools 最大并行 Tool 工具调用数上限
 * @param maxStepsPerAgent 单个 Agent 内最大步数上限
 * @param contextMergeStrategy 上下文合并碰撞解冲突策略
 */
public record ConcurrencyPolicy(
        int maxParallelAgents,
        int maxParallelTools,
        int maxStepsPerAgent,
        ContextMergeStrategy contextMergeStrategy
) {
    /** 默认最大并行 Agent 数量：4 */
    public static final int DEFAULT_MAX_PARALLEL_AGENTS = 4;

    /** 默认最大并行 Tool 调用数量：8 */
    public static final int DEFAULT_MAX_PARALLEL_TOOLS = 8;

    /** 默认单个 Agent 最大步骤数：16 */
    public static final int DEFAULT_MAX_STEPS_PER_AGENT = 16;

    /** 紧凑构造函数做输入限额断言校验 */
    public ConcurrencyPolicy {
        if (maxParallelAgents < 1 || maxParallelTools < 1 || maxStepsPerAgent < 1) {
            throw new IllegalArgumentException("并发 Agent、工具和步骤上限必须大于零。");
        }
        if (contextMergeStrategy == null) {
            throw new IllegalArgumentException("上下文合并策略不能为空。");
        }
    }

    /**
     * 构建默认并发控制策略。
     *
     * @return 默认的 ConcurrencyPolicy 实例
     */
    public static ConcurrencyPolicy defaults() {
        return new ConcurrencyPolicy(DEFAULT_MAX_PARALLEL_AGENTS, DEFAULT_MAX_PARALLEL_TOOLS,
                DEFAULT_MAX_STEPS_PER_AGENT, ContextMergeStrategy.REJECT_CONFLICT);
    }
}

