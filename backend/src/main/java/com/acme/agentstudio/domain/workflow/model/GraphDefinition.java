package com.acme.agentstudio.domain.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 工作流编排图完整拓扑结构定义 Record（Graph Definition）。
 * 包含编排图类型 graphType、Schema 版本号、输入/输出 Schema、节点集合 nodes、边集合 edges 以及上下文变量引用 variables。
 *
 * @param graphType 工作流图类型编码
 * @param schemaVersion 规范版本号
 * @param inputSchema 图级别的输入参数 JSON Schema
 * @param outputSchema 图级别的输出结构 JSON Schema
 * @param nodes 图包含的节点列表 List&lt;GraphNode&gt;
 * @param edges 节点间的有向边列表 List&lt;GraphEdge&gt;
 * @param variables 图中引用的跨节点变量引用列表 List&lt;VariableReference&gt;
 */
public record GraphDefinition(
        String graphType,
        String schemaVersion,
        JsonNode inputSchema,
        JsonNode outputSchema,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        List<VariableReference> variables
) {
}

