package com.acme.agentstudio.infrastructure.model.strategy.llm;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Google Gemini 厂商原生 REST 接口大语言模型 LLM 调用策略实现类（Google Gemini LLM Strategy）。
 * 透传 connection.baseUrl()，使用 RestClient 直接调用 Gemini REST API `generateContent` 端点。
 */
@Component
public class GoogleGeminiLlmStrategy implements LlmCallStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeminiLlmStrategy.class);

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public GoogleGeminiLlmStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前策略是否匹配特定的大模型供应商：仅支持 GOOGLE_GEMINI。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.GOOGLE_GEMINI;
    }

    /**
     * 构造基于自定义 GeminiChatLanguageModel 的 LangChain4j 聊天模型实例。
     *
     * @param connection 模型连接契约配置
     * @return ChatLanguageModel 实例
     */
    @Override
    public ChatLanguageModel createChatModel(RagModelConnectionResolver.ModelConnection connection) {
        return new GeminiChatLanguageModel(connection, properties);
    }

    /** Google Gemini REST 模式自定义 ChatLanguageModel 实现类 */
    private static class GeminiChatLanguageModel implements ChatLanguageModel {

        private final RagModelConnectionResolver.ModelConnection connection;
        private final RagProperties properties;

        public GeminiChatLanguageModel(RagModelConnectionResolver.ModelConnection connection, RagProperties properties) {
            this.connection = connection;
            this.properties = properties;
        }

        /**
         * 拼接 prompt 消息列表并请求 Gemini API。
         *
         * @param messages 聊天消息列表 List&lt;ChatMessage&gt;
         * @return Response&lt;AiMessage&gt; 响应包装对象
         */
        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            StringBuilder prompt = new StringBuilder();
            for (ChatMessage msg : messages) {
                if (msg != null && msg.text() != null) {
                    prompt.append(msg.text()).append("\n");
                }
            }
            String resultText = generateTextInternal(prompt.toString().trim());
            return Response.from(AiMessage.from(resultText));
        }

        private String generateTextInternal(String promptText) {
            String baseUrl = (connection.baseUrl() != null) ? connection.baseUrl().trim() : "";
            if (baseUrl.isEmpty()) {
                baseUrl = "https://generativelanguage.googleapis.com";
            }
            String uri = String.format("/v1beta/models/%s:generateContent?key=%s", connection.modelKey(), connection.apiKey());

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiContent("user", List.of(new GeminiPart(promptText))))
            );

            try {
                int timeoutMs = Math.max(1, (properties != null) ? properties.getModelTimeoutSeconds() : 60) * 1000;
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(timeoutMs);
                factory.setReadTimeout(timeoutMs);

                GeminiResponse response = RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl(baseUrl)
                        .build()
                        .post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(GeminiResponse.class);

                if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                    GeminiCandidate candidate = response.candidates().get(0);
                    if (candidate.content() != null && candidate.content().parts() != null && !candidate.content().parts().isEmpty()) {
                        return candidate.content().parts().get(0).text().trim();
                    }
                }
                throw new IllegalStateException("Google Gemini 未返回有效的 text 内容。");
            } catch (Exception e) {
                LOG.error("调用 Google Gemini API 失败，baseUrl={}, modelKey={}, error={}", baseUrl, connection.modelKey(), e.getMessage());
                throw new RuntimeException("Gemini 模型生成失败: " + e.getMessage(), e);
            }
        }
    }

    /** Gemini API 请求结构 Record */
    private record GeminiRequest(List<GeminiContent> contents) {
    }

    /** Gemini API 内容块 Record */
    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    /** Gemini API Part 块 Record */
    private record GeminiPart(String text) {
    }

    /** Gemini API 响应结构 Record */
    private record GeminiResponse(List<GeminiCandidate> candidates) {
    }

    /** Gemini API 候选对象 Record */
    private record GeminiCandidate(GeminiContent content) {
    }
}

