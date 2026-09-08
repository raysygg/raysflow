package com.acme.agentstudio.infrastructure.workflow;

import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphEdge;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.domain.workflow.model.NodeType;
import com.acme.agentstudio.domain.workflow.model.ExecutionType;
import com.acme.agentstudio.domain.workflow.model.VariableReference;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工作流 Graph 画布强类型契约与参数输入输出校验器组件（Graph Contract Validator）。
 * 针对各类型节点的变量绑定、条件表达式与输入输出契约实施硬断言。
 */
@Component
public class GraphContractValidator {
    private static final String NODE_START = NodeType.START.code();
    private static final String NODE_END = NodeType.END.code();
    private static final String NODE_CONDITION = NodeType.CONDITION.code();
    private static final String FIELD_MODEL_ID = "modelId";
    private static final String FIELD_MODEL_KEY = "modelKey";
    private final GraphDefinitionParser parser;
    private final NodeRegistry nodeRegistry;

    public GraphContractValidator(GraphDefinitionParser parser, NodeRegistry nodeRegistry) {
        this.parser = parser;
        this.nodeRegistry = nodeRegistry;
    }

        /**
         * 验证validate 业务逻辑处理。
         *
         * @param graphJson graphJson 参数
         * @return GraphValidationResult 返回对象
         */
    public GraphValidationResult validate(String graphJson) {
        try {
            return validate(parser.read(graphJson));
        } catch (IllegalArgumentException ex) {
            return GraphValidationResult.invalid(null, List.of(ValidationIssue.error("GRAPH_JSON_INVALID", null, null, ex.getMessage(), "检查图 JSON 格式。")));
        }
    }

