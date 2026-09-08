package com.acme.agentstudio.domain.knowledge.model;

/**
 * 工作流与应用下拉选择器使用的轻量级知识文档选项实体 Record（Knowledge Document Option）。
 * 包含文档唯一 ID id、文档标题 title、语种 language (KnowledgeLanguage)、语种是否已确认 languageConfirmed 及文档解析状态 status。
 *
 * @param id 文档 ID
 * @param title 文档标题
 * @param language 识别/设定的主要语种（KnowledgeLanguage）
 * @param languageConfirmed 语种类型是否经过用户人工二次确认
 * @param status 文档当前解析/向量化状态
 */
public record KnowledgeDocumentOption(
        Long id,
        String title,
        KnowledgeLanguage language,
        boolean languageConfirmed,
        String status
) {
}

