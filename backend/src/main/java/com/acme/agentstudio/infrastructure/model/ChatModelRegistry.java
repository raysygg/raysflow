package com.acme.agentstudio.infrastructure.model;

import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.application.model.ModelCredentialService;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.domain.workflow.model.ModelTenantScope;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import com.acme.agentstudio.infrastructure.model.strategy.ModelProviderStrategyFactory;
import com.acme.agentstudio.infrastructure.model.strategy.llm.LlmCallStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态大模型注册中心与缓存加载管理类（Chat Model Registry）。
 * 负责从租户独立配置及系统全局共享配置中提取 API Key/Base URL 等连接凭证，
 * 基于 ConcurrentHashMap 动态实例化并缓存 LangChain4j ChatLanguageModel 与 StreamingChatLanguageModel 通道实例。
 */
@Component
public class ChatModelRegistry {
    private static final Logger log = LoggerFactory.getLogger(ChatModelRegistry.class);

    private final SysModelConfigMapper sysModelConfigMapper;
    private final ModelCredentialService credentialService;
    private final ModelProviderStrategyFactory strategyFactory;

    // 基于缓存结构提高加载性能，防止高频重复构建连接通道
    private final ConcurrentHashMap<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    /**
     * 实时运行单独缓存流式连接，避免把普通调用错误地转成伪流式输出。
     */
    private final ConcurrentHashMap<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    public ChatModelRegistry(SysModelConfigMapper sysModelConfigMapper, ModelCredentialService credentialService, ModelProviderStrategyFactory strategyFactory) {
        this.sysModelConfigMapper = sysModelConfigMapper;
        this.credentialService = credentialService;
        this.strategyFactory = strategyFactory;
    }

    /**
     * 加载或从缓存中直接读取大模型连接实例
     */
    public ChatLanguageModel getModel(Long tenantId, String modelKey) {
        // 配置修改后由 clearCache 清理旧连接，正常请求优先复用缓存实例。
        if (modelKey == null || modelKey.isBlank()) {
            throw new IllegalStateException("未指定模型编码，无法创建模型连接。");
        }

        String cacheKey = tenantId + ":" + modelKey;
        ChatLanguageModel cached = modelCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 1. 首要获取当前租户独立的私有模型连接参数
        SysModelConfigEntity config = resolveConfig(tenantId, modelKey);
        ChatLanguageModel model = createModel(config);
        modelCache.put(cacheKey, model);
        return model;
    }

    /**
     * 生产工作流按发布快照中的确定来源加载模型，禁止再次执行私有/共享回退。
     */
    public ChatLanguageModel getModel(WorkflowDependencySnapshot.ModelDependencyReference reference) {
        SysModelConfigEntity config = requireSnapshotConfig(reference);
        String cacheKey = snapshotCacheKey(reference);
        return modelCache.computeIfAbsent(cacheKey, ignored -> createModel(config));
    }

