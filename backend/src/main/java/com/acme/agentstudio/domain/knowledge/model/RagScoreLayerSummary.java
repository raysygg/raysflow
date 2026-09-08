package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 调试全景视图中各多路召回与重排分层最高得分统计摘要 Record（RAG Score Layer Summary）。
 * 包含稠密向量最高分 dense、词法/BM25 向量最高分 sparse、精确匹配最高分 exact、
 * RRF 多路融合最高分 fusion 及 Reranker 重排器最高分 rerank。
 *
 * @param dense 稠密向量检索最高得分
 * @param sparse 词法/BM25 向量检索最高得分
 * @param exact 关键词精确匹配最高得分
 * @param fusion RRF 融合打分最高分
 * @param rerank Reranker 重排模型最高得分
 */
public record RagScoreLayerSummary(
        double dense,
        double sparse,
        double exact,
        double fusion,
        double rerank
) {
}

