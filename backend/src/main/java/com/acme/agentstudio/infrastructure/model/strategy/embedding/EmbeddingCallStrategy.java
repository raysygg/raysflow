package com.acme.agentstudio.infrastructure.model.strategy.embedding;

import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * 基于 LangChain4j 框架体系的 Embedding 文本向量化策略接口（Embedding Call Strategy）。
 */
public interface EmbeddingCallStrategy {

    /**
     * 根据模型连接契约创建 LangChain4j 的 EmbeddingModel 向量模型实例。
     *
     * @param connection 包含了配置 Base URL、API Key 与 ModelKey 的连接对象
     * @return 实例化后的 LangChain4j EmbeddingModel 实例
     */
    EmbeddingModel createEmbeddingModel(RagModelConnectionResolver.ModelConnection connection);

    /**
     * 判断当前策略是否匹配特定的大模型供应商（默认 OpenAI 策略返回 false，特化供应商重写该方法返回 true）。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    default boolean supports(ModelProvider provider) {
        return false;
    }
}

