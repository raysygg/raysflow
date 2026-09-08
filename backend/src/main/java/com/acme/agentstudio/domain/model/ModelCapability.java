package com.acme.agentstudio.domain.model;

/**
 * 模型能力分类枚举（Model Capability）。
 * 声明模型支持的服务能力，避免将文本生成对话大模型误用于 Embedding 向量化或交叉注意力 Reranker 重排调用。
 */
public enum ModelCapability {

    /** LLM 大语言模型对话与生成能力 */
    CHAT,

    /** 文本向量化 Dense Embedding 提取能力 */
    EMBEDDING,

    /** 交叉注意力 Reranker 精排重排打分能力 */
    RERANKER
}