        /**
         * 验证validate 业务逻辑处理。
         *
         * @param graph graph 参数
         * @return GraphValidationResult 返回对象
         */
    public GraphValidationResult validate(GraphDefinition graph) {
        // 先校验图结构，再校验节点和边，前端才能得到稳定的字段路径与修复建议。
        List<ValidationIssue> issues = new ArrayList<>();
        if (graph == null) {
            return GraphValidationResult.invalid(null, List.of(ValidationIssue.error("GRAPH_REQUIRED", null, null, "编排图不能为空。", "提供有效的编排图。")));
        }
        if (!ExecutionType.ALL.contains(graph.graphType())) {
            issues.add(ValidationIssue.error("GRAPH_TYPE_UNSUPPORTED", null, "graphType", "图类型必须是应用主工作流。", "使用 APPLICATION_WORKFLOW 图类型。"));
        }
        Map<String, GraphNode> nodes = new HashMap<>();
        int starts = 0;
        int ends = 0;
        for (GraphNode node : graph.nodes()) {
            if (node.nodeId() == null || node.nodeId().isBlank()) {
                issues.add(ValidationIssue.error("NODE_ID_REQUIRED", null, "nodes[].nodeId", "节点 ID 不能为空。", "为节点生成稳定 ID。"));
                continue;
            }
            if (nodes.put(node.nodeId(), node) != null) {
                issues.add(ValidationIssue.error("DUPLICATE_NODE_ID", node.nodeId(), "nodes[].nodeId", "节点 ID 重复。", "确保每个节点 ID 唯一。"));
            }
            NodeTypeDescriptor descriptor = descriptor(node, issues);
            if (descriptor != null) {
                if (!descriptor.supportsGraphType(graph.graphType())) {
                    issues.add(ValidationIssue.error("NODE_GRAPH_TYPE_UNSUPPORTED", node.nodeId(), "nodes[].nodeType", "该节点类型不支持当前图类型。", "调整图类型或替换节点。"));
                }
                for (String required : descriptor.requiredConfigFields()) {
                    if (!hasConfiguredField(node.config(), required)) {
                        issues.add(ValidationIssue.error("NODE_CONFIG_REQUIRED", node.nodeId(), "nodes[].config." + required, "节点配置缺少必填字段。", "补充节点配置。"));
                    }
                }
                checkNodeContract(node, descriptor, issues);
            }
            if (NODE_START.equals(node.nodeType())) starts++;
            if (NODE_END.equals(node.nodeType())) ends++;
        }
        if (graph.nodes().isEmpty()) issues.add(ValidationIssue.error("NODES_REQUIRED", null, "nodes", "编排图至少需要一个节点。", "添加开始和结束节点。"));
        if (starts != 1 || ends != 1) issues.add(ValidationIssue.error("START_END_REQUIRED", null, "nodes", "编排图必须且只能有一个开始节点和一个结束节点。", "检查 START/END 节点数量。"));

        Map<String, Set<String>> forward = new HashMap<>();
        Map<String, Set<String>> reverse = new HashMap<>();
        Set<String> edgeKeys = new HashSet<>();
        Map<String, Integer> incoming = new HashMap<>();
        Map<String, Integer> outgoing = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            forward.put(nodeId, new HashSet<>());
            reverse.put(nodeId, new HashSet<>());
            incoming.put(nodeId, 0);
            outgoing.put(nodeId, 0);
        }
        for (GraphEdge edge : graph.edges()) {
            if (!nodes.containsKey(edge.sourceNodeId()) || !nodes.containsKey(edge.targetNodeId())) {
                issues.add(ValidationIssue.error("EDGE_NODE_MISSING", null, "edges[].targetNodeId", "边引用了不存在的节点。", "使用已存在的节点 ID。"));
                continue;
            }
            if (edge.sourceNodeId().equals(edge.targetNodeId())) {
                issues.add(ValidationIssue.error("SELF_LOOP_DENIED", edge.sourceNodeId(), "edges[]", "不允许节点连接自身。", "删除自环或拆分节点。"));
            }
            String key = String.join("|", edge.sourceNodeId(), edge.sourcePort(), edge.targetNodeId(), edge.targetPort());
            if (!edgeKeys.add(key)) issues.add(ValidationIssue.error("DUPLICATE_EDGE", edge.sourceNodeId(), "edges[]", "边重复。", "删除重复连线。"));
            NodeTypeDescriptor source = nodeRegistry.require(nodes.get(edge.sourceNodeId()).nodeType());
            NodeTypeDescriptor target = nodeRegistry.require(nodes.get(edge.targetNodeId()).nodeType());
            if (!source.supportsOutputPort(edge.sourcePort())) issues.add(ValidationIssue.error("SOURCE_PORT_INVALID", edge.sourceNodeId(), "edges[].sourcePort", "源端口不属于节点类型。", "使用节点注册中心提供的端口。"));
            if (!target.supportsInputPort(edge.targetPort())) issues.add(ValidationIssue.error("TARGET_PORT_INVALID", edge.targetNodeId(), "edges[].targetPort", "目标端口不属于节点类型。", "使用节点注册中心提供的端口。"));
            forward.get(edge.sourceNodeId()).add(edge.targetNodeId());
            reverse.get(edge.targetNodeId()).add(edge.sourceNodeId());
            outgoing.computeIfPresent(edge.sourceNodeId(), (ignored, value) -> value + 1);
            incoming.computeIfPresent(edge.targetNodeId(), (ignored, value) -> value + 1);
        }
        if (nodes.size() > 1 && graph.edges().isEmpty()) issues.add(ValidationIssue.error("EDGES_REQUIRED", null, "edges", "多节点编排图必须包含边。", "连接开始节点、业务节点和结束节点。"));
        checkReachability(nodes, forward, reverse, incoming, outgoing, issues);
        checkConditions(nodes, graph.edges(), issues);
        checkVariables(nodes, graph.variables(), issues);
        return issues.isEmpty() ? GraphValidationResult.valid(graph) : GraphValidationResult.invalid(graph, issues);
    }

        /**
         * 验证validateForPublish 业务逻辑处理。
         *
         * @param graph graph 参数
         * @return GraphValidationResult 返回对象
         */
    public GraphValidationResult validateForPublish(GraphDefinition graph) {
        // 草稿可以保存未接入执行器的节点，但发布必须保证节点具备真实运行能力。
        GraphValidationResult result = validate(graph);
        List<ValidationIssue> issues = new ArrayList<>(result.issues());
        if (result.graph() != null) {
            checkPublishContract(result.graph(), issues);
            for (GraphNode node : result.graph().nodes()) {
                NodeTypeDescriptor descriptor = nodeRegistry.find(node.nodeType());
                if (descriptor != null && !descriptor.active()) {
                    issues.add(ValidationIssue.error("NODE_EXECUTOR_UNAVAILABLE", node.nodeId(), "nodes[].nodeType",
                            "节点目录状态不是 ACTIVE，尚未具备可发布执行能力。", "先注册执行器并通过节点运行测试。"));
                }
            }
        }
        return issues.isEmpty() ? GraphValidationResult.valid(result.graph()) : GraphValidationResult.invalid(result.graph(), issues);
    }

    private void checkPublishContract(GraphDefinition graph, List<ValidationIssue> issues) {
        boolean hasBusinessNode = graph.nodes().stream()
                .anyMatch(node -> !Set.of(NODE_START, NODE_END, NodeType.USER_INPUT.code()).contains(node.nodeType()));
        if (!hasBusinessNode) {
            issues.add(ValidationIssue.error("BUSINESS_NODE_REQUIRED", null, "nodes",
                    "空白流程没有任何业务能力节点，不能发布到生产环境。",
                    "至少添加并配置一个模型、知识、工具、转换、条件或人工节点。"));
        }
        checkObjectSchema(graph.inputSchema(), "inputSchema", "输入", issues);
        checkObjectSchema(graph.outputSchema(), "outputSchema", "输出", issues);
    }

    private void checkObjectSchema(JsonNode schema, String field, String label, List<ValidationIssue> issues) {
        if (schema == null || !schema.isObject() || !"object".equals(schema.path("type").asText())
                || !schema.path("properties").isObject()) {
            issues.add(ValidationIssue.error("WORKFLOW_SCHEMA_INVALID", null, field,
                    label + "契约必须是包含 properties 的对象结构。",
                    "在应用输入输出中补充有效的对象 Schema。"));
        }
    }

        /**
         * normalize 方法。
         *
         * @param graphJson graphJson 参数
         * @return GraphDefinition 返回对象
         */
    public GraphDefinition normalize(String graphJson) {
        return parser.read(graphJson);
    }

        /**
         * 验证validateOrThrow 业务逻辑处理。
         *
         * @param graphJson graphJson 参数
         */
    public void validateOrThrow(String graphJson) {
        GraphValidationResult result = validate(graphJson);
        if (!result.valid()) throw new IllegalArgumentException(result.issues().get(0).message());
    }

    private NodeTypeDescriptor descriptor(GraphNode node, List<ValidationIssue> issues) {
        try {
            return nodeRegistry.require(node.nodeType());
        } catch (IllegalArgumentException ex) {
            issues.add(ValidationIssue.error("NODE_TYPE_UNREGISTERED", node.nodeId(), "nodes[].nodeType", ex.getMessage(), "从节点目录选择已注册类型。"));
            return null;
        }
    }

    private void checkReachability(Map<String, GraphNode> nodes, Map<String, Set<String>> forward, Map<String, Set<String>> reverse,
                                   Map<String, Integer> incoming, Map<String, Integer> outgoing, List<ValidationIssue> issues) {
        String start = nodes.values().stream().filter(node -> NODE_START.equals(node.nodeType())).map(GraphNode::nodeId).findFirst().orElse(null);
        String end = nodes.values().stream().filter(node -> NODE_END.equals(node.nodeType())).map(GraphNode::nodeId).findFirst().orElse(null);
        if (start == null || end == null) return;
        if (incoming.getOrDefault(start, 0) > 0) issues.add(ValidationIssue.error("START_INCOMING_DENIED", start, "edges", "开始节点不能有入边。", "删除指向 START 的连线。"));
        if (outgoing.getOrDefault(end, 0) > 0) issues.add(ValidationIssue.error("END_OUTGOING_DENIED", end, "edges", "结束节点不能有出边。", "删除从 END 发出的连线。"));
        Set<String> fromStart = traverse(start, forward);
        Set<String> toEnd = traverse(end, reverse);
        for (GraphNode node : nodes.values()) {
            if (!NODE_START.equals(node.nodeType()) && incoming.getOrDefault(node.nodeId(), 0) == 0) issues.add(ValidationIssue.error("NODE_INCOMING_REQUIRED", node.nodeId(), "edges", "节点没有入边。", "连接上游节点。"));
            if (!NODE_END.equals(node.nodeType()) && outgoing.getOrDefault(node.nodeId(), 0) == 0) issues.add(ValidationIssue.error("NODE_OUTGOING_REQUIRED", node.nodeId(), "edges", "节点没有出边。", "连接下游节点。"));
            if (!fromStart.contains(node.nodeId())) issues.add(ValidationIssue.error("NODE_UNREACHABLE_FROM_START", node.nodeId(), "nodes[].nodeId", "节点不可从开始节点到达。", "删除孤立节点或补充连线。"));
            if (!toEnd.contains(node.nodeId())) issues.add(ValidationIssue.error("NODE_UNREACHABLE_TO_END", node.nodeId(), "nodes[].nodeId", "节点无法到达结束节点。", "补充通往 END 的连线。"));
        }
    }

    private void checkConditions(Map<String, GraphNode> nodes, List<GraphEdge> edges, List<ValidationIssue> issues) {
        for (GraphNode node : nodes.values()) {
            if (!NODE_CONDITION.equals(node.nodeType())) continue;
            String trueTarget = node.config().path("trueTarget").asText("");
            String falseTarget = node.config().path("falseTarget").asText("");
            if (trueTarget.isBlank() || falseTarget.isBlank() || trueTarget.equals(falseTarget)
                    || !nodes.containsKey(trueTarget) || !nodes.containsKey(falseTarget)) {
                issues.add(ValidationIssue.error("CONDITION_BRANCH_REQUIRED", node.nodeId(), "nodes[].config.trueTarget", "条件节点必须配置两个不同且有效的分支目标。", "分别配置满足和不满足的后继节点。"));
                continue;
            }
            boolean hasTrue = edges.stream().anyMatch(edge -> edge.sourceNodeId().equals(node.nodeId())
                    && edge.targetNodeId().equals(trueTarget) && "true".equals(edge.sourcePort()));
            boolean hasFalse = edges.stream().anyMatch(edge -> edge.sourceNodeId().equals(node.nodeId())
                    && edge.targetNodeId().equals(falseTarget) && "false".equals(edge.sourcePort()));
            boolean hasUnexpectedBranch = edges.stream().anyMatch(edge -> edge.sourceNodeId().equals(node.nodeId())
                    && !(edge.targetNodeId().equals(trueTarget) && "true".equals(edge.sourcePort()))
                    && !(edge.targetNodeId().equals(falseTarget) && "false".equals(edge.sourcePort())));
            if (!hasTrue || !hasFalse || hasUnexpectedBranch) {
                issues.add(ValidationIssue.error("CONDITION_BRANCH_PORT_REQUIRED", node.nodeId(), "edges[].sourcePort",
                        "条件节点必须用 true 和 false 端口分别连接配置的两个分支目标。", "重新连接两个分支，或在画布中更新条件节点的后继节点。"));
            }
        }
    }

    private void checkVariables(Map<String, GraphNode> nodes, List<VariableReference> variables, List<ValidationIssue> issues) {
        for (VariableReference variable : variables) {
            if (!nodes.containsKey(variable.sourceNodeId())) issues.add(ValidationIssue.error("VARIABLE_SOURCE_NODE_MISSING", variable.sourceNodeId(), "variables[].sourceNodeId", "变量引用的源节点不存在。", "选择现有节点 ID。"));
            if (variable.outputPath() == null || variable.outputPath().isBlank() || !variable.outputPath().startsWith("$")) issues.add(ValidationIssue.error("VARIABLE_OUTPUT_PATH_INVALID", variable.sourceNodeId(), "variables[].outputPath", "变量输出路径必须以 $ 开头。", "使用例如 $.result.text 的输出路径。"));
            if (variable.dataType() == null || !Set.of("string", "number", "boolean", "object", "array", "any").contains(variable.dataType())) issues.add(ValidationIssue.error("VARIABLE_TYPE_INVALID", variable.sourceNodeId(), "variables[].dataType", "变量数据类型不受支持。", "使用 string、number、boolean、object、array 或 any。"));
        }
    }

    private void checkNodeContract(GraphNode node, NodeTypeDescriptor descriptor, List<ValidationIssue> issues) {
        JsonNode config = node.config();
        if (config == null || !config.isObject()) {
            issues.add(ValidationIssue.error("NODE_CONFIG_OBJECT_REQUIRED", node.nodeId(), "nodes[].config", "节点配置必须是结构化对象。", "补充结构化节点配置。"));
            return;
        }
        JsonNode schema = descriptor.configSchema();
        for (JsonNode required : schema.path("required")) {
            if (!hasConfiguredField(config, required.asText())) {
                issues.add(ValidationIssue.error("NODE_SCHEMA_REQUIRED", node.nodeId(), "nodes[].config." + required.asText(), "节点契约缺少必填字段。", "补充节点属性。"));
            }
        }
        for (JsonNode field : schema.path("fields")) {
            String name = field.path("name").asText(); JsonNode value = config.get(name);
            if (value == null || value.isNull()) continue;
            String widget = field.path("widget").asText();
            if (widget.contains("number") || "duration".equals(widget)) {
                if (!value.isNumber() || value.asDouble() < 0) issues.add(ValidationIssue.error("NODE_NUMBER_INVALID", node.nodeId(), "nodes[].config." + name, "节点数值配置无效。", "填写不小于零的数值。"));
            }
        }
        if (Set.of("LOOP", "ITERATION").contains(node.nodeType())) {
            int bound = config.path("maxIterations").asInt(config.path("maxItems").asInt(0));
            if (bound <= 0 || bound > 10000) issues.add(ValidationIssue.error("LOOP_BOUND_INVALID", node.nodeId(), "nodes[].config.maxIterations", "循环次数必须在 1 到 10000 之间。", "设置明确且有限的循环上限。"));
        }
        if ("ITERATION".equals(node.nodeType()) && (!config.has("bodyNodeId") || config.path("bodyNodeId").asText().isBlank())) {
            issues.add(ValidationIssue.error("ITERATION_BODY_REQUIRED", node.nodeId(), "nodes[].config.bodyNodeId",
                    "迭代节点必须配置循环体节点。", "选择每个数组元素要执行的节点。"));
        }
        JsonNode retry = config.path("retryPolicy");
        if (retry.isObject() && retry.path("maxAttempts").asInt(1) > 5) issues.add(ValidationIssue.error("RETRY_ATTEMPTS_INVALID", node.nodeId(), "nodes[].config.retryPolicy.maxAttempts", "重试次数超过平台上限。", "最多设置五次重试。"));
    }

    /** 兼容历史节点目录的 modelId，同时统一运行时使用的 modelKey 模型编码。 */
    private boolean hasConfiguredField(JsonNode config, String field) {
        if (isTextConfigured(config, field)) return true;
        return FIELD_MODEL_ID.equals(field) && isTextConfigured(config, FIELD_MODEL_KEY);
    }

    private boolean isTextConfigured(JsonNode config, String field) {
        JsonNode value = config.get(field);
        return value != null && !value.isNull() && !value.asText().isBlank();
    }

    private Set<String> traverse(String root, Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!visited.add(current)) continue;
            pending.addAll(graph.getOrDefault(current, Set.of()));
        }
        return visited;
    }
}
