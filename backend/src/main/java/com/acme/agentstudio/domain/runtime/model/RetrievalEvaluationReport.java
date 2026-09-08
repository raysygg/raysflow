package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 知识库 RAG 检索策略效果对比与评测报告 Record（Retrieval Evaluation Report）。
 * 包含策略名称 policyName、总用例数 totalCases、命中用例数 hitCases、命中率 hitRate (0.0~1.0)、
 * 引用覆盖率 citationCoverage (0.0~1.0)、指标明细 Map metrics 与失败用例描述 List failures。
 *
 * @param policyName 评估的检索策略名称说明
 * @param totalCases 测试集样例总数
 * @param hitCases 成功命中期望段落的样例总数
 * @param hitRate 总体命中召回率（0.0 ~ 1.0）
 * @param citationCoverage 答复中成功包含溯源引用的覆盖率（0.0 ~ 1.0）
 * @param metrics 包含 Precision, Recall, MAP, MRR 等细粒度指标 Map
 * @param failures 评测未命中的失败用例 ID 或原因列表
 */
public record RetrievalEvaluationReport(
        String policyName,
        int totalCases,
        int hitCases,
        double hitRate,
        double citationCoverage,
        Map<String, Object> metrics,
        List<String> failures
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public RetrievalEvaluationReport {
        if (policyName == null || policyName.isBlank() || totalCases < 0 || hitCases < 0) {
            throw new IllegalArgumentException("检索评测报告参数无效");
        }
        if (hitRate < 0D || hitRate > 1D || citationCoverage < 0D || citationCoverage > 1D) {
            throw new IllegalArgumentException("检索评测比例必须在零到一之间");
        }
        metrics = (metrics == null) ? Map.of() : Map.copyOf(metrics);
        failures = (failures == null) ? List.of() : List.copyOf(failures);
    }
}

