package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识多路召回通道类型枚举（Retrieval Channel）。
 */
public enum RetrievalChannel {

    /** KNN 稠密向量召回通道 */
    VECTOR,

    /** 中文词法 BM25 召回通道 */
    LEXICAL_ZH,

    /** 英文词法 BM25 召回通道 */
    LEXICAL_EN,

    /** 通用多语言词法 BM25 召回通道 */
    LEXICAL_GENERAL,

    /** 关键词/摘要精确哈希匹配召回通道 */
    EXACT
}

