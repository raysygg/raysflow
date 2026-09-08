package com.acme.agentstudio.interfaces.rest.dto;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;

/**
 * KnowledgeSearch 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 KnowledgeSearch 操作参数。
 */
/**
 * 知识库检索调试请求对象。
 *
 * @param query 检索查询问题或关键字
 * @param queryLanguage 查询语言类型（如 CHINESE, ENGLISH 等）
 * @param modelSelection 指定使用的向量模型策略（可选）
 */
/**
 * KnowledgeSearch 业务请求数据传输对象 (DTO)。
 */
public record KnowledgeSearchRequest(
        String query,
        KnowledgeLanguage queryLanguage,
        RagModelSelection modelSelection
) {
}

