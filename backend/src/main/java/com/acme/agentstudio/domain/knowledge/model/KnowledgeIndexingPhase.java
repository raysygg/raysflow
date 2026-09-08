package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库文档向量化与索引生成实时阶段枚举（Knowledge Indexing Phase）。
 */
public enum KnowledgeIndexingPhase {

    /** 队列等待中 */
    QUEUED,

    /** 文本解析中 */
    PARSING,

    /** 智能切分切块中 */
    CHUNKING,

    /** 调用 Embedding 向量计算中 */
    EMBEDDING,

    /** 写入向量数据库持久化中 */
    PERSISTING,

    /** 索引全部处理完成 */
    COMPLETED,

    /** 索引生成失败 */
    FAILED
}

