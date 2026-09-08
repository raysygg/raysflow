package com.acme.agentstudio.application.model;

import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.domain.model.ModelCapability;
import com.acme.agentstudio.domain.model.ModelInvocationErrorCategory;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.model.strategy.ModelInvocationErrorClassifier;
import com.acme.agentstudio.infrastructure.model.strategy.ModelProviderStrategyFactory;
import com.acme.agentstudio.infrastructure.model.strategy.embedding.EmbeddingCallStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.llm.LlmCallStrategy;
import com.acme.agentstudio.infrastructure.model.strategy.rerank.RerankerCallStrategy;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 平台 AI 大模型连通性健康探测服务 (Model Connection Healthcheck Service)。
 * 提供零入侵的探针测试能力，在管理员或用户配置 API 凭证、Base URL 或 Key 关联时，发起轻量级 Ping 探针数据包，回传准确的响应耗时（latencyMs）、通道连通性以及中文故障诊断提示。
 */
@Service
public class ModelTestConnectionService {

    private static final Logger LOG = LoggerFactory.getLogger(ModelTestConnectionService.class);

    /** 供应商策略创建工厂 */
    private final ModelProviderStrategyFactory strategyFactory;

    /** 探针异常分类归因器 */
    private final ModelInvocationErrorClassifier errorClassifier;

    /**
     * 构造函数注入策略工厂与错误分类器。
     */
    public ModelTestConnectionService(ModelProviderStrategyFactory strategyFactory,
                                      ModelInvocationErrorClassifier errorClassifier) {
        this.strategyFactory = strategyFactory;
        this.errorClassifier = errorClassifier;
    }

    /**
     * 执行基础模型通道连通性健康探测。
     *
     * @param provider 供应商枚举
     * @param capability 模型能力 (CHAT / EMBEDDING / RERANKER)
     * @param baseUrl 接口 Base URL
     * @param apiKey API 密钥凭证
     * @param modelKey 模型标识名称
     * @return 结构化的探针诊断测试结果
     */
    public TestResult testConnection(ModelProvider provider, ModelCapability capability, String baseUrl, String apiKey, String modelKey) {
        return testConnection(provider, capability, baseUrl, apiKey, modelKey, null);
    }

