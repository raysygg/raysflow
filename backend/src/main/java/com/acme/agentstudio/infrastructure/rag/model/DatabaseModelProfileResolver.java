package com.acme.agentstudio.infrastructure.rag.model;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingModelOption;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.model.RetrievalDegradePolicy;
import com.acme.agentstudio.domain.knowledge.port.ModelProfileResolver;
import com.acme.agentstudio.domain.model.ModelCapability;
import com.acme.agentstudio.domain.workflow.model.ModelTenantScope;
import com.acme.agentstudio.infrastructure.persistence.entity.RagEmbeddingProfileEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagEmbeddingProfileMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的 RAG 模型 Profile 动态解析器实现类（Database Model Profile Resolver）。
 * 将一次模型选择解析为稳定的 RAG Profile。
 * 不同 Embedding 模型不能共享向量空间，因此每个来源和模型都对应独立 Profile。
 * Profile 创建只访问 MySQL，不加载模型、不访问 Qdrant，也不调用外部接口。
 */
@Component
public class DatabaseModelProfileResolver implements ModelProfileResolver {
    private static final String DISTANCE_COSINE = "COSINE";
    private static final String LOCAL_PROFILE_PREFIX = "local-";
    private static final String API_PROFILE_PREFIX = "api-";

    private final RagEmbeddingProfileMapper profileMapper;
    private final SysModelConfigMapper modelMapper;
    private final RagProperties properties;

    public DatabaseModelProfileResolver(RagEmbeddingProfileMapper profileMapper,
                                        SysModelConfigMapper modelMapper,
                                        RagProperties properties) {
        this.profileMapper = profileMapper;
        this.modelMapper = modelMapper;
        this.properties = properties;
    }

        /**
         * resolveActive 方法。
         *
         * @param tenantId tenantId 参数
         * @return RagEmbeddingProfile 返回对象
         */
    @Override
    public RagEmbeddingProfile resolveActive(Long tenantId) {
        return resolve(tenantId, null);
    }

        /**
         * resolve 方法。
         *
         * @param tenantId tenantId 参数
         * @param selection selection 参数
         * @return RagEmbeddingProfile 返回对象
         */
    @Override
    public RagEmbeddingProfile resolve(Long tenantId, RagModelSelection selection) {
        requireTenant(tenantId);
        RagModelSelection effective = selection == null || !selection.specified()
                ? recommendedSelection(tenantId) : selection;
        return effective.source() == RagModelSource.LOCAL
                ? resolveLocal(tenantId, effective.modelKey())
                : resolveRemote(tenantId, effective);
    }

        /**
         * 查询列表listEmbeddingModels 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         * @return List<RagEmbeddingModelOption> 返回对象
         */
    @Override
    public List<RagEmbeddingModelOption> listEmbeddingModels(Long tenantId) {
        requireTenant(tenantId);
        List<RagEmbeddingModelOption> options = new ArrayList<>();
        if (tenantId.longValue() != ModelTenantScope.PLATFORM_TENANT_ID) {
            addRemoteOptions(options, tenantId, RagModelSource.TENANT_PRIVATE, true);
        }
        addRemoteOptions(options, ModelTenantScope.PLATFORM_TENANT_ID, RagModelSource.PLATFORM_SHARED,
                options.isEmpty());
        boolean localRecommended = options.isEmpty();
        for (LocalEmbeddingModel model : LocalEmbeddingModel.values()) {
            options.add(new RagEmbeddingModelOption(RagModelSource.LOCAL, null, model.modelKey(),
                    model.modelName(), model.vectorDimension(),
                    localRecommended && model.modelKey().equals(properties.getLocal().getDefaultModel()), "平台本地"));
        }
        return List.copyOf(options);
    }

    private RagModelSelection recommendedSelection(Long tenantId) {
        return listEmbeddingModels(tenantId).stream()
                .filter(RagEmbeddingModelOption::recommended)
                .findFirst()
                .map(option -> new RagModelSelection(option.source(), option.modelId(), option.modelKey()))
                .orElseGet(() -> new RagModelSelection(RagModelSource.LOCAL, null,
                        properties.getLocal().getDefaultModel()));
    }

