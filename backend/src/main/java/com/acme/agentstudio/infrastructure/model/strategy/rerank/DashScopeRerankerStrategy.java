package com.acme.agentstudio.infrastructure.model.strategy.rerank;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.domain.model.ModelProvider;
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
 * 阿里 DashScope (通义千问) 特化 Reranker 重排序策略实现类（DashScope Reranker Strategy）。
 * 透传 connection.baseUrl()，通过 REST API 直接调用阿里云 DashScope /services/rerank/text-rerank/rerank 端点。
 */
@Component
public class DashScopeRerankerStrategy implements RerankerCallStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DashScopeRerankerStrategy.class);

    /** RAG 与模型配置属性 */
    private final RagProperties properties;

    /**
     * 构造函数注入 RagProperties。
     *
     * @param properties RAG 配置属性
     */
    public DashScopeRerankerStrategy(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前策略是否匹配特定的模型供应商：仅支持 ALIBABA_DASHSCOPE。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    @Override
    public boolean supports(ModelProvider provider) {
        return provider == ModelProvider.ALIBABA_DASHSCOPE;
    }

    /**
     * 调用 DashScope API 执行候选文档段落与查询 Prompt 的重排序 Scoring。
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
        String targetBaseUrl = connection.baseUrl().trim();
        String modelKey = connection.modelKey();

        String uri = (targetBaseUrl.endsWith("/rerank") || targetBaseUrl.contains("/text-rerank")) ? "" : "/rerank";

        DashScopeRerankRequest request = new DashScopeRerankRequest(
                modelKey,
                new DashScopeInput(query, docTexts),
                new DashScopeParameters(candidates.size())
        );

        try {
            DashScopeRerankResponse response = RestClient.builder()
                    .requestFactory(requestFactory())
                    .baseUrl(targetBaseUrl)
                    .build()
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + connection.apiKey())
                    .body(request)
                    .retrieve()
                    .body(DashScopeRerankResponse.class);

            List<DashScopeItem> items = (response != null && response.output() != null) ? response.output().results() : List.of();
            if (!items.isEmpty()) {
                List<RerankResult> results = new ArrayList<>();
                for (DashScopeItem item : items) {
                    if (item.index() < 0 || item.index() >= candidates.size()) {
                        continue;
                    }
                    results.add(new RerankResult(candidates.get(item.index()).chunkId(), item.relevanceScore()));
                }
                return List.copyOf(results);
            }
        } catch (Exception e) {
            LOG.warn("调用 DashScope Reranker 接口失败，modelKey={}, baseUrl={}, error={}", modelKey, targetBaseUrl, e.getMessage());
        }

        return List.of();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        int timeoutMs = Math.max(1, (properties != null) ? properties.getModelTimeoutSeconds() : 60) * 1000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return factory;
    }

    /** DashScope Rerank 请求体 Record */
    private record DashScopeRerankRequest(String model, DashScopeInput input, DashScopeParameters parameters) {
    }

    /** DashScope Rerank 输入参数 Record */
    private record DashScopeInput(String query, List<String> documents) {
    }

    /** DashScope Rerank 控制参数 Record */
    private record DashScopeParameters(@JsonProperty("top_n") int topN) {
    }

    /** DashScope Rerank 响应结构 Record */
    private record DashScopeRerankResponse(DashScopeOutput output) {
    }

    /** DashScope Output 内部容器 Record */
    private record DashScopeOutput(List<DashScopeItem> results) {
    }

    /** DashScope 单个得分条目 Record */
    private record DashScopeItem(int index, @JsonProperty("relevance_score") double relevanceScore) {
    }
}

