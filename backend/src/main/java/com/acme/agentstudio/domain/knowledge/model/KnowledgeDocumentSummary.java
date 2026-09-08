package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库文档摘要实体 Record（Knowledge Document Summary）。
 * 包含文档唯一 ID id、文档标题 title、来源类型 sourceType (FILE, URL, API)、
 * 当前处理状态 status、已拆分的 Chunk 数量 chunkCount、主要语种 language (KnowledgeLanguage) 及语种确认状态 languageConfirmed。
 *
 * @param id 文档 ID
 * @param title 文档标题
 * @param sourceType 知识来源类型
 * @param status 状态
 * @param chunkCount 切块总数量
 * @param language 识别出的主语种（KnowledgeLanguage）
 * @param languageConfirmed 语种类型是否经过用户人工确认
 */
public record KnowledgeDocumentSummary(
        Long id,
        String title,
        String sourceType,
        String status,
        Integer chunkCount,
        KnowledgeLanguage language,
        boolean languageConfirmed
) {
}