    private RagEmbeddingProfile resolveLocal(Long tenantId, String requestedModelKey) {
        String modelKey = requestedModelKey == null || requestedModelKey.isBlank()
                ? properties.getLocal().getDefaultModel() : requestedModelKey;
        LocalEmbeddingModel model = LocalEmbeddingModel.require(modelKey);
        String profileCode = LOCAL_PROFILE_PREFIX + model.modelKey();
        RagEmbeddingProfileEntity entity = findProfile(tenantId, profileCode);
        if (entity == null) {
            entity = baseProfile(tenantId, profileCode, model.modelName());
            entity.setEmbeddingSource(RagModelSource.LOCAL.name());
            entity.setEmbeddingModelKey(model.modelKey());
            entity.setVectorDimension(model.vectorDimension());
            entity.setRerankerModelKey(recommendedRerankerKey(tenantId));
            profileMapper.insert(entity);
        } else if (entity.getRerankerModelKey() == null || entity.getRerankerModelKey().isBlank()) {
            String activeReranker = recommendedRerankerKey(tenantId);
            if (activeReranker != null) {
                entity.setRerankerModelKey(activeReranker);
                entity.setUpdatedAt(LocalDateTime.now());
                profileMapper.updateById(entity);
            }
        }
        return toDomain(entity);
    }

    private RagEmbeddingProfile resolveRemote(Long tenantId, RagModelSelection selection) {
        SysModelConfigEntity model = requireEmbeddingModel(tenantId, selection);
        RagModelSource source = model.getTenantId().equals(ModelTenantScope.PLATFORM_TENANT_ID)
                ? RagModelSource.PLATFORM_SHARED : RagModelSource.TENANT_PRIVATE;
        String profileCode = remoteProfileCode(source, model);
        RagEmbeddingProfileEntity entity = findProfile(tenantId, profileCode);
        if (entity == null) {
            entity = baseProfile(tenantId, profileCode, model.getModelName());
            entity.setEmbeddingSource(source.name());
            entity.setEmbeddingModelId(model.getId());
            entity.setEmbeddingModelKey(model.getModelKey());
            entity.setVectorDimension(model.getVectorDimension());
            entity.setRerankerModelKey(recommendedRerankerKey(tenantId));
            profileMapper.insert(entity);
        } else if (entity.getRerankerModelKey() == null || entity.getRerankerModelKey().isBlank()) {
            String activeReranker = recommendedRerankerKey(tenantId);
            if (activeReranker != null) {
                entity.setRerankerModelKey(activeReranker);
                entity.setUpdatedAt(LocalDateTime.now());
                profileMapper.updateById(entity);
            }
        }
        return toDomain(entity);
    }

    /**
     * 远程模型编码或维度发生变化时必须创建新 Profile，确保 Qdrant 不会继续复用旧向量空间。
     * 接口地址和凭证变化不改变向量语义，因此不参与 Profile 编码。
     */
    private String remoteProfileCode(RagModelSource source, SysModelConfigEntity model) {
        String modelFingerprint = Integer.toUnsignedString(model.getModelKey().hashCode(), 16);
        return API_PROFILE_PREFIX + source.name().toLowerCase() + "-" + model.getId()
                + "-d" + model.getVectorDimension() + "-" + modelFingerprint;
    }

