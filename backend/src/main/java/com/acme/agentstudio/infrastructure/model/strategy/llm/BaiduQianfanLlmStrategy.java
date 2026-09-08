package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 百度千帆 (文心一言) 厂商特化 LLM 大模型调用策略实现类（Baidu Qianfan LLM Strategy）。
 * 透传 connection.baseUrl()，使用兼容规范对接百度千帆大模型 v2 REST 接口。
 */
@Component
public class BaiduQianfanLlmStrategy implements LlmCallStrategy {

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public BaiduQianfanLlmStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前策略是否匹配特定的大模型供应商：仅支持 BAIDU_QIANFAN。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.BAIDU_QIANFAN;
    }

    /**
     * 构造基于 OpenAiChatModel 兼容模式的百度千帆 ChatLanguageModel 实例。
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

