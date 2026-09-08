package com.acme.agentstudio.interfaces.rest.dto;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;

/**
 * CreateKnowledgeDocument 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 CreateKnowledgeDocument 操作参数。
 */
/**
 * 创建知识库文档请求对象。
 *
 * @param tenantId 租户 ID
 * @param title 知识文档标题名称
 * @param sourceType 知识文档来源类型（如 MANUAL_UPLOAD, WEBPAGE_PARSER 等）
 * @param filePath 知识文件在本地或对象存储中的存储路径
 * @param language 文档的自然语言类型（如 CHINESE, ENGLISH 等）
 */
/**
 * CreateKnowledgeDocument 业务请求数据传输对象 (DTO)。
 */
public record CreateKnowledgeDocumentRequest(
        Long tenantId,
        String title,
        String sourceType,
        String filePath,
        KnowledgeLanguage language
) {
}

