package com.acme.agentstudio.infrastructure.rag.model;

import com.acme.agentstudio.application.model.ModelCredentialService;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.workflow.model.ModelTenantScope;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 模型基础设施 Endpoint 与 Key 凭证连接解析组件（Rag Model Connection Resolver）。
 * 模型凭证只在模型基础设施内部解析，不进入 RAG 领域对象。
 */
@Component
public class RagModelConnectionResolver {
    private final SysModelConfigMapper modelMapper;
    private final ModelCredentialService credentialService;

    public RagModelConnectionResolver(SysModelConfigMapper modelMapper, ModelCredentialService credentialService) {
        this.modelMapper = modelMapper;
        this.credentialService = credentialService;
    }

    /**
     * 按租户和模型编码解析连接配置。
     *
     * @param tenantId 租户 ID
     * @param modelKey 模型编码
     * @return 模型连接对象
     */
    public ModelConnection resolve(Long tenantId, String modelKey) {
        if (tenantId == null || modelKey == null || modelKey.isBlank()) {
            throw new IllegalStateException("RAG 模型编码和租户不能为空。");
        }
        SysModelConfigEntity model = find(tenantId, modelKey);
        if (model == null) model = find(ModelTenantScope.PLATFORM_TENANT_ID, modelKey);
        if (model == null) throw new IllegalStateException("RAG 模型不存在或已停用：" + modelKey);
        return connection(model);
    }

    /**
     * 按冻结的模型来源和数据库标识加载连接，避免运行时再次根据名称猜测来源。
     *
     * @param tenantId 租户 ID
     * @param source 模型来源
     * @param modelId 模型持久化 ID
     * @param modelKey 模型编码
     * @return 模型连接对象
     */
    public ModelConnection resolve(Long tenantId, RagModelSource source, Long modelId, String modelKey) {
        if (source == null || source == RagModelSource.LOCAL) {
            throw new IllegalArgumentException("本地 Embedding 模型不需要远程连接。");
        }
        SysModelConfigEntity model = modelId == null ? null : modelMapper.selectById(modelId);
        Long expectedTenantId = source == RagModelSource.PLATFORM_SHARED
                ? ModelTenantScope.PLATFORM_TENANT_ID : tenantId;
        if (model == null || !expectedTenantId.equals(model.getTenantId())
                || !BusinessStatus.ACTIVE.equals(model.getStatus())
                || !model.getModelKey().equals(modelKey)) {
            throw new IllegalStateException("RAG Embedding 模型不存在、已停用或来源不匹配：" + modelKey);
        }
        return connection(model);
    }

    /**
     * 构建模型连接契约，向供应商透传上游真实模型标识。
     *
     * @param model 模型实体
     * @return 模型连接契约
     */
    private ModelConnection connection(SysModelConfigEntity model) {
        if (model.getBaseUrl() == null || model.getBaseUrl().isBlank()) {
            throw new IllegalStateException("RAG 模型未配置接口地址：" + model.getModelKey());
        }
        if (model.getCredentialRefId() == null) {
            throw new IllegalStateException("RAG 模型未配置接口凭证：" + model.getModelKey());
        }
        return new ModelConnection(model.getId(), model.getTenantId(), model.resolveUpstreamModelName(), model.getBaseUrl(),
                credentialService.resolveSecret(model.getTenantId(), model.getCredentialRefId()));
    }

    /**
     * 查询租户下可用的 Reranker 模型编码。
     *
     * @param tenantId 租户 ID
     * @return Reranker 模型编码
     */
    public String findActiveRerankerKey(Long tenantId) {
        if (tenantId == null) return null;
        List<Long> tenantOrder = tenantId.equals(ModelTenantScope.PLATFORM_TENANT_ID)
                ? List.of(tenantId) : List.of(tenantId, ModelTenantScope.PLATFORM_TENANT_ID);
        for (Long sourceTenantId : tenantOrder) {
            SysModelConfigEntity model = modelMapper.selectOne(new LambdaQueryWrapper<SysModelConfigEntity>()
                    .eq(SysModelConfigEntity::getTenantId, sourceTenantId)
                    .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)
                    .eq(SysModelConfigEntity::getModelCapability, com.acme.agentstudio.domain.model.ModelCapability.RERANKER.name())
                    .orderByAsc(SysModelConfigEntity::getId)
                    .last("LIMIT 1"));
            if (model != null && model.getBaseUrl() != null && !model.getBaseUrl().isBlank()
                    && model.getCredentialRefId() != null) {
                return model.getModelKey();
            }
        }
        return null;
    }

    private SysModelConfigEntity find(Long tenantId, String modelKey) {
        return modelMapper.selectOne(new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getTenantId, tenantId)
                .eq(SysModelConfigEntity::getModelKey, modelKey)
                .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE));
    }

    public record ModelConnection(Long modelId, Long sourceTenantId, String modelKey, String baseUrl, String apiKey) {
        public ModelConnection(String modelKey, String baseUrl, String apiKey) {
            this(null, null, modelKey, baseUrl, apiKey);
        }
    }
}
