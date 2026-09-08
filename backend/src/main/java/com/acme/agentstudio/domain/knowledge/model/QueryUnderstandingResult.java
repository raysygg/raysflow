package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 知识检索前 Query 理解与改写纠错处理结果实体 Record（Query Understanding Result）。
 * 包含用户输入的原始 Query originalQuery、基于 LLM/同义词拓展改写后的 Query rewrittenQuery
 * 以及是否成功生效改写标记 rewritten (仅当改写有效且不等于原始 Query 时为 true)。
 *
 * @param originalQuery 用户发起的原始自然语言查询文本
 * @param rewrittenQuery 经过纠错、扩展、补全后的检索 Query
 * @param rewritten 是否成功产生了有差异的有效 Query 改写
 */
public record QueryUnderstandingResult(
        String originalQuery,
        String rewrittenQuery,
        boolean rewritten
) {
    /** 紧凑构造函数做防空保护与改写校验 */
    public QueryUnderstandingResult {
        originalQuery = (originalQuery == null) ? "" : originalQuery;
        rewrittenQuery = (rewrittenQuery == null) ? "" : rewrittenQuery;
        rewritten = rewritten && !rewrittenQuery.isBlank() && !rewrittenQuery.equals(originalQuery);
    }
}


