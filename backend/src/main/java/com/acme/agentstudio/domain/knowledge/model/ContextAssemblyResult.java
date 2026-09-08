package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * RAG 知识切块父级上下文（Parent-Child / Window Context）组合拼接还原结果 Record（Context Assembly Result）。
 * 包含组合后的检索切片结果列表 results (List&lt;RagSearchResult&gt;)、父段落上下文覆盖率 parentCoverage (0.0~1.0)
 * 以及被跳过过滤的上下文节点列表 skippedContexts (List&lt;SkippedContext&gt;)。
 *
 * @param results 组装后的 RAG 检索结果节点列表
 * @param parentCoverage 父段落/滑动窗口在最终 Context 中的覆盖比率
 * @param skippedContexts 因超长或安全拦截而被跳过的 Context 列表
 */
public record ContextAssemblyResult(
        List<RagSearchResult> results,
        double parentCoverage,
        List<SkippedContext> skippedContexts
) {
    /**
     * 快捷构造函数，默认不提供跳过的 Context 列表。
     *
     * @param results 检索结果列表
     * @param parentCoverage 父段落覆盖比率
     */
    public ContextAssemblyResult(List<RagSearchResult> results, double parentCoverage) {
        this(results, parentCoverage, List.of());
    }

    /** 紧凑构造函数做输入数组防空与覆盖率边界裁剪 */
    public ContextAssemblyResult {
        results = (results == null) ? List.of() : List.copyOf(results);
        parentCoverage = Math.max(0D, Math.min(1D, parentCoverage));
        skippedContexts = (skippedContexts == null) ? List.of() : List.copyOf(skippedContexts);
    }
}

