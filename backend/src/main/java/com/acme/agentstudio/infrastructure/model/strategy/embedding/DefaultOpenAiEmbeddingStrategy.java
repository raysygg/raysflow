package com.acme.agentstudio.infrastructure.model.strategy.embedding;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 默认 OpenAI 兼容协议文本向量化 Embedding 策略实现类（Default OpenAI Embedding Strategy）。
 * 直接使用 connection.baseUrl() 配置并构造 OpenAiEmbeddingModel 实例，支持多种第三方 OpenAI 兼容 API 规范。
 */
@Component
public class DefaultOpenAiEmbeddingStrategy implements EmbeddingCallStrategy {

    /** RAG 检索配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties 配置对象。
     *
     * @param properties RAG 检索配置属性
     */
    public DefaultOpenAiEmbeddingStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 根据传入的连接配置构造基于 OpenAiEmbeddingModel 的 LangChain4j 向量模型实例。
     *
     * @param connection 包含了配置 Base URL、API Key 与 ModelKey 的连接对象
     * @return 实例化后的 EmbeddingModel 实例
     */
    @Override
    public EmbeddingModel createEmbeddingModel(RagModelConnectionResolver.ModelConnection connection) {
        int timeoutSeconds = (properties != null) ? Math.max(1, properties.getModelTimeoutSeconds()) : 60;

        return OpenAiEmbeddingModel.builder()
                .apiKey(connection.apiKey())
                .baseUrl(connection.baseUrl().trim())
                .modelName(connection.modelKey())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}

