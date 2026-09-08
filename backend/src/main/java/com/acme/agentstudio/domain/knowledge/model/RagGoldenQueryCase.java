package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 离线自动化评测用的 Golden Query 测试集标准测试用例 Record（RAG Golden Query Case）。
 * 包含用例 ID caseId、语种分类 language (KnowledgeLanguage)、标准查询文本 query 及期望命中匹配的标签列表 expectedTags (List&lt;String&gt;)。
 *
 * @param caseId 用例 ID
 * @param language 用例语种
 * @param query 评估发起的检索 Query
 * @param expectedTags 期望命中的文档/切片标签集合
 */
public record RagGoldenQueryCase(
        String caseId,
        KnowledgeLanguage language,
        String query,
        List<String> expectedTags
) {
    /** 紧凑构造函数做输入防空与默认语种填充 */
    public RagGoldenQueryCase {
        caseId = (caseId == null) ? "" : caseId;
        language = (language == null) ? KnowledgeLanguage.OTHER : language;
        query = (query == null) ? "" : query;
        expectedTags = (expectedTags == null) ? List.of() : List.copyOf(expectedTags);
    }
}

