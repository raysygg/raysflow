package com.acme.agentstudio.domain.runtime.model;

/**
 * 租户与 Agent 应用的资源用量与速率限制配额 Record（Runtime Quota）。
 * 限制最大并发运行数 maxConcurrentRuns、每分钟最大触发请求数 maxRunsPerMinute、
 * 每日最大允许消费 Token 数 maxTokensPerDay、每日最大消费金额微元 maxCostMicrosPerDay 与单次运行最大工具调用次数 maxToolCallsPerRun。
 *
 * @param maxConcurrentRuns 租户全局最大并发执行 Run 任务上限
 * @param maxRunsPerMinute 每分钟允许发起的最大 Run 任务数 (Rate Limit)
 * @param maxTokensPerDay 每日配额允许消耗的最大 Token 数上限
 * @param maxCostMicrosPerDay 每日配额允许消耗的最大微元金额上限
 * @param maxToolCallsPerRun 单次 Run 任务允许调用的最大外部工具次数
 */
public record RuntimeQuota(
        int maxConcurrentRuns,
        int maxRunsPerMinute,
        long maxTokensPerDay,
        long maxCostMicrosPerDay,
        int maxToolCallsPerRun
) {
    /** 紧凑构造函数做正数边界断言校验 */
    public RuntimeQuota {
        if (maxConcurrentRuns < 1 || maxRunsPerMinute < 1 || maxTokensPerDay < 1
                || maxCostMicrosPerDay < 0 || maxToolCallsPerRun < 1) {
            throw new IllegalArgumentException("Runtime 配额必须满足正数边界");
        }
    }
}

