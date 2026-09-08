package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 语义化双层（父块+子块 Parent-Child）结构化切片生成计划实体 Record（Semantic Chunk Plan）。
 * 父切块（Parent Chunk）保留完整段落上下文用于注入 Prompt，子切块（Child Chunk）保持短小精确用于向量检索。
 *
 * @param parents 父切块列表 (List&lt;ParentChunk&gt;)
 * @param children 子切块列表 (List&lt;ChildChunk&gt;)
 */
public record SemanticChunkPlan(
        List<ParentChunk> parents,
        List<ChildChunk> children
) {
    /** 紧凑构造函数做输入 List 防空保护 */
    public SemanticChunkPlan {
        parents = (parents == null) ? List.of() : List.copyOf(parents);
        children = (children == null) ? List.of() : List.copyOf(children);
    }

    /** 大段落父切块实体 Record */
    public record ParentChunk(
            int ordinal,
            String text,
            String sectionPath,
            Integer pageNo,
            int tokenCount,
            String contentHash
    ) {
    }

    /** 小粒度向量子切块实体 Record */
    public record ChildChunk(
            int ordinal,
            int parentOrdinal,
            String text,
            String embeddingText,
            String sectionPath,
            Integer pageNo,
            int tokenCount,
            String contentHash
    ) {
    }
}


