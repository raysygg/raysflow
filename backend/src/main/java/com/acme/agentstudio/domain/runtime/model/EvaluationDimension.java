package com.acme.agentstudio.domain.runtime.model;

/**
 * 离线自动化评测与发布门禁证据维度枚举（Evaluation Dimension）。
 * 覆盖任务完成度 (TASK_COMPLETION)、Grounding 忠实度 (GROUNDEDNESS)、引用准确率 (CITATION_CORRECTNESS)、
 * 工具选择正确性 (TOOL_SELECTION)、规划完成度 (PLAN_COMPLETION) 与安全合规 (SAFETY)。
 */
public enum EvaluationDimension {

    /** 任务目标最终完成度 */
    TASK_COMPLETION,

    /** RAG 检索 Grounding 忠实度与无幻觉率 */
    GROUNDEDNESS,

    /** 引用角标与文本匹配准确率 */
    CITATION_CORRECTNESS,

    /** 工具选择与参数提取正确性 */
    TOOL_SELECTION,

    /** 步骤规划完成度 */
    PLAN_COMPLETION,

    /** 内容与合规安全防护等级 */
    SAFETY
}

