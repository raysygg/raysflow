package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Anthropic Claude 厂商特化 LLM 模型策略实现类（Anthropic Claude LLM Strategy）。
 * 基于 LangChain4j AnthropicChatModel，支持自定义 Base URL 代理及超时时间配置。
 */
@Component
public class AnthropicClaudeLlmStrategy implements LlmCallStrategy {

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    public AnthropicClaudeLlmStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前策略是否匹配特定的大模型供应商：仅支持 ANTHROPIC_CLAUDE。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.ANTHROPIC_CLAUDE;
    }

    /**
     * 构造基于 AnthropicChatModel 的 LangChain4j 聊天模型实例。
     *
     * @param connection 模型连接契约配置
     * @return ChatLanguageModel 实例
     */
    @Override
    public ChatLanguageModel createChatModel(RagModelConnectionResolver.ModelConnection connection) {
        int timeoutSeconds = (properties != null) ? Math.max(1, properties.getModelTimeoutSeconds()) : 60;

        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .apiKey(connection.apiKey())
                .modelName(connection.modelKey())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false);

        if (connection.baseUrl() != null && !connection.baseUrl().isBlank()) {
            builder.baseUrl(connection.baseUrl().trim());
        }

        return builder.build();
    }
}

