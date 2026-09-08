package com.acme.agentstudio.domain.knowledge.model;

/**
 * 检索结果可解释命中归因枚举（Retrieval Hit Reason）。
 */
public enum RetrievalHitReason {

    /** 向量余弦/欧式距离相似度高 */
    VECTOR_SIMILARITY,

    /** 词法 BM25 词频匹配 */
    SPARSE_TERM_MATCH,

    /** 关键词/标识符精确哈希命中 */
    EXACT_IDENTIFIER,

    /** 多路 RRF 综合混合命中 */
    HYBRID_MATCH
}

