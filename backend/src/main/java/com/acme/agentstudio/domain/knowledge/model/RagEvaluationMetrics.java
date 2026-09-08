package com.acme.agentstudio.domain.knowledge.model;

import java.math.BigDecimal;

/**
 * RAG 检索评测质量量化指标明细实体 Record（RAG Evaluation Metrics）。
 * 包含评测模式 mode (RagEvaluationMode)、测试集 Query 总数 queryCount、
 * 召回率 recallAtK (0.0~1.0)、NDCG@K 排序质量分 ndcgAtK、首位命中率 firstHitRate、
 * 平均检索延迟毫秒数 averageLatencyMs、Reranker 重排器调用次数 rerankerCalls 及 Reranker 消耗金额成本 rerankerCost。
 *
 * @param mode 评测模式（OFFLINE_GOLDEN / ONLINE_REPLAY）
 * @param queryCount 参与评估测验的 Query 用例总条数
 * @param recallAtK Top-K 召回率覆盖比率（Recall@K）
 * @param ndcgAtK 归一化折损累计收益排序分（NDCG@K）
 * @param firstHitRate 搜索结果首条即精确命中的比例（First Hit Rate）
 * @param averageLatencyMs 耗时平均时延（毫秒）
 * @param rerankerCalls 重排模型总调用次数
 * @param rerankerCost 重排模型消耗金额
 */
public record RagEvaluationMetrics(
        RagEvaluationMode mode,
        int queryCount,
        double recallAtK,
        double ndcgAtK,
        double firstHitRate,
        double averageLatencyMs,
        int rerankerCalls,
        BigDecimal rerankerCost
) {
}