    private void addRemoteOptions(List<RagEmbeddingModelOption> options, Long sourceTenantId,
                                  RagModelSource source, boolean firstRecommended) {
        List<SysModelConfigEntity> models = modelMapper.selectList(
                new LambdaQueryWrapper<SysModelConfigEntity>()
                        .eq(SysModelConfigEntity::getTenantId, sourceTenantId)
                        .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)
                        .eq(SysModelConfigEntity::getModelCapability, ModelCapability.EMBEDDING.name())
                        .gt(SysModelConfigEntity::getVectorDimension, 0)
                        .orderByAsc(SysModelConfigEntity::getId));
        for (int index = 0; index < models.size(); index++) {
            SysModelConfigEntity model = models.get(index);
            options.add(new RagEmbeddingModelOption(source, model.getId(), model.getModelKey(), model.getModelName(),
                    model.getVectorDimension(), firstRecommended && index == 0,
                    source == RagModelSource.TENANT_PRIVATE ? "租户私有" : "平台共享"));
        }
    }

    private SysModelConfigEntity requireEmbeddingModel(Long tenantId, RagModelSelection selection) {
        if (selection.modelId() == null) {
            throw new IllegalArgumentException("远程 Embedding 模型标识不能为空。");
        }
        SysModelConfigEntity model = modelMapper.selectById(selection.modelId());
        boolean tenantAllowed = model != null && (tenantId.equals(model.getTenantId())
                || model.getTenantId().equals(ModelTenantScope.PLATFORM_TENANT_ID));
        if (!tenantAllowed || !BusinessStatus.ACTIVE.equals(model.getStatus())) {
            throw new IllegalArgumentException("选择的 Embedding 模型不存在、已停用或不属于当前租户。");
        }
        boolean sourceMatches = selection.source() == RagModelSource.PLATFORM_SHARED
                ? model.getTenantId().equals(ModelTenantScope.PLATFORM_TENANT_ID)
                : tenantId.equals(model.getTenantId());
        if (!sourceMatches) {
            throw new IllegalArgumentException("Embedding 模型来源与模型归属不一致，请重新选择模型。");
        }
        if (!ModelCapability.EMBEDDING.name().equals(model.getModelCapability())) {
            throw new IllegalArgumentException("选择的模型不是 Embedding 模型，请在模型中心检查能力类型。");
        }
        if (model.getVectorDimension() == null || model.getVectorDimension() <= 0) {
            throw new IllegalArgumentException("Embedding 模型未配置有效向量维度。");
        }
        return model;
    }

    private String recommendedRerankerKey(Long tenantId) {
        List<Long> tenantOrder = tenantId.equals(ModelTenantScope.PLATFORM_TENANT_ID)
                ? List.of(tenantId) : List.of(tenantId, ModelTenantScope.PLATFORM_TENANT_ID);
        for (Long sourceTenantId : tenantOrder) {
            SysModelConfigEntity model = modelMapper.selectOne(new LambdaQueryWrapper<SysModelConfigEntity>()
                    .eq(SysModelConfigEntity::getTenantId, sourceTenantId)
                    .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)
                    .eq(SysModelConfigEntity::getModelCapability, ModelCapability.RERANKER.name())
                    .orderByAsc(SysModelConfigEntity::getId)
                    .last("LIMIT 1"));
            if (model != null) return model.getModelKey();
        }
        return null;
    }

    private RagEmbeddingProfileEntity baseProfile(Long tenantId, String profileCode, String modelName) {
        RagEmbeddingProfileEntity entity = new RagEmbeddingProfileEntity();
        entity.setTenantId(tenantId);
        entity.setProfileCode(profileCode);
        entity.setProfileName(modelName + " 检索配置");
        entity.setDistanceMetric(DISTANCE_COSINE);
        entity.setCandidateLimit(properties.getDefaultCandidateLimit());
        entity.setRerankThreshold(properties.getDefaultRerankThreshold());
        entity.setNoHitThreshold(properties.getDefaultNoHitThreshold());
        entity.setScoreGapThreshold(properties.getDefaultScoreGapThreshold());
        entity.setMaxContextTokens(properties.getDefaultMaxContextTokens());
        entity.setMaxDocuments(properties.getDefaultMaxDocuments());
        entity.setMaxParentChunks(properties.getDefaultMaxParentChunks());
        entity.setPerDocumentContextLimit(properties.getDefaultPerDocumentContextLimit());
        entity.setQueryRewriteEnabled(false);
        entity.setLateInteractionEnabled(false);
        entity.setDegradePolicy(RetrievalDegradePolicy.VECTOR_ONLY.name());
        entity.setStatus(BusinessStatus.ACTIVE);
        entity.setVersionNo(1);
        entity.setSupportedLanguages("ZH,EN,OTHER");
        entity.setSupportedScripts("HAN,LATIN,OTHER");
        entity.setCrossLanguageEnabled(true);
        entity.setModelVersion("1");
        entity.setQueryInstruction("");
        entity.setDocumentInstruction("");
        entity.setRerankerEnabled(false);
        entity.setRerankerCandidateLimit(30);
        entity.setRerankerTimeoutSeconds(properties.getModelTimeoutSeconds());
        entity.setRerankerBudget(0D);
        entity.setVectorRecallWeight(properties.getVectorRecallWeight());
        entity.setLexicalRecallWeight(properties.getLexicalRecallWeight());
        entity.setExactRecallBoost(properties.getExactRecallBoost());
        entity.setActive(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private RagEmbeddingProfileEntity findProfile(Long tenantId, String profileCode) {
        return profileMapper.selectOne(new LambdaQueryWrapper<RagEmbeddingProfileEntity>()
                .eq(RagEmbeddingProfileEntity::getTenantId, tenantId)
                .eq(RagEmbeddingProfileEntity::getProfileCode, profileCode)
                .eq(RagEmbeddingProfileEntity::getStatus, BusinessStatus.ACTIVE)
                .last("LIMIT 1"));
    }

    private RagEmbeddingProfile toDomain(RagEmbeddingProfileEntity entity) {
        RetrievalDegradePolicy degradePolicy;
        try {
            degradePolicy = RetrievalDegradePolicy.valueOf(entity.getDegradePolicy());
        } catch (RuntimeException ignored) {
            degradePolicy = RetrievalDegradePolicy.VECTOR_ONLY;
        }
        RagModelSource source;
        try {
            source = RagModelSource.valueOf(entity.getEmbeddingSource());
        } catch (RuntimeException ignored) {
            source = RagModelSource.LOCAL;
        }
        String rerankerModelKey = entity.getRerankerModelKey();
        if (rerankerModelKey == null || rerankerModelKey.isBlank()) {
            rerankerModelKey = recommendedRerankerKey(entity.getTenantId());
        }
        return new RagEmbeddingProfile(entity.getId(), entity.getTenantId(), entity.getProfileCode(),
                entity.getProfileName(), source, entity.getEmbeddingModelId(), entity.getEmbeddingModelKey(),
                rerankerModelKey, entity.getQueryRewriteModelKey(), entity.getVectorDimension(),
                entity.getDistanceMetric(), entity.getCandidateLimit(), entity.getRerankThreshold(),
                entity.getNoHitThreshold(), entity.getScoreGapThreshold(), entity.getMaxContextTokens(),
                entity.getMaxDocuments(), entity.getMaxParentChunks(), entity.getPerDocumentContextLimit(),
                Boolean.TRUE.equals(entity.getQueryRewriteEnabled()),
                Boolean.TRUE.equals(entity.getLateInteractionEnabled()), degradePolicy, value(entity.getVersionNo(), 1),
                entity.getSupportedLanguages(), entity.getSupportedScripts(),
                Boolean.TRUE.equals(entity.getCrossLanguageEnabled()),
                entity.getModelVersion(), entity.getQueryInstruction(), entity.getDocumentInstruction(),
                !Boolean.FALSE.equals(entity.getRerankerEnabled()), value(entity.getRerankerCandidateLimit(), 30),
                value(entity.getRerankerTimeoutSeconds(), properties.getModelTimeoutSeconds()),
                value(entity.getRerankerBudget(), 0D), value(entity.getVectorRecallWeight(), properties.getVectorRecallWeight()),
                value(entity.getLexicalRecallWeight(), properties.getLexicalRecallWeight()),
                value(entity.getExactRecallBoost(), properties.getExactRecallBoost()));
    }

    private int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private double value(Double value, double fallback) { return value == null ? fallback : value; }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("RAG 模型配置的租户不能为空。");
    }
}
