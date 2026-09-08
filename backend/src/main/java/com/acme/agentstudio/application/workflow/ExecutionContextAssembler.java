package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流统一执行上下文（Execution Context）组装器。
 * 负责将应用输入、流程变量、Prompt 模板、模型引用和输出映射组装成统一执行上下文环境。
 * 节点执行器统一消费 variables，其它映射数据用于运行轨迹记录与可视化回放，避免前端与后台各玩各的导致字段不一致。
 */
@Component
public class ExecutionContextAssembler {

    /** LLM 节点类型标识 */
    private static final String NODE_LLM = "LLM";

    /** END 节点类型标识 */
    private static final String NODE_END = "END";

    /** 默认输入变量 Key */
    private static final String DEFAULT_INPUT_KEY = WorkflowVariableNames.VARIABLE_INPUT;

    /** 默认输出 JSONPath */
    private static final String DEFAULT_OUTPUT_PATH = "$.output";

    /** 上下文键：输入参数 */
    private static final String KEY_INPUT = "input";

    /** 上下文键：全局变量 Map */
    private static final String KEY_VARIABLES = "variables";

    /** 上下文键：提示词配置 */
    private static final String KEY_PROMPT = "prompt";

    /** 上下文键：模型引用信息 */
    private static final String KEY_MODEL = "model";

    /** 上下文键：最终输出结果 */
    private static final String KEY_OUTPUT = "output";

    /** 上下文键：映射策略 */
    private static final String KEY_MAPPINGS = "mappings";

    /** 映射键：输入到变量 */
    private static final String KEY_INPUT_TO_VARIABLE = "inputToVariable";

    /** 映射键：从变量到输出 */
    private static final String KEY_OUTPUT_FROM_VARIABLE = "outputFromVariable";

    /** 映射键：模板 */
    private static final String KEY_TEMPLATE = "template";

    /** 映射键：模型 Key */
    private static final String KEY_MODEL_KEY = "modelKey";

    /** 映射键：模型 ID */
    private static final String KEY_MODEL_ID = "modelId";

    /** 映射键：输出变量名 */
    private static final String KEY_OUTPUT_VARIABLE = "outputVariable";

    /** 映射键：输出路径 */
    private static final String KEY_OUTPUT_PATH = "outputPath";

    /**
     * 创建一次工作流运行的初始上下文，校验 Graph 的 Schema 输入要求并归一化变量映射。
     *
     * @param graph 工作流图定义
     * @param input 外部传入的原始输入 Map
     * @return 组装完成的执行上下文 Context Map
     */
    public Map<String, Object> initialize(GraphDefinition graph, Map<String, Object> input) {
        if (graph == null) {
            throw new IllegalArgumentException("工作流执行图 Graph 不能为空。");
        }
        Map<String, Object> normalizedInput = new LinkedHashMap<>(input == null ? Map.of() : input);
        validateRequiredFields(graph.inputSchema(), normalizedInput, "工作流输入参数");

        Map<String, Object> variables = new LinkedHashMap<>();
        normalizedInput.forEach((name, value) -> variables.put("input." + name, value));
        Object request = firstValue(normalizedInput, "request", "input", "query", "user_message");
        if (request != null) {
            variables.putIfAbsent(WorkflowVariableNames.VARIABLE_INPUT, request);
            variables.putIfAbsent(WorkflowVariableNames.VARIABLE_QUERY, request);
            variables.putIfAbsent(WorkflowVariableNames.VARIABLE_USER_MESSAGE, request);
        }

        GraphNode llm = graph.nodes().stream().filter(node -> NODE_LLM.equals(node.nodeType())).findFirst().orElse(null);
        GraphNode end = graph.nodes().stream().filter(node -> NODE_END.equals(node.nodeType())).findFirst().orElse(null);

        Map<String, Object> prompt = new LinkedHashMap<>();
        Map<String, Object> model = new LinkedHashMap<>();
        if (llm != null) {
            prompt.put(KEY_TEMPLATE, text(llm.config(), "promptTemplate"));
            model.put(KEY_MODEL_KEY, text(llm.config(), KEY_MODEL_KEY));
            model.put(KEY_MODEL_ID, text(llm.config(), KEY_MODEL_ID));
            variables.put("context.promptTemplate", text(llm.config(), "promptTemplate"));
        }

        String outputVariable = end == null ? null : text(end.config(), KEY_OUTPUT_VARIABLE);
        String outputPath = end == null ? DEFAULT_OUTPUT_PATH : text(end.config(), KEY_OUTPUT_PATH);
        if (outputVariable == null || outputVariable.isBlank()) {
            outputVariable = llm == null ? DEFAULT_INPUT_KEY : "nodes." + llm.nodeId() + ".output";
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put(KEY_OUTPUT_VARIABLE, outputVariable);
        output.put(KEY_OUTPUT_PATH, outputPath == null || outputPath.isBlank() ? DEFAULT_OUTPUT_PATH : outputPath);

        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put(KEY_INPUT_TO_VARIABLE, DEFAULT_INPUT_KEY);
        mappings.put(KEY_OUTPUT_FROM_VARIABLE, outputVariable);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put(KEY_INPUT, normalizedInput);
        context.put(KEY_VARIABLES, variables);
        context.put(KEY_PROMPT, prompt);
        context.put(KEY_MODEL, model);
        context.put(KEY_OUTPUT, output);
        context.put(KEY_MAPPINGS, mappings);
        return context;
    }

    /**
     * 将节点或工作流完成后的最终业务输出对象写回统一上下文。
     *
     * @param context 执行上下文 Context Map
     * @param output 最终业务输出对象
     */
    public void complete(Map<String, Object> context, Object output) {
        if (context == null) {
            throw new IllegalArgumentException("执行上下文 Context 不能为 null。");
        }
        context.put(KEY_OUTPUT, output == null ? Map.of() : output);
    }

    /**
     * 从统一执行上下文中提取 variables 变量 Map（供各类 NodeExecutionHandler 消费使用）。
     *
     * @param context 执行上下文 Context Map
     * @return 变量映射表
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> variables(Map<String, Object> context) {
        Object value = context == null ? null : context.get(KEY_VARIABLES);
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("当前执行上下文中缺失有效的 variables 变量映射表。");
        }
        return (Map<String, Object>) map;
    }

    /** 校验 Schema 中定义的 required 必填字段是否存在 */
    private void validateRequiredFields(JsonNode schema, Map<String, Object> input, String label) {
        if (schema == null || !schema.isObject() || !schema.path("required").isArray()) {
            return;
        }
        for (JsonNode required : schema.path("required")) {
            String field = required.asText("");
            if (!field.isBlank() && !input.containsKey(field)) {
                throw new IllegalArgumentException(label + "缺少必填参数字段：" + field);
            }
        }
    }

    /** 依次提取 Map 中第一个非空的属性值 */
    private Object firstValue(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /** 安全提取 JsonNode 文本属性 */
    private String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return "";
        }
        String value = node.path(field).asText("");
        return value == null ? "" : value;
    }
}

