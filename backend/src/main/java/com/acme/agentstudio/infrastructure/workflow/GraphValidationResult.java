package com.acme.agentstudio.infrastructure.workflow;

import com.acme.agentstudio.domain.workflow.model.GraphDefinition;

import java.util.List;

/**
 * 工作流画布拓扑图与节点契约校验结果 Record（Graph Validation Result）。
 *
 * @param valid 拓扑图校验是否整体通过（true 表示合法且无 BLOCKING 阻断性错误）
 * @param graph 校验的目标工作流拓扑图对象 GraphDefinition
 * @param issues 包含的错误与告警提示条目列表 List&lt;ValidationIssue&gt;
 */
public record GraphValidationResult(
        boolean valid,
        GraphDefinition graph,
        List<ValidationIssue> issues
) {
    /**
     * 静态工厂方法：构建校验通过的 GraphValidationResult 实例。
     *
     * @param graph 校验合格的工作流图对象
     * @return 标记 valid=true 的结果对象
     */
    public static GraphValidationResult valid(GraphDefinition graph) {
        return new GraphValidationResult(true, graph, List.of());
    }

    /**
     * 静态工厂方法：构建校验未通过的 GraphValidationResult 实例。
     *
     * @param graph 校验的工作流图对象
     * @param issues 包含的校验问题列表
     * @return 标记 valid=false 的结果对象
     */
    public static GraphValidationResult invalid(GraphDefinition graph, List<ValidationIssue> issues) {
        return new GraphValidationResult(false, graph, List.copyOf(issues));
    }
}

