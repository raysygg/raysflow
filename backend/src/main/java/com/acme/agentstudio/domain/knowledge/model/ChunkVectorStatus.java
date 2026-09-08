package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识切块（Chunk）向量索引生成状态枚举（Chunk Vector Status）。
 */
public enum ChunkVectorStatus {

    /** 待切块/待计算 Embedding 向量生成中 */
    PENDING,

    /** 向量索引计算完毕并成功写入 Qdrant 就绪 */
    READY,

    /** 向量生成或构建索引失败 */
    FAILED,

    /** 已被禁用 */
    DISABLED
}


