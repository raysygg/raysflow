package com.acme.agentstudio.domain.knowledge.model;

/**
 * 前端与工作区可下拉选择的 Embedding 模型选项实体 Record（Rag Embedding Model Option）。
 * 包含模型来源 source (RagModelSource: TENANT / PLATFORM)、模型物理 ID modelId、模型编码 modelKey、
 * 模型名称 modelName、向量输出维度 vectorDimension、推荐标志 recommended 及来源描述标签 sourceLabel。
 *
 * @param source 模型来源（租户自定义 / 平台公共预置）
 * @param modelId 模型配置 ID
 * @param modelKey 模型检索 Key
 * @param modelName 展示名称
 * @param vectorDimension 输出向量维度（如 1536, 1024）
 * @param recommended 是否为平台官方推荐模型
 * @param sourceLabel 来源提示标签（如“租户专属”、“平台公共”）
 */
public record RagEmbeddingModelOption(
        RagModelSource source,
        Long modelId,
        String modelKey,
        String modelName,
        int vectorDimension,
        boolean recommended,
        String sourceLabel
) {
}

