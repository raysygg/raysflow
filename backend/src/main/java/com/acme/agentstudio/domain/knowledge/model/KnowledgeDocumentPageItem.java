package com.acme.agentstudio.domain.knowledge.model;

import java.time.LocalDateTime;

/**
 * 知识库文档分页列表展示单行明细实体 Record（Knowledge Document Page Item）。
 * 包含文档唯一 ID id、文档标题 title、来源类型 sourceType (FILE, URL, API)、解析状态 status、
 * 成功导出的 Chunk 切块总数 chunkCount、识别语种 language (KnowledgeLanguage)、语种确认状态 languageConfirmed、
 * 内部版本号 versionNo、向量索引状态 indexStatus、模型 Profile ID profileId、
 * 绑定索引版本 Generation ID indexGenerationId、构建时间 indexedAt 及最后更新时间 updatedAt。
 *
 * @param id 文档 ID
 * @param title 文档标题
 * @param sourceType 知识源类型（FILE, URL, MANUAL）
 * @param status 文档解析状态
 * @param chunkCount 切块片段总数量
 * @param language 文档语种枚举（KnowledgeLanguage）
 * @param languageConfirmed 语种类型是否已人工确认
 * @param versionNo 文档版本号
 * @param indexStatus 向量索引生成状态
 * @param profileId 使用的模型 Profile ID
 * @param indexGenerationId 关联的向量 Generation 版本 ID
 * @param indexedAt 向量构建成功时刻
 * @param updatedAt 最近一次更新修改时间
 */
public record KnowledgeDocumentPageItem(
        Long id,
        String title,
        String sourceType,
        String status,
        Integer chunkCount,
        KnowledgeLanguage language,
        boolean languageConfirmed,
        Integer versionNo,
        String indexStatus,
        Long profileId,
        Long indexGenerationId,
        LocalDateTime indexedAt,
        LocalDateTime updatedAt
) {
}

