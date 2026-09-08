package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 检索权限与范围约束类型枚举（Retrieval Scope Type）。
 */
public enum RetrievalScopeType {

    /** 显式绑定的特定文档列表集合 */
    EXPLICIT_DOCUMENTS,

    /** 租户或当前用户可见的所有有效文档范围 */
    VISIBLE_DOCUMENTS
}

