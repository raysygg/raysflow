package com.acme.agentstudio.domain.knowledge.model;

/**
 * 重排模型（Reranker）精排得分输出结果 Record（Rerank Result）。
 * 包含物理切块物理 ID chunkId 及重排模型输出的相似度打分 score。
 *
 * @param chunkId 归属切块 ID
 * @param score 精排得分
 */
public record RerankResult(
        Long chunkId,
        double score
) {
}


