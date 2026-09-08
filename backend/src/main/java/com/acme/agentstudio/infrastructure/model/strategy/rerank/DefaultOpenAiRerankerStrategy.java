package com.acme.agentstudio.infrastructure.model.strategy.rerank;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用标准 Reranker 重排序默认策略实现类（Default OpenAI Reranker Strategy）。
 * 透传 connection.baseUrl()，自动识别展平结构（Standard Request/Response）与嵌套结构（Nested Input/Output），
 * 适用于 Jina, BGE-Reranker, Cohere, SiliconFlow, FastChat 等提供 Rerank 接口的计算服务。
 */
@Component
public class DefaultOpenAiRerankerStrategy implements RerankerCallStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultOpenAiRerankerStrategy.class);

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public DefaultOpenAiRerankerStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 调用通用 Reranker 端点执行文档片段重排序 Scoring。
     *
     * @param connection 模型连接契约配置
     * @param query 查询 Prompt
     * @param candidates 候选文档段落列表 List&lt;RerankCandidate&gt;
     * @return List&lt;RerankResult&gt; 重排序得分结果列表
     */
    @Override
    public List<RerankResult> rerank(RagModelConnectionResolver.ModelConnection connection, String query, List<RerankCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<String> docTexts = candidates.stream().map(RerankCandidate::text).toList();
        String rawBaseUrl = connection.baseUrl().trim();
        String modelKey = connection.modelKey();

        List<Object> requestPayloads = List.of(
                new StandardRerankRequest(modelKey, query, docTexts, candidates.size()),
                new NestedRerankRequest(modelKey, new NestedInput(query, docTexts), new NestedParameters(candidates.size()))
        );

        String uri = (rawBaseUrl.endsWith("/rerank") || rawBaseUrl.contains("/text-rerank")) ? "" : "/rerank";

        for (Object payload : requestPayloads) {
            try {
                RerankResponse response = RestClient.builder()
                        .requestFactory(requestFactory())
                        .baseUrl(rawBaseUrl)
                        .build()
                        .post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + connection.apiKey())
                        .body(payload)
                        .retrieve()
                        .body(RerankResponse.class);

                List<RerankItem> items = (response == null) ? List.of() : response.extractResults();
                if (!items.isEmpty()) {
                    List<RerankResult> results = new ArrayList<>();
                    for (RerankItem item : items) {
                        if (item.index() < 0 || item.index() >= candidates.size()) {
                            continue;
                        }
                        results.add(new RerankResult(candidates.get(item.index()).chunkId(), item.getScore()));
                    }
                    return List.copyOf(results);
                }
            } catch (RuntimeException exception) {
                LOG.debug("尝试 Reranker 载荷推演，modelKey={}, error={}", modelKey, exception.getMessage());
            }
        }

        LOG.warn("Reranker 接口未返回有效结果，modelKey={}, baseUrl={}", modelKey, rawBaseUrl);
        return List.of();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        int timeoutMs = Math.max(1, (properties != null) ? properties.getModelTimeoutSeconds() : 60) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return factory;
    }

    /** 标准展平式 Rerank 请求包 Record */
    private record StandardRerankRequest(
            String model,
            String query,
            List<String> documents,
            @JsonProperty("top_n") int topN
    ) {
    }

    /** 嵌套结构 Rerank 请求包 Record */
    private record NestedRerankRequest(
            String model,
            NestedInput input,
            NestedParameters parameters
    ) {
    }

    /** 嵌套输入 Record */
    private record NestedInput(
            String query,
            List<String> documents
    ) {
    }

    /** 嵌套控制参数 Record */
    private record NestedParameters(
            @JsonProperty("top_n") int topN
    ) {
    }

    /** 兼容响应包装 Record */
    private record RerankResponse(
            List<RerankItem> results,
            List<RerankItem> data,
            NestedOutput output
    ) {
        /**
         * 多格式自动兼容萃取 Rerank 得分列表。
         *
         * @return 列表结果
         */
        public List<RerankItem> extractResults() {
            if (results != null && !results.isEmpty()) {
                return results;
            }
            if (data != null && !data.isEmpty()) {
                return data;
            }
            if (output != null && output.results() != null) {
                return output.results();
            }
            return List.of();
        }
    }

    /** 嵌套输出结构容器 Record */
    private record NestedOutput(List<RerankItem> results) {
    }

    /** 单个文档排序结果条目 Record */
    private record RerankItem(
            int index,
            @JsonProperty("relevance_score") Double relevanceScore,
            @JsonProperty("score") Double score
    ) {
        /**
         * 萃取标准 Relevance Score 分值。
         *
         * @return 得分 double
         */
        public double getScore() {
            if (relevanceScore != null) {
                return relevanceScore;
            }
            if (score != null) {
                return score;
            }
            return 0.0;
        }
    }
}

