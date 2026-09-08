package com.acme.agentstudio.infrastructure.rag.model;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.domain.knowledge.model.RerankerExecutionResult;
import com.acme.agentstudio.domain.knowledge.port.RerankerProvider;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.model.strategy.ModelProviderStrategyFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通用 Reranker 重排模型统一路由提供商（OpenAI Compatible Reranker Provider）。
 * 统一由 ModelProviderStrategyFactory 分发至具体的 Reranker 重排策略，
 * 支持标准 OpenAPI 展平结构与 DashScope 原生嵌套结构。
 */
@Component
public class OpenAiCompatibleRerankerProvider implements RerankerProvider {
    private final RagModelConnectionResolver connectionResolver;
    private final ModelProviderStrategyFactory strategyFactory;

    public OpenAiCompatibleRerankerProvider(RagModelConnectionResolver connectionResolver, ModelProviderStrategyFactory strategyFactory) {
        this.connectionResolver = connectionResolver;
        this.strategyFactory = strategyFactory;
    }

        /**
         * rerank 方法。
         *
         * @param profile profile 参数
         * @param query query 参数
         * @param candidates candidates 参数
         * @return RerankerExecutionResult 返回对象
         */
    @Override
    public RerankerExecutionResult rerank(RagEmbeddingProfile profile, String query, List<RerankCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return RerankerExecutionResult.withoutUsage(List.of());
        String rerankerKey = profile.rerankerModelKey();
        if (rerankerKey == null || rerankerKey.isBlank()) {
            rerankerKey = connectionResolver.findActiveRerankerKey(profile.tenantId());
        }
        if (rerankerKey == null || rerankerKey.isBlank()) {
            // 未配置远程 Reranker 时平滑降级为标准向量检索
            return RerankerExecutionResult.withoutUsage(List.of());
        }
        RagModelConnectionResolver.ModelConnection connection =
                connectionResolver.resolve(profile.tenantId(), rerankerKey);

        ModelProvider provider = ModelProvider.fromCode(connection.modelKey());
        return RerankerExecutionResult.withoutUsage(
                strategyFactory.getRerankerStrategy(provider).rerank(connection, query, candidates));
    }
}
