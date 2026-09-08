package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * RAG 知识检索评测聚合报告实体 Record（RAG Evaluation Report）。
 * 包含了各个评测维度与模式的指标明细列表 metrics (List&lt;RagEvaluationMetrics&gt;)。
 *
 * @param metrics 各维度评估指标度量列表
 */
public record RagEvaluationReport(
        List<RagEvaluationMetrics> metrics
) {
    /** 紧凑构造函数做输入数组防空保护 */
    public RagEvaluationReport {
        metrics = (metrics == null) ? List.of() : List.copyOf(metrics);
    }
}

