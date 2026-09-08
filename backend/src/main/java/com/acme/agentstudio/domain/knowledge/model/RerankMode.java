package com.acme.agentstudio.domain.knowledge.model;

/**
 * 单次检索任务实际生效并采用的重排与融合排序模式枚举（Rerank Mode）。
 */
public enum RerankMode {

    /** 标准多路 RRF 融合排序 */
    STANDARD_HYBRID,

    /** 专用多语言 Cross-Encoder Reranker 模型重排 */
    MULTILINGUAL_RERANKER,

    /** 降级退回多路融合排序 */
    HYBRID_FALLBACK,

    /** 降级退回单一向量向量排序（兼容旧版历史） */
    VECTOR_FALLBACK
}

