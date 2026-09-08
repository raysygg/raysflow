package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 检索离线评测与对比试验模式枚举（RAG Evaluation Mode）。
 */
public enum RagEvaluationMode {

    /** 仅使用 KNN 稠密向量召回 */
    DENSE_ONLY,

    /** 稠密向量 + BM25 稀疏向量双路召回 */
    DENSE_SPARSE,

    /** 稠密向量 + 稀疏向量 + 关键词精确三路混合召回 */
    DENSE_SPARSE_EXACT,

    /** 多路混合召回 + Reranker 模型精排 */
    HYBRID_RERANKER
}

