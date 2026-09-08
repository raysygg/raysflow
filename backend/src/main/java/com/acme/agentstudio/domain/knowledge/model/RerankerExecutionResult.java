package com.acme.agentstudio.domain.knowledge.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 重排模型 Reranker 调用执行强类型结果实体 Record（Reranker Execution Result）。
 * 包含重排打分项列表 scores (List&lt;RerankResult&gt;)、模型 Token 消费用量 usage (RerankerUsage)
 * 及计算扣减金额 cost (BigDecimal)。
 *
 * @param scores 重排打分结果项列表
 * @param usage 模型 Token 用量
 * @param cost 法币消耗金额
 */
public record RerankerExecutionResult(
        List<RerankResult> scores,
        RerankerUsage usage,
        BigDecimal cost
) {
    /** 紧凑构造函数做防空保护 */
    public RerankerExecutionResult {
        scores = (scores == null) ? List.of() : List.copyOf(scores);
        usage = (usage == null) ? RerankerUsage.unknown() : usage;
    }

    /**
     * 创建无 Token 用量信息的重排结果实例。
     *
     * @param scores 重排打分列表
     * @return RerankerExecutionResult 对象
     */
    public static RerankerExecutionResult withoutUsage(List<RerankResult> scores) {
        return new RerankerExecutionResult(scores, RerankerUsage.unknown(), null);
    }

    /**
     * 附加上预估的 Token 消费与预估金额成本。
     *
     * @param estimatedInputTokens 预估 Prompt 输入 Token 数
     * @param estimatedCost 预估金额（元）
     * @return 包含用量估计的全新 RerankerExecutionResult 实例
     */
    public RerankerExecutionResult withEstimate(long estimatedInputTokens, BigDecimal estimatedCost) {
        RerankerUsage effectiveUsage = (usage.inputTokens() > 0L)
                ? usage : new RerankerUsage(estimatedInputTokens, 0L, true);
        BigDecimal effectiveCost = (cost == null) ? estimatedCost : cost;
        return new RerankerExecutionResult(scores, effectiveUsage, effectiveCost);
    }
}

