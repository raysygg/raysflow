package com.acme.agentstudio.domain.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工作流拓扑图单个节点定义 Record（Graph Node）。
 *
 * @param nodeId 节点唯一 ID
 * @param nodeType 节点类型编码（如 START, END, LLM, RAG_RETRIEVAL 等）
 * @param title 节点在画布上显示的名称标题
 * @param x 可视化画布 X 轴坐标
 * @param y 可视化画布 Y 轴坐标
 * @param config 节点私有属性配置 ObjectNode
 * @param inputSchema 节点输入数据契约 JSON Schema
 * @param outputSchema 节点输出数据契约 JSON Schema
 */
public record GraphNode(
        String nodeId,
        String nodeType,
        String title,
        Double x,
        Double y,
        JsonNode config,
        JsonNode inputSchema,
        JsonNode outputSchema
) {
}

