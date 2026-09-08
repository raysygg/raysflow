package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent/Workflow 单次运行防失控硬性资源预算限额配置 Record（Runtime Budget）。
 * 限制最大运行步骤数 maxSteps、最大消耗 Token 数 maxTokens、最大微元用量成本 maxCostMicros (0 表示不限制) 与超时毫秒数 timeoutMillis。
 *
 * @param maxSteps 最大步骤上限
 * @param maxTokens 最大 Token 数量上限
 * @param maxCostMicros 最大允许金额成本微元（1元 = 1,000,000 微元）
 * @param timeoutMillis 全链路超时时间（毫秒）
 */
public record RuntimeBudget(
        int maxSteps,
        int maxTokens,
        long maxCostMicros,
        long timeoutMillis
) {
    /** 默认最大步骤限制：32 步 */
    public static final int DEFAULT_MAX_STEPS = 32;

    /** 默认最大 Token 限制：32,000 Token */
    public static final int DEFAULT_MAX_TOKENS = 32_000;

    /** 默认最大成本限制：0（不硬性限制） */
    public static final long DEFAULT_MAX_COST_MICROS = 0L;

    /** 默认超时限制：15 分钟（900,000 毫秒） */
    public static final long DEFAULT_TIMEOUT_MILLIS = 15 * 60_000L;

    /** 紧凑构造函数做输入校验 */
    public RuntimeBudget {
        if (maxSteps < 0 || maxTokens < 0 || maxCostMicros < 0 || timeoutMillis < 0) {
            throw new IllegalArgumentException("运行预算不能为负数。");
        }
    }

    /**
     * 构建默认运行预算配置。
     *
     * @return 默认的 RuntimeBudget 实例
     */
    public static RuntimeBudget defaults() {
        return new RuntimeBudget(DEFAULT_MAX_STEPS, DEFAULT_MAX_TOKENS,
                DEFAULT_MAX_COST_MICROS, DEFAULT_TIMEOUT_MILLIS);
    }
}

