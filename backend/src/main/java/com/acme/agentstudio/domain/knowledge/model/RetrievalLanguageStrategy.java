package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 节点选择与控制检索语种匹配策略枚举（Retrieval Language Strategy）。
 */
public enum RetrievalLanguageStrategy {

    /** 强制以文档主语种为准进行检索 */
    DOCUMENT,

    /** 强制以 Query 输入语种为准进行检索 */
    QUERY,

    /** 自动多语言混合跨语言检索 */
    AUTO
}

