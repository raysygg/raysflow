package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识文档解析、切块与 RAG 混合检索支持的语种分类枚举（Knowledge Language）。
 */
public enum KnowledgeLanguage {

    /** 中文（Simplified/Traditional Chinese） */
    ZH,

    /** 英文（English） */
    EN,

    /** 中英双语混合 */
    MIXED,

    /** 其他多语言 */
    OTHER
}

