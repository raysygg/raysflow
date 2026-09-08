package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ollama 本地部署大语言模型 LLM 调用策略实现类（Ollama LLM Strategy）。
 * 直接使用配置的 connection.baseUrl()（例如 http://localhost:11434/v1），在未显式提供 API Key 时填充默认占位符 "ollama"。
 */
@Component
public class OllamaLlmStrategy implements LlmCallStrategy {

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public OllamaLlmStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前策略是否匹配特定的大模型供应商：仅支持 OLLAMA。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.OLLAMA;
    }

    /**
     * 构造基于 OpenAiChatModel 兼容模式的 Ollama 本地 ChatLanguageModel 实例。
     *
     * @param connection 模型连接契约配置
     * @return ChatLanguageModel 实例
     */
    @Override
    public ChatLanguageModel createChatModel(RagModelConnectionResolver.ModelConnection connection) {
        int timeoutSeconds = (properties != null) ? Math.max(1, properties.getModelTimeoutSeconds()) : 60;
        String apiKey = (connection.apiKey() != null && !connection.apiKey().isBlank()) ? connection.apiKey() : "ollama";

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(connection.baseUrl().trim())
                .modelName(connection.modelKey())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}

