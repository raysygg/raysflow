package com.acme.agentstudio.domain.metrics.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户与全平台模型用量总览统计实体 Record（Model Usage Summary）。
 * 包含统计起始时间 from、总调用次数 totalCalls、成功次数 successCalls、失败次数 failedCalls、
 * 输入 Token 数 inputTokens、输出 Token 数 outputTokens、总花费金额 totalCost 及金额计费是否可用标志 costAvailable。
 *
 * @param from 统计周期起始时间戳
 * @param totalCalls 周期内发生的模型总调用次数
 * @param successCalls 成功返回结果的次数
 * @param failedCalls 调用抛错异常的次数
 * @param inputTokens 输入 Prompt Token 消费总量
 * @param outputTokens 输出 Completion Token 消费总量
 * @param totalCost 周期内消费的总金额（元）
 * @param costAvailable 是否成功计算并提供了有效成本金额
 */
public record ModelUsageSummary(
        LocalDateTime from,
        long totalCalls,
        long successCalls,
        long failedCalls,
        long inputTokens,
        long outputTokens,
        BigDecimal totalCost,
        boolean costAvailable
) {
}

