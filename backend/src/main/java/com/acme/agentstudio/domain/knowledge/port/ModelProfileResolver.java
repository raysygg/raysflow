package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingModelOption;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import java.util.List;

/**
 * 租户与应用工作区 RAG 模型配置解析端口接口（Model Profile Resolver）。
 * 负责根据租户、模型选型解析出在线生效的向量与重排配置 Profile。
 */
public interface ModelProfileResolver {

    /**
     * 解析租户当前全量活跃生效的 RagEmbeddingProfile。
     *
     * @param tenantId 租户物理 ID
     * @param tenantId 归属租户
     * @return 激活的 RagEmbeddingProfile 配置对象
     */
    RagEmbeddingProfile resolveActive(Long tenantId);

    /**
     * 根据显式指定的模型选择策略解析 RagEmbeddingProfile。
     *
     * @param tenantId 租户物理 ID
     * @param selection 显式指定的模型来源 selection
     * @return 匹配的 RagEmbeddingProfile 配置对象
     */
    RagEmbeddingProfile resolve(Long tenantId, RagModelSelection selection);

    /**
     * 查询租户当前可用的全量 Embedding 模型配置选项列表。
     *
     * @param tenantId 租户物理 ID
     * @return 包含可用模型的 RagEmbeddingModelOption 列表
     */
    List<RagEmbeddingModelOption> listEmbeddingModels(Long tenantId);
}

