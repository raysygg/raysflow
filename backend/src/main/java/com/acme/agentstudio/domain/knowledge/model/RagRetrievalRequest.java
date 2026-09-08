package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * RagRetrieval 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 RagRetrieval 操作参数。
 */
/** RAG 检索的唯一输入合同，避免继续增加带有隐式语义的方法重载。 */
public record RagRetrievalRequest(
        Long tenantId,
        Long userId,
        String query,
        RetrievalLanguageStrategy languageStrategy,
        KnowledgeLanguage queryLanguage,
        RetrievalScopeType scope,
        List<Long> documentIds,
        int topK,
        RagModelSelection modelSelection
) {
    public RagRetrievalRequest {
        languageStrategy = languageStrategy == null ? RetrievalLanguageStrategy.AUTO : languageStrategy;
        scope = scope == null ? RetrievalScopeType.VISIBLE_DOCUMENTS : scope;
        documentIds = documentIds == null ? List.of() : List.copyOf(documentIds);
        topK = Math.max(1, topK);
    }
}
