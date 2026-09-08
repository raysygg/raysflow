package com.acme.agentstudio.infrastructure.workflow;

import com.acme.agentstudio.domain.workflow.model.ExecutionType;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphEdge;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.domain.workflow.model.VariableReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流 JSON 拓扑图解析与序列化组件（Graph Definition Parser）。
 * 负责将前端可视化画布存入数据库的复杂 JSON 字符串反序列化并规格化映射为标准的领域对象 GraphDefinition，
 * 同时支持将 GraphDefinition 归一化重新写回序列化 JSON ObjectNode。
 */
@Component
public class GraphDefinitionParser {

    /** Jackson JSON 序列化映射组件 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入 ObjectMapper。
     *
     * @param objectMapper Jackson 映射组件
     */
    public GraphDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将 JSON 拓扑图字符串解析并规格化为 GraphDefinition。
     *
     * @param graphJson 工作流图 JSON 字符串
     * @return 规格化后的 GraphDefinition 实例
     */
    public GraphDefinition read(String graphJson) {
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("工作流编排图根节点必须是合法的 JSON 对象。");
            }
            return normalize(root);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("工作流编排图 JSON 语法格式无效：" + ex.getMessage(), ex);
        }
    }

    /**
     * 规格化 JsonNode 根节点为标准的 GraphDefinition 领域实体。
     *
     * @param root 拓扑图根 JsonNode
     * @return GraphDefinition 领域实体
     */
    public GraphDefinition normalize(JsonNode root) {
        String graphType = text(root, "graphType", ExecutionType.APPLICATION_WORKFLOW);
        String schemaVersion = text(root, "schemaVersion", "1.0");

        JsonNode inputSchema = root.path("inputSchema").isObject() ? root.path("inputSchema") : JsonNodeFactory.instance.objectNode();
        JsonNode outputSchema = root.path("outputSchema").isObject() ? root.path("outputSchema") : JsonNodeFactory.instance.objectNode();

        List<GraphNode> nodes = new ArrayList<>();
        JsonNode rawNodes = root.path("nodes");
        if (rawNodes.isArray()) {
            for (JsonNode raw : rawNodes) {
                nodes.add(normalizeNode(raw));
            }
        }

        List<GraphEdge> edges = new ArrayList<>();
        JsonNode rawEdges = root.path("edges");
        if (rawEdges.isArray()) {
            int index = 0;
            for (JsonNode raw : rawEdges) {
                String edgeId = text(raw, "edgeId", "edge-" + index++);
                String sourceNodeId = text(raw, "sourceNodeId", text(raw, "source", ""));
                String sourcePort = text(raw, "sourcePort", "default");
                String targetNodeId = text(raw, "targetNodeId", text(raw, "target", ""));
                String targetPort = text(raw, "targetPort", "default");

                edges.add(new GraphEdge(edgeId, sourceNodeId, sourcePort, targetNodeId, targetPort));
            }
        }

        List<VariableReference> variables = new ArrayList<>();
        JsonNode rawVariables = root.path("variables");
        if (rawVariables.isArray()) {
            for (JsonNode raw : rawVariables) {
                variables.add(new VariableReference(
                        text(raw, "sourceNodeId", text(raw, "sourceNode", "")),
                        text(raw, "outputPath", ""),
                        text(raw, "dataType", "string"),
                        raw.path("sensitive").asBoolean(false)
                ));
            }
        }

        return new GraphDefinition(graphType, schemaVersion, inputSchema, outputSchema, nodes, edges, variables);
    }

    /**
     * 将 GraphDefinition 编排图转写回可持久化的 ObjectNode JSON 结构。
     *
     * @param graph 拓扑图 GraphDefinition 对象
     * @return 导出的 ObjectNode 实例
     */
    public ObjectNode write(GraphDefinition graph) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("graphType", graph.graphType());
        root.put("schemaVersion", graph.schemaVersion());
        root.set("inputSchema", graph.inputSchema());
        root.set("outputSchema", graph.outputSchema());

        ArrayNode nodes = root.putArray("nodes");
        for (GraphNode node : graph.nodes()) {
            ObjectNode value = nodes.addObject();
            value.put("id", node.nodeId());
            value.put("type", node.nodeType());
            value.put("name", node.title());
            if (node.x() != null) {
                value.put("x", node.x());
            }
            if (node.y() != null) {
                value.put("y", node.y());
            }
            value.set("config", node.config());
            value.set("inputSchema", node.inputSchema());
            value.set("outputSchema", node.outputSchema());
        }

        ArrayNode edges = root.putArray("edges");
        for (GraphEdge edge : graph.edges()) {
            ObjectNode value = edges.addObject();
            value.put("edgeId", edge.edgeId());
            value.put("sourceNodeId", edge.sourceNodeId());
            value.put("sourcePort", edge.sourcePort());
            value.put("targetNodeId", edge.targetNodeId());
            value.put("targetPort", edge.targetPort());
        }

        ArrayNode variables = root.putArray("variables");
        for (VariableReference variable : graph.variables()) {
            ObjectNode value = variables.addObject();
            value.put("sourceNodeId", variable.sourceNodeId());
            value.put("outputPath", variable.outputPath());
            value.put("dataType", variable.dataType());
            value.put("sensitive", variable.sensitive());
        }

        return root;
    }

    /** 规格化单个节点对象 */
    private GraphNode normalizeNode(JsonNode raw) {
        ObjectNode config = raw.path("config").isObject()
                ? (ObjectNode) raw.path("config").deepCopy()
                : objectMapper.createObjectNode();

        for (String field : List.of(
                "modelId", "modelKey", "backupModelId", "backupModelKey", "promptTemplate",
                "agentId", "operator", "value", "trueTarget", "falseTarget",
                "approvalTitle", "approvalDescription", "approvalGroupId", "approvalGroup", "riskLevel"
        )) {
            if (!config.has(field) && raw.has(field)) {
                config.set(field, raw.get(field));
            }
        }

        JsonNode inputSchema = raw.path("inputSchema").isObject() ? raw.path("inputSchema") : JsonNodeFactory.instance.objectNode();
        JsonNode outputSchema = raw.path("outputSchema").isObject() ? raw.path("outputSchema") : JsonNodeFactory.instance.objectNode();

        return new GraphNode(
                text(raw, "nodeId", text(raw, "id", "")),
                text(raw, "nodeType", text(raw, "type", "")),
                text(raw, "title", text(raw, "name", "")),
                decimal(raw, "x"),
                decimal(raw, "y"),
                config,
                inputSchema,
                outputSchema
        );
    }

    /** 安全解析带数值的坐标坐标系浮点数 */
    private Double decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isValueNode()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 安全获取节点的字符串属性值 */
    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value != null && value.isValueNode() && !value.asText().isBlank()) {
            return value.asText();
        }
        return fallback;
    }
}