    /**
     * 获取真实流式模型。流式模型和普通模型使用不同缓存，调用方必须明确选择实时交付。
     */
    public StreamingChatLanguageModel getStreamingModel(Long tenantId, String modelKey) {
        validateModelKey(modelKey);
        String cacheKey = cacheKey(tenantId, modelKey);
        StreamingChatLanguageModel cached = streamingModelCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        SysModelConfigEntity config = resolveConfig(tenantId, modelKey);
        StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
                .apiKey(resolveCredential(config))
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelKey())
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
        streamingModelCache.put(cacheKey, model);
        return model;
    }

    /**
     * 流式生产调用与普通调用使用相同的冻结来源。
     */
    public StreamingChatLanguageModel getStreamingModel(WorkflowDependencySnapshot.ModelDependencyReference reference) {
        SysModelConfigEntity config = requireSnapshotConfig(reference);
        String cacheKey = snapshotCacheKey(reference);
        return streamingModelCache.computeIfAbsent(cacheKey, ignored -> createStreamingModel(config));
    }

    /**
     * 清理指定租户的模型连接缓存，方便修改配置参数后实时重新加载生效
     */
    public void clearCache(Long tenantId, String modelKey) {
        String suffix = ":" + modelKey;
        modelCache.keySet().removeIf(key -> key.endsWith(suffix));
        streamingModelCache.keySet().removeIf(key -> key.endsWith(suffix));
    }

    private SysModelConfigEntity resolveConfig(Long tenantId, String modelKey) {
        SysModelConfigEntity config = activeConfig(tenantId, modelKey);
        // 私有模型不存在时回退平台共享模型，凭证仍只由模型中心管理。
        if (config == null) {
            config = activeConfig(ModelTenantScope.PLATFORM_TENANT_ID, modelKey);
        }
        if (config == null || config.getCredentialRefId() == null) {
            throw new IllegalStateException("模型“" + modelKey + "”未配置有效接口凭证，请先在模型中心完成配置。");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalStateException("模型“" + modelKey + "”未配置接口地址，请先在模型中心完成配置。");
        }
        return config;
    }

    private SysModelConfigEntity activeConfig(Long tenantId, String modelKey) {
        return sysModelConfigMapper.selectOne(new QueryWrapper<SysModelConfigEntity>()
                .eq("tenant_id", tenantId)
                .eq("model_key", modelKey)
                .eq("status", "ACTIVE"));
    }

    private void validateModelKey(String modelKey) {
        if (modelKey == null || modelKey.isBlank()) {
            throw new IllegalStateException("未指定模型编码，无法创建模型连接。");
        }
    }

    private String cacheKey(Long tenantId, String modelKey) {
        return tenantId + ":" + modelKey;
    }

    private SysModelConfigEntity requireSnapshotConfig(WorkflowDependencySnapshot.ModelDependencyReference reference) {
        if (reference == null || reference.modelId() == null || reference.sourceTenantId() == null) {
            throw new IllegalStateException("发布快照缺少模型来源，禁止执行模型节点。");
        }
        WorkflowDependencySnapshot.DependencySource expectedSource =
                reference.sourceTenantId() == ModelTenantScope.PLATFORM_TENANT_ID
                        ? WorkflowDependencySnapshot.DependencySource.PLATFORM_SHARED
                        : WorkflowDependencySnapshot.DependencySource.TENANT_PRIVATE;
        if (reference.source() != expectedSource) {
            throw new IllegalStateException("发布快照中的模型来源与来源租户不一致，禁止加载模型凭证。");
        }
        SysModelConfigEntity config = sysModelConfigMapper.selectOne(new QueryWrapper<SysModelConfigEntity>()
                .eq("id", reference.modelId())
                .eq("tenant_id", reference.sourceTenantId())
                .eq("model_key", reference.modelKey())
                .eq("status", "ACTIVE"));
        if (config == null) {
            throw new IllegalStateException("发布快照引用的模型已停用或不存在：" + reference.name());
        }
        validateCredentials(config, reference.modelKey());
        return config;
    }

    private void validateCredentials(SysModelConfigEntity config, String modelKey) {
        if (config.getCredentialRefId() == null) {
            throw new IllegalStateException("模型“" + modelKey + "”未配置有效接口凭证，请先在模型中心完成配置。");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalStateException("模型“" + modelKey + "”未配置有效接口凭证，请先在模型中心完成配置。");
        }
    }

    private String snapshotCacheKey(WorkflowDependencySnapshot.ModelDependencyReference reference) {
        return reference.sourceTenantId() + ":" + reference.modelId() + ":" + reference.modelKey();
    }

    private ChatLanguageModel createModel(SysModelConfigEntity config) {
        log.info("按供应商策略与 LangChain4j 实例化模型，tenantId={}, modelId={}, modelKey={}, upstreamModel={}, provider={}",
                config.getTenantId(), config.getId(), config.getModelKey(), config.resolveUpstreamModelName(), config.getProvider());
        ModelProvider provider = ModelProvider.fromCode(config.getProvider());
        String decryptedKey = resolveCredential(config);
        RagModelConnectionResolver.ModelConnection connection =
                new RagModelConnectionResolver.ModelConnection(
                        config.getId(), config.getTenantId(), config.resolveUpstreamModelName(), config.getBaseUrl(), decryptedKey);

        return strategyFactory.getLlmStrategy(provider).createChatModel(connection);
    }

    private StreamingChatLanguageModel createStreamingModel(SysModelConfigEntity config) {
        log.info("按发布快照加载流式模型，tenantId={}, modelId={}, modelKey={}, upstreamModel={}",
                config.getTenantId(), config.getId(), config.getModelKey(), config.resolveUpstreamModelName());
        return OpenAiStreamingChatModel.builder()
                .apiKey(resolveCredential(config))
                .baseUrl(config.getBaseUrl())
                .modelName(config.resolveUpstreamModelName())
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /**
     * 按模型来源租户解析受保护凭证，明文仅停留在模型客户端构建过程。
     */
    private String resolveCredential(SysModelConfigEntity config) {
        return credentialService.resolveSecret(config.getTenantId(), config.getCredentialRefId());
    }
}