    /**
     * 执行带期望维度（Expected Dimension）的精细化模型连通性健康探测。
     *
     * @param provider 供应商枚举
     * @param capability 模型能力
     * @param baseUrl 接口 Base URL
     * @param apiKey API 密钥凭证
     * @param modelKey 模型标识名称
     * @param expectedDimension 期望的向量维度
     * @return 结构化的探针诊断测试结果
     */
    public TestResult testConnection(ModelProvider provider, ModelCapability capability, String baseUrl,
                                     String apiKey, String modelKey, Integer expectedDimension) {
        if (provider == null || capability == null || baseUrl == null || apiKey == null || modelKey == null) {
            return TestResult.fail(ModelInvocationErrorCategory.INVALID_CONFIGURATION, "探测请求参数不完整，存在未填充的空字段。");
        }

        if (!provider.supports(capability)) {
            return TestResult.fail(ModelInvocationErrorCategory.CAPABILITY_MISMATCH,
                    String.format("供应商“%s”不支持“%s”能力，无需探测该模型通道。", provider.displayName(), capability.name()));
        }

        RagModelConnectionResolver.ModelConnection connection =
                new RagModelConnectionResolver.ModelConnection(modelKey.trim(), baseUrl.trim(), apiKey.trim());

        long start = System.currentTimeMillis();
        try {
            if (capability == ModelCapability.CHAT) {
                LlmCallStrategy strategy = strategyFactory.getLlmStrategy(provider);
                ChatLanguageModel chatModel = strategy.createChatModel(connection);
                String responseText = chatModel.generate("Hi");
                long elapsed = System.currentTimeMillis() - start;
                return TestResult.success("LLM 对话通道响应正常", elapsed, responseText);
            } else if (capability == ModelCapability.EMBEDDING) {
                EmbeddingCallStrategy strategy = strategyFactory.getEmbeddingStrategy(provider);
                EmbeddingModel embeddingModel = strategy.createEmbeddingModel(connection);
                int dimension = embeddingModel.embedAll(List.of(TextSegment.from("Hi"))).content().get(0).vector().length;
                long elapsed = System.currentTimeMillis() - start;
                if (expectedDimension != null && expectedDimension > 0 && dimension != expectedDimension) {
                    return new TestResult(
                            false,
                            "向量维度与配置不一致，探测实际维度为: " + dimension,
                            elapsed,
                            null,
                            ModelInvocationErrorCategory.DIMENSION_MISMATCH,
                            dimension,
                            ModelProbeCapabilityStatus.MISMATCH
                    );
                }
                return TestResult.success("Embedding 向量通道响应正常", elapsed, "Embedding Vector Verified", dimension);
            } else if (capability == ModelCapability.RERANKER) {
                RerankerCallStrategy strategy = strategyFactory.getRerankerStrategy(provider);
                List<RerankResult> results = strategy.rerank(connection, "Hi", List.of(
                        new RerankCandidate(1L, "first candidate", 0.5D),
                        new RerankCandidate(2L, "second candidate", 0.4D)));
                boolean validRanking = results != null && results.size() >= 2
                        && results.stream().map(RerankResult::chunkId).filter(Objects::nonNull)
                        .distinct().count() >= 2
                        && results.stream().allMatch(result -> Double.isFinite(result.score()));
                if (!validRanking) {
                    return TestResult.fail(ModelInvocationErrorCategory.INVALID_RESPONSE, "Rerank 重排通道未返回有效的候选评分项。");
                }
                long elapsed = System.currentTimeMillis() - start;
                return TestResult.success("Rerank 重排通道响应正常", elapsed, "Rerank Ready");
            }
            return TestResult.fail(ModelInvocationErrorCategory.INVALID_CONFIGURATION, "未知的模型能力类别");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            LOG.warn("模型连通性探针测试异常，provider={}, modelKey={}, error={}", provider, modelKey, e.getMessage());
            String rawError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (rawError.contains("Exception: ")) {
                rawError = rawError.substring(rawError.indexOf("Exception: ") + 11);
            }
            return TestResult.fail(errorClassifier.classify(e), "接口响应异常: " + rawError, elapsed);
        }
    }

    /**
     * 连通性测试诊断结果 Record 契约。
     */
    public record TestResult(
            boolean success,
            String message,
            long latencyMs,
            String sampleOutput,
            ModelInvocationErrorCategory errorCategory,
            Integer observedDimension,
            ModelProbeCapabilityStatus capabilityStatus
    ) {
        /** 快捷静态方法：构造成功探测结果 */
        public static TestResult success(String message, long latencyMs, String sampleOutput) {
            return success(message, latencyMs, sampleOutput, null);
        }

        /** 快捷静态方法：构造带维度的成功向量探测结果 */
        public static TestResult success(String message, long latencyMs, String sampleOutput, Integer dimension) {
            return new TestResult(true, message, latencyMs, sampleOutput, ModelInvocationErrorCategory.NONE, dimension,
                    ModelProbeCapabilityStatus.AVAILABLE);
        }

        /** 快捷静态方法：构造无耗时记录的失败结果 */
        public static TestResult fail(ModelInvocationErrorCategory category, String message) {
            return new TestResult(false, message, 0, null, category, null, failureStatus(category));
        }

        /** 快捷静态方法：构造带耗时记录的失败结果 */
        public static TestResult fail(ModelInvocationErrorCategory category, String message, long latencyMs) {
            return new TestResult(false, message, latencyMs, null, category, null, failureStatus(category));
        }

        /** 根据错误分类判断模型能力状态 */
        private static ModelProbeCapabilityStatus failureStatus(ModelInvocationErrorCategory category) {
            return category == ModelInvocationErrorCategory.CAPABILITY_MISMATCH
                    || category == ModelInvocationErrorCategory.DIMENSION_MISMATCH
                    ? ModelProbeCapabilityStatus.MISMATCH : ModelProbeCapabilityStatus.UNAVAILABLE;
        }
    }
}

