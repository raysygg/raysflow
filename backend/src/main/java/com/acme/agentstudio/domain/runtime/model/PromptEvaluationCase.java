package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;
import java.util.Set;

/**
 * 评测中心标准测试集中的单个离线评测测试样例 Record（Prompt Evaluation Case）。
 * 包含用例 ID caseId、输入文本 input、测试变量输入 Map variables、预期引用的知识源 ID 集合 expectedSourceIds 与预期标准输出结果 expectedOutput。
 *
 * @param caseId 用例唯一 ID
 * @param input 测评用户真实 Query 输入
 * @param variables 参数插值变量 Map
 * @param expectedSourceIds 预期命中的知识库文档段落源 ID 集合
 * @param expectedOutput 预期基准 Ground Truth 期望输出
 */
public record PromptEvaluationCase(
        String caseId,
        String input,
        Map<String, Object> variables,
        Set<String> expectedSourceIds,
        String expectedOutput
) {
    /** 紧凑构造函数做输入属性校验 */
    public PromptEvaluationCase {
        if (caseId == null || caseId.isBlank() || input == null) {
            throw new IllegalArgumentException("评测样例标识和输入不能为空");
        }
        variables = (variables == null) ? Map.of() : Map.copyOf(variables);
        expectedSourceIds = (expectedSourceIds == null) ? Set.of() : Set.copyOf(expectedSourceIds);
        expectedOutput = (expectedOutput == null) ? "" : expectedOutput;
    }
}

