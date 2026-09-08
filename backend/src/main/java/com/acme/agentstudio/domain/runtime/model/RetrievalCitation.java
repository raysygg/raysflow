package com.acme.agentstudio.domain.runtime.model;

/**
 * 知识库 RAG 检索召回结果对前端界面与溯源暴露的明细引用 Record（Retrieval Citation）。
 * 包含召回源 ID sourceId、文献标签/标题 sourceLabel、相关度匹配得分 score 及引用片段文本 excerpt。
 *
 * @param sourceId 知识切块/文档段落唯一 ID
 * @param sourceLabel 来源文档的中文标题或标签说明
 * @param score 向量检索/重排器打分相关度得分 (0.0 ~ 1.0)
 * @param excerpt 被引用的文本片段摘录内容
 */
public record RetrievalCitation(
        String sourceId,
        String sourceLabel,
        double score,
        String excerpt
) {
    /** 紧凑构造函数做输入验证校验 */
    public RetrievalCitation {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("引用来源标识不能为空");
        }
        excerpt = (excerpt == null) ? "" : excerpt;
    }
}

