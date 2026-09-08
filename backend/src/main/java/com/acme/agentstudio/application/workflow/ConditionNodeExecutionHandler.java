package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 条件分支节点执行处理器（CONDITION）。
 * 根据配置的操作符（EQUALS / NOT_EMPTY / CONTAINS）评估条件，只计算路由分支（true / false 端口），不直接强行推进流程。
 */
@Component
class ConditionNodeExecutionHandler implements NodeExecutionHandler {

    /** True 路由分支端口标识 */
    private static final String PORT_TRUE = "true";

    /** False 路由分支端口标识 */
    private static final String PORT_FALSE = "false";

    /** 变量引用解析器 */
    private final VariableReferenceResolver resolver;

    /**
     * 构造函数注入依赖组件。
     */
    ConditionNodeExecutionHandler(VariableReferenceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 返回条件节点的路由执行描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.routingCapability(NodeType.CONDITION.code(), NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 执行条件逻辑评估，并产生响应的路由分支与 VariablePatch。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        JsonNode config = request.config();
        boolean matched = evaluate(config, request.inputs());
        VariablePatch patch = VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), matched);
        return NodeExecutionOutcome.route(matched, matched ? PORT_TRUE : PORT_FALSE, patch);
    }

    /**
     * 根据节点配置与当前输入动态评估条件表达式。
     */
    private boolean evaluate(JsonNode config, NodeInputValues inputs) {
        if (config.path("value").isBoolean() && config.path("operator").asText("").isBlank()) {
            return config.path("value").asBoolean();
        }
        String reference = config.path("inputReference").asText("");
        Object value = resolver.require(reference, inputs);
        String actual = String.valueOf(value);
        String expected = config.path("value").asText(config.path("equals").asText(""));

        return switch (config.path("operator").asText("EQUALS")) {
            case "NOT_EMPTY" -> !actual.isBlank();
            case "EQUALS" -> actual.equals(expected);
            case "CONTAINS" -> actual.contains(expected);
            default -> throw new IllegalArgumentException("不支持的条件判断操作符：" + config.path("operator").asText());
        };
    }
}

