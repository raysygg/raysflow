package com.acme.agentstudio.domain.metrics.model;

import java.math.BigDecimal;

/**
 * 平台与租户模型调用量、Token 及金额成本多维度拆分明细实体 Record（Model Usage Breakdown）。
 * 包含统计维度 key/名称 dimension、总调用次数 calls、成功次数 successCalls、
 * 失败次数 failedCalls、输入 Input Token 数 inputTokens、输出 Output Token 数 outputTokens 及消耗金额 totalCost。
 *
 * @param dimension 统计维度 Key（如模型 Key、应用 ID、提供商等）
 * @param calls 累计发起总调用次数
 * @param successCalls 成功调用次数
 * @param failedCalls 异常失败调用次数
 * @param inputTokens 输入 Prompt Token 消费总量
 * @param outputTokens 文本 Completion Token 消费总量
 * @param totalCost 预估/精确计算的法币消费金额（元）
 */
public record ModelUsageBreakdown(
        String dimension,
        long calls,
        long successCalls,
        long failedCalls,
        long inputTokens,
        long outputTokens,
        BigDecimal totalCost
) {
}

