package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库索引与 RAG 检索时选择配置的向量 Embedding 模型选项 Record（RAG Model Selection）。
 * 包含模型来源 source (RagModelSource: TENANT_PLATFORM / CUSTOM_MODEL)、
 * 模型平台/数据库实体 ID modelId 及模型唯一标识 Key modelKey。
 *
 * @param source 模型来源（RagModelSource）
 * @param modelId 平台模型 ID
 * @param modelKey 模型 key
 */
public record RagModelSelection(
        RagModelSource source,
        Long modelId,
        String modelKey
) {
    /**
     * 是否显式指定了 Embedding 模型来源。
     *
     * @return true 表示已指定
     */
    public boolean specified() {
        return source != null;
    }
}

