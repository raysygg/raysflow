package com.acme.agentstudio.infrastructure.model.strategy.embedding;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini 原生 Embedding 向量化策略实现类（Google Gemini Embedding Strategy）。
 * 通过 RestClient 调用 Google Generative Language REST API（/v1beta/models/{model}:embedContent）进行原生向量生成。
 */
@Component
public class GoogleGeminiEmbeddingStrategy implements EmbeddingCallStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeminiEmbeddingStrategy.class);

    /** RAG 检索配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入依赖。
     *
     * @param properties RAG 检索配置属性
     */
    public GoogleGeminiEmbeddingStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验支持的供应商类型：仅支持 ModelProvider.GOOGLE_GEMINI。
     *
     * @param provider 模型供应商枚举
     * @return true 表示为 GOOGLE_GEMINI 供应商
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.GOOGLE_GEMINI;
    }

    /**
     * 构造 Gemini 专属的 EmbeddingModel 实例。
     *
     * @param connection 连接配置
     * @return GeminiEmbeddingModel 包装对象
     */
    @Override
    public EmbeddingModel createEmbeddingModel(RagModelConnectionResolver.ModelConnection connection) {
        return new GeminiEmbeddingModel(connection, properties);
    }

    /** Gemini 原生 HTTP 接口实现类 */
    private static class GeminiEmbeddingModel implements EmbeddingModel {

        private final RagModelConnectionResolver.ModelConnection connection;
        private final RagProperties properties;

        public GeminiEmbeddingModel(RagModelConnectionResolver.ModelConnection connection, RagProperties properties) {
            this.connection = connection;
            this.properties = properties;
        }

        /** 批量生成文本段落向量 */
        @Override
        public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            List<Embedding> embeddings = new ArrayList<>();
            for (TextSegment segment : textSegments) {
                embeddings.add(embedSingle(segment.text()));
            }
            return Response.from(embeddings);
        }

        /** 单文本 REST API 向量请求 */
        private Embedding embedSingle(String text) {
            String baseUrl = (connection.baseUrl() != null) ? connection.baseUrl().trim() : "";
            if (baseUrl.isEmpty()) {
                baseUrl = "https://generativelanguage.googleapis.com";
            }
            String uri = String.format("/v1beta/models/%s:embedContent?key=%s", connection.modelKey(), connection.apiKey());

            GeminiEmbedRequest request = new GeminiEmbedRequest(
                    new GeminiEmbedContent(List.of(new GeminiEmbedPart(text)))
            );

            try {
                int timeoutSeconds = (properties != null) ? properties.getModelTimeoutSeconds() : 60;
                int timeoutMs = Math.max(1, timeoutSeconds) * 1000;

                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(timeoutMs);
                factory.setReadTimeout(timeoutMs);

                GeminiEmbedResponse response = RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl(baseUrl)
                        .build()
                        .post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(GeminiEmbedResponse.class);

                if (response != null && response.embedding() != null && response.embedding().values() != null) {
                    List<Double> values = response.embedding().values();
                    float[] floats = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        floats[i] = values.get(i).floatValue();
                    }
                    return Embedding.from(floats);
                }
                throw new IllegalStateException("Google Gemini API 返回了空的 embedding values。");
            } catch (Exception e) {
                LOG.error("调用 Google Gemini Embed API 失败，baseUrl={}, modelKey={}, error={}", baseUrl, connection.modelKey(), e.getMessage());
                throw new RuntimeException("Gemini 向量提取处理失败：" + e.getMessage(), e);
            }
        }
    }

    /** Gemini API 向量请求 Record */
    private record GeminiEmbedRequest(GeminiEmbedContent content) {
    }

    /** Gemini API 请求 Payload Content Record */
    private record GeminiEmbedContent(List<GeminiEmbedPart> parts) {
    }

    /** Gemini API 请求 Part Record */
    private record GeminiEmbedPart(String text) {
    }

    /** Gemini API 向量响应 Record */
    private record GeminiEmbedResponse(GeminiEmbeddingValues embedding) {
    }

    /** Gemini API 向量数值 Vector Values Record */
    private record GeminiEmbeddingValues(List<Double> values) {
    }
}

