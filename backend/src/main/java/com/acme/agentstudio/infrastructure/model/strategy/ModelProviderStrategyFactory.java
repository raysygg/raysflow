package com.acme.agentstudio.infrastructure.model.strategy;

import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.model.strategy.embedding.DefaultOpenAiEmbeddingStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.embedding.EmbeddingCallStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.llm.DefaultOpenAiLlmStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.llm.LlmCallStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.rerank.DefaultOpenAiRerankerStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.rerank.RerankerCallStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 供应商多能力策略总工厂组件（Model Provider Strategy Factory）。
 * 遵循策略与工厂模式（Strategy & Factory Pattern），按能力划分为 llm、embedding 及 rerank 三大子包。
 * 遵循“配置什么就是什么”原则，透传 baseUrl 与配置，并根据 ModelProvider 自动路由派发特化策略实现。
 */
@Component
public class ModelProviderStrategyFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ModelProviderStrategyFactory.class);

    private final DefaultOpenAiLlmStrategy defaultLlmStrategy;
    private final List<LlmCallStrategy> llmStrategies;

    private final DefaultOpenAiEmbeddingStrategy defaultEmbeddingStrategy;
    private final List<EmbeddingCallStrategy> embeddingStrategies;

    private final DefaultOpenAiRerankerStrategy defaultRerankerStrategy;
    private final List<RerankerCallStrategy> rerankerStrategies;

    public ModelProviderStrategyFactory(
            DefaultOpenAiLlmStrategy defaultLlmStrategy,
            List<LlmCallStrategy> llmStrategies,
            DefaultOpenAiEmbeddingStrategy defaultEmbeddingStrategy,
            List<EmbeddingCallStrategy> embeddingStrategies,
            DefaultOpenAiRerankerStrategy defaultRerankerStrategy,
            List<RerankerCallStrategy> rerankerStrategies) {
        this.defaultLlmStrategy = defaultLlmStrategy;
        this.llmStrategies = llmStrategies;
        this.defaultEmbeddingStrategy = defaultEmbeddingStrategy;
        this.embeddingStrategies = embeddingStrategies;
        this.defaultRerankerStrategy = defaultRerankerStrategy;
        this.rerankerStrategies = rerankerStrategies;
    }

    /** 获取 LLM 大模型生成策略。 */
    public LlmCallStrategy getLlmStrategy(ModelProvider provider) {
        if (provider != null) {
            for (LlmCallStrategy strategy : llmStrategies) {
                if (strategy != defaultLlmStrategy && strategy.supports(provider)) {
                    LOG.debug("命中特化 LLM 策略: provider={}, strategy={}", provider, strategy.getClass().getSimpleName());
                    return strategy;
                }
            }
        }
        return defaultLlmStrategy;
    }

    /** 获取 Embedding 向量模型策略。 */
    public EmbeddingCallStrategy getEmbeddingStrategy(ModelProvider provider) {
        if (provider != null) {
            for (EmbeddingCallStrategy strategy : embeddingStrategies) {
                if (strategy != defaultEmbeddingStrategy && strategy.supports(provider)) {
                    LOG.debug("命中特化 Embedding 策略: provider={}, strategy={}", provider, strategy.getClass().getSimpleName());
                    return strategy;
                }
            }
        }
        return defaultEmbeddingStrategy;
    }

    /** 获取 Reranker 重排模型策略。 */
    public RerankerCallStrategy getRerankerStrategy(ModelProvider provider) {
        if (provider != null) {
            for (RerankerCallStrategy strategy : rerankerStrategies) {
                if (strategy != defaultRerankerStrategy && strategy.supports(provider)) {
                    LOG.debug("命中特化 Reranker 策略: provider={}, strategy={}", provider, strategy.getClass().getSimpleName());
                    return strategy;
                }
            }
        }
        return defaultRerankerStrategy;
    }
}
