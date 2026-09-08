package com.acme.agentstudio.infrastructure.workflow;

import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工作流 Graph 画布拓扑校验器组件（Workflow Graph Validator）。
 * 校验顺序为：JSON 格式、节点字段、开始/结束节点数量、边引用、环路、连通性及孤立节点。
 */
@Component
public class WorkflowGraphValidator {
    /**
     * 工作流图校验器。
     * 校验顺序为：JSON 格式、节点字段、开始/结束节点数量、边引用、
     * 自环、入度/出度，以及从 START 到 END 的双向可达性。
     */

    private static final String FIELD_NODES = "nodes";
    private static final String FIELD_EDGES = "edges";
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_TARGET = "target";
    private static final String NODE_START = "START";
    private static final String NODE_END = "END";
    private static final String NODE_CONDITION = "CONDITION";

    private final ObjectMapper objectMapper;

    public WorkflowGraphValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

        /**
         * 验证validate 业务逻辑处理。
         *
         * @param graphJson graphJson 参数
         */
    public void validate(String graphJson) {
        // 先校验节点，再校验边；这样可以在边校验时确认 source/target 是否真实存在。
        JsonNode root = parse(graphJson);
        JsonNode nodes = root.get(FIELD_NODES);
        if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
            throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_NODES_REQUIRED);
        }

        // 保存节点 ID 和类型，后续用于检查重复节点以及查找 START、END。
        Set<String> nodeIds = new HashSet<>();
        Map<String, String> nodeTypes = new HashMap<>();
        int startCount = 0;
        int endCount = 0;
        for (JsonNode node : nodes) {
            String id = requiredText(node, FIELD_ID, ApplicationMessages.WORKFLOW_GRAPH_NODE_ID_REQUIRED);
            String type = requiredText(node, FIELD_TYPE, ApplicationMessages.WORKFLOW_GRAPH_NODE_TYPE_REQUIRED);
            if (!nodeIds.add(id)) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_DUPLICATE_NODE_PREFIX + id);
            }
            nodeTypes.put(id, type);
            if (NODE_START.equals(type)) {
                startCount++;
            }
            if (NODE_END.equals(type)) {
                endCount++;
            }
        }
        if (startCount != 1 || endCount != 1) {
            throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_START_END_REQUIRED);
        }

        validateEdges(root.get(FIELD_EDGES), nodeIds, nodeTypes);
        validateConditionNodes(nodes, nodeIds, root.get(FIELD_EDGES));
    }

    private void validateConditionNodes(JsonNode nodes, Set<String> nodeIds, JsonNode edges) {
        Map<String, Set<String>> targetsBySource = new HashMap<>();
        if (edges != null && edges.isArray()) {
            for (JsonNode edge : edges) {
                targetsBySource.computeIfAbsent(edge.path(FIELD_SOURCE).asText(), ignored -> new HashSet<>())
                        .add(edge.path(FIELD_TARGET).asText());
            }
        }
        for (JsonNode node : nodes) {
            if ("LLM".equals(node.path(FIELD_TYPE).asText())) {
                String modelKey = node.path("modelKey").asText(node.path("config").path("modelKey").asText(""));
                String backupModelKey = node.path("backupModelKey").asText(node.path("config").path("backupModelKey").asText(""));
                if (!backupModelKey.isBlank() && modelKey.equals(backupModelKey)) {
                    throw new IllegalArgumentException("模型生成节点的备用模型不能与主模型相同：" + node.path(FIELD_ID).asText());
                }
            }
            if ("HUMAN".equals(node.path(FIELD_TYPE).asText())) {
                JsonNode config = node.path("config");
                requireConfigured(config, "approvalTitle", "人工审批节点必须配置审批标题：" + node.path(FIELD_ID).asText());
                requireConfigured(config, "approvalDescription", "人工审批节点必须配置审批说明：" + node.path(FIELD_ID).asText());
                requireConfigured(config, "approvalGroup", "人工审批节点必须配置审批组：" + node.path(FIELD_ID).asText());
            }
            if (!NODE_CONDITION.equals(node.path(FIELD_TYPE).asText())) continue;
            JsonNode config = node.path("config");
            String operator = config.path("operator").asText("");
            if (!Set.of("CONTAINS", "EQUALS", "NOT_EMPTY").contains(operator)) {
                throw new IllegalArgumentException("条件节点必须配置受支持的判断操作：" + node.path(FIELD_ID).asText());
            }
            String trueTarget = config.path("trueTarget").asText("");
            String falseTarget = config.path("falseTarget").asText("");
            if (!nodeIds.contains(trueTarget) || !nodeIds.contains(falseTarget) || trueTarget.equals(falseTarget)) {
                throw new IllegalArgumentException("条件节点的真假后继必须是两个不同的有效节点：" + node.path(FIELD_ID).asText());
            }
            Set<String> outgoingTargets = targetsBySource.getOrDefault(node.path(FIELD_ID).asText(), Set.of());
            if (!outgoingTargets.contains(trueTarget) || !outgoingTargets.contains(falseTarget)) {
                throw new IllegalArgumentException("条件节点的真假后继必须都连出当前节点：" + node.path(FIELD_ID).asText());
            }
        }
    }

    private void requireConfigured(JsonNode config, String field, String message) {
        if (config == null || !config.path(field).isTextual() || config.path(field).asText().isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private JsonNode parse(String graphJson) {
        // 使用 Jackson 解析结构化 JSON，禁止通过字符串截取判断图结构。
        try {
            JsonNode root = objectMapper.readTree(graphJson);
            if (!root.isObject()) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_OBJECT_REQUIRED);
            }
            return root;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_JSON_INVALID, ex);
        }
    }

    private void validateEdges(JsonNode edges, Set<String> nodeIds, Map<String, String> nodeTypes) {
        // 同时构建正向图和反向图：正向图检查能否从 START 到达，反向图检查能否到达 END。
        if (nodeIds.size() > 1 && (edges == null || !edges.isArray() || edges.isEmpty())) {
            throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_EDGES_REQUIRED);
        }
        // 入度和出度用于拒绝孤立节点、开始节点入边以及结束节点出边。
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Set<String>> reverseAdjacency = new HashMap<>();
        for (String nodeId : nodeIds) {
            incoming.put(nodeId, 0);
            outgoing.put(nodeId, 0);
            adjacency.put(nodeId, new HashSet<>());
            reverseAdjacency.put(nodeId, new HashSet<>());
        }
        if (edges != null && edges.isArray()) {
            for (JsonNode edge : edges) {
                String source = requiredText(edge, FIELD_SOURCE, ApplicationMessages.WORKFLOW_GRAPH_EDGE_SOURCE_REQUIRED);
                String target = requiredText(edge, FIELD_TARGET, ApplicationMessages.WORKFLOW_GRAPH_EDGE_TARGET_REQUIRED);
                if (!nodeIds.contains(source) || !nodeIds.contains(target)) {
                    throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_EDGE_NODE_MISSING);
                }
                if (source.equals(target)) {
                    throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_SELF_LOOP_DENIED);
                }
                outgoing.put(source, outgoing.get(source) + 1);
                incoming.put(target, incoming.get(target) + 1);
                adjacency.get(source).add(target);
                reverseAdjacency.get(target).add(source);
            }
        }
        validateReachability(incoming, outgoing, nodeTypes, adjacency, reverseAdjacency);
    }

    private void validateReachability(
            Map<String, Integer> incoming,
            Map<String, Integer> outgoing,
            Map<String, String> nodeTypes,
            Map<String, Set<String>> adjacency,
            Map<String, Set<String>> reverseAdjacency
    ) {
        // 每个节点必须同时满足“从 START 可达”和“最终可达 END”，避免保存不可执行的流程图。
        String startId = nodeTypes.entrySet().stream()
                .filter(entry -> NODE_START.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        String endId = nodeTypes.entrySet().stream()
                .filter(entry -> NODE_END.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        Set<String> reachableFromStart = traverse(startId, adjacency);
        Set<String> reachableToEnd = traverse(endId, reverseAdjacency);
        for (Map.Entry<String, String> entry : nodeTypes.entrySet()) {
            String nodeId = entry.getKey();
            String nodeType = entry.getValue();
            if (NODE_START.equals(nodeType) && incoming.get(nodeId) > 0) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_START_INCOMING_DENIED);
            }
            if (NODE_END.equals(nodeType) && outgoing.get(nodeId) > 0) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_END_OUTGOING_DENIED);
            }
            if ("HUMAN".equals(nodeType) && outgoing.get(nodeId) != 1) {
                throw new IllegalArgumentException("人工审批节点必须且只能连接一个后继节点。");
            }
            if (!NODE_START.equals(nodeType) && incoming.get(nodeId) == 0) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_UNREACHABLE_NODE_PREFIX + nodeId);
            }
            if (!NODE_END.equals(nodeType) && outgoing.get(nodeId) == 0) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_DEAD_END_NODE_PREFIX + nodeId);
            }
            if (!reachableFromStart.contains(nodeId)) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_NOT_REACHABLE_FROM_START_PREFIX + nodeId);
            }
            if (!reachableToEnd.contains(nodeId)) {
                throw new IllegalArgumentException(ApplicationMessages.WORKFLOW_GRAPH_NOT_REACHABLE_TO_END_PREFIX + nodeId);
            }
        }
    }

    private Set<String> traverse(String root, Map<String, Set<String>> graph) {
        // 使用广度优先遍历，并通过 visited 防止图中存在环时无限循环。
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            pending.addAll(graph.getOrDefault(current, Set.of()));
        }
        return visited;
    }

    private String requiredText(JsonNode node, String fieldName, String message) {
        // 节点和边的关键字段必须是非空字符串，错误信息统一由消息常量提供。
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.asText();
    }

}
