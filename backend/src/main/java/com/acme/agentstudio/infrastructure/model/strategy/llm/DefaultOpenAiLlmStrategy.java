package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 默认 OpenAI 兼容协议大语言模型 LLM 调用策略实现类（Default OpenAI LLM Strategy）。
 * 原生集成 LangChain4j OpenAiChatModel，直接透传 connection.baseUrl()、API Key 与 ModelKey，
 * 适用于 OpenAI, DeepSeek, Kimi, 智谱 GLM, 豆包 Volcengine, 混元, Grok 等兼容 OpenAI REST 规范的模型厂商。
 */
@Component
public class DefaultOpenAiLlmStrategy implements LlmCallStrategy {

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public DefaultOpenAiLlmStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 构造基于 OpenAiChatModel 的 LangChain4j 聊天模型实例。
     *
     * @param connection 模型连接契约配置
     * @return ChatLanguageModel 实例
     */
    @Override
    public ChatLanguageModel createChatModel(RagModelConnectionResolver.ModelConnection connection) {
        int timeoutSeconds = (properties != null) ? Math.max(1, properties.getModelTimeoutSeconds()) : 60;

        return OpenAiChatModel.builder()
                .apiKey(connection.apiKey())
                .baseUrl(connection.baseUrl().trim())
                .modelName(connection.modelKey())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}

