package com.acme.agentstudio.infrastructure.rag.model;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.port.EmbeddingProvider;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhq.BgeSmallZhQuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.model.strategy.ModelProviderStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 兼容 OpenAI 协议的向量模型与本地 Embedding 混合路由提供方（OpenAI Compatible Embedding Provider）。
 * 本地模型直接在 JVM 内调用；租户私有和平台共享模型通过模型中心保存的 OpenAI 兼容连接调用。
 * 本地实例按模型编码惰性缓存，避免每次请求重复加载 ONNX 权重，也避免应用启动时占用不必要内存。
 */
@Component
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingProvider.class);

    private final RagModelConnectionResolver connectionResolver;
    private final RagProperties properties;
    private final ModelProviderStrategyFactory strategyFactory;
    private final ConcurrentMap<String, EmbeddingModel> localModels = new ConcurrentHashMap<>();
    private final ConcurrentMap<RemoteModelCacheKey, EmbeddingModel> remoteModels = new ConcurrentHashMap<>();

    public OpenAiCompatibleEmbeddingProvider(RagModelConnectionResolver connectionResolver, RagProperties properties, ModelProviderStrategyFactory strategyFactory) {
        this.connectionResolver = connectionResolver;
        this.properties = properties;
        this.strategyFactory = strategyFactory;
    }

        /**
         * embedDocuments 方法。
         *
         * @param profile profile 参数
         * @param texts texts 参数
         * @return List<List<Float>> 返回对象
         */
    @Override
    public List<List<Float>> embedDocuments(RagEmbeddingProfile profile, List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        List<TextSegment> segments = texts.stream().map(TextSegment::from).toList();
        return model(profile).embedAll(segments).content().stream().map(this::toList).toList();
    }

        /**
         * embedQuery 方法。
         *
         * @param profile profile 参数
         * @param query query 参数
         * @return List<Float> 返回对象
         */
    @Override
    public List<Float> embedQuery(RagEmbeddingProfile profile, String query) {
        return toList(model(profile).embed(query).content());
    }

    private EmbeddingModel model(RagEmbeddingProfile profile) {
        if (profile.embeddingSource() == RagModelSource.LOCAL) {
            return localModels.computeIfAbsent(profile.embeddingModelKey(), this::createLocalModel);
        }
        RemoteModelCacheKey cacheKey = new RemoteModelCacheKey(profile.embeddingSource(), profile.tenantId(),
                profile.embeddingModelId(), profile.embeddingModelKey());
        return remoteModels.computeIfAbsent(cacheKey, ignored -> createRemoteModel(profile));
    }

    private EmbeddingModel createRemoteModel(RagEmbeddingProfile profile) {
        RagModelConnectionResolver.ModelConnection connection = connectionResolver.resolve(
                profile.tenantId(), profile.embeddingSource(), profile.embeddingModelId(), profile.embeddingModelKey());
        ModelProvider provider = ModelProvider.fromCode(connection.modelKey());
        return strategyFactory.getEmbeddingStrategy(provider).createEmbeddingModel(connection);
    }

    /** 模型中心更新或删除连接后立即清理对应客户端，下一次调用会读取最新地址和凭证。 */
    public void clearRemoteModel(Long modelId) {
        if (modelId == null) return;
        remoteModels.keySet().removeIf(key -> modelId.equals(key.modelId()));
    }

    private EmbeddingModel createLocalModel(String modelKey) {
        LocalEmbeddingModel model = LocalEmbeddingModel.require(modelKey);
        LOG.info("加载平台本地 Embedding 模型，modelKey={}, dimension={}", model.modelKey(), model.vectorDimension());
        return switch (model) {
            case MULTILINGUAL_MINILM -> new AllMiniLmL6V2QuantizedEmbeddingModel();
            case BGE_SMALL_ZH -> new BgeSmallZhQuantizedEmbeddingModel();
        };
    }

    private List<Float> toList(Embedding embedding) {
        float[] vector = embedding.vector();
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) values.add(value);
        return List.copyOf(values);
    }

    private record RemoteModelCacheKey(RagModelSource source, Long tenantId, Long modelId, String modelKey) {
    }
}
