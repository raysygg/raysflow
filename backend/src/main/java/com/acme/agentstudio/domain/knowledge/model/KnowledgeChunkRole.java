package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 知识切块角色（Parent/Child Segment Role）枚举。
 */
public enum KnowledgeChunkRole {

    /** 父切片：用于扩展包含完整滑窗与上下文还原 */
    PARENT,

    /** 子切片：写入向量索引库，用于精确距离相关度召回 */
    CHILD
}

