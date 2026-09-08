package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.connector.ConnectorInvocationPort;
import com.acme.agentstudio.application.connector.HttpConnectorInvocationAdapter;
import com.acme.agentstudio.domain.workflow.model.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抽象上下文构建型节点处理器。
 * 为 ContextBuilder、PromptTemplate、SessionMemory 等上下文加工与管理节点提供变量截断与 VariablePatch 构造逻辑。
 */
abstract class AbstractContextNodeExecutionHandler implements NodeExecutionHandler {

    /** 默认上下文最大字符长度限制 */
    protected static final int DEFAULT_MAX_CHARS = 12_000;

    /** 变量引用解析器 */
    protected final VariableReferenceResolver resolver;

    /** 节点类型编码 */
    private final String nodeType;

    /**
     * 构造函数初始化依赖。
     */
    protected AbstractContextNodeExecutionHandler(VariableReferenceResolver resolver, String nodeType) {
        this.resolver = resolver;
        this.nodeType = nodeType;
    }

    /**
     * 返回无侧效应的节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(nodeType, NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 构造上下文变量更新补丁 VariablePatch。
     */
    protected VariablePatch patch(NodeExecutionRequest request, String variable, Object output) {
        return VariablePatch.builder()
                .put(resolver.variableKey(variable), output)
                .put(resolver.nodeOutputKey(request.nodeId()), output)
                .build();
    }

    /**
     * 对生成的上下文文本按最大长度进行截断（保留最新尾部文本）。
     */
    protected String limit(String value, int configuredMaxChars) {
        int maxChars = Math.max(500, Math.min(100_000, configuredMaxChars));
        return value.length() <= maxChars ? value : value.substring(value.length() - maxChars);
    }
}

/**
 * 多源上下文拼接组装节点处理器（CONTEXT_BUILDER）。
 */
@Component
class ContextBuilderNodeExecutionHandler extends AbstractContextNodeExecutionHandler {

    /** 默认引用的输入变量路径列表 */
    private static final List<String> DEFAULT_REFERENCES = List.of(
            "input.user_message", "context.conversationHistory", "variables.rag_context"
    );

    /**
     * 构造函数注入依赖。
     */
    ContextBuilderNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.CONTEXT_BUILDER.code());
    }

    /**
     * 拼接多个输入的上下文内容，生成格式化的 context 文本。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        JsonNode config = request.config();
        List<String> references = new ArrayList<>();
        config.path("inputReferences").forEach(item -> references.add(item.asText()));
        if (references.isEmpty()) {
            references.addAll(DEFAULT_REFERENCES);
        }

        String separator = config.path("separator").asText("\n\n");
        StringBuilder output = new StringBuilder();
        for (String reference : references) {
            Object value = request.inputs().get(reference);
            if (value == null || String.valueOf(value).isBlank()) {
                continue;
            }
            if (output.length() > 0) {
                output.append(separator);
            }
            output.append(config.path("labels").path(reference).asText(reference)).append(":\n").append(value);
        }

        String result = limit(output.toString(), config.path("maxChars").asInt(DEFAULT_MAX_CHARS));
        String variable = config.path("outputVariable").asText("context");
        return NodeExecutionOutcome.output(result, patch(request, variable, result));
    }
}

/**
 * 提示词模板生成节点处理器（PROMPT_TEMPLATE）。
 */
@Component
class PromptTemplateNodeExecutionHandler extends AbstractContextNodeExecutionHandler {
    PromptTemplateNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.PROMPT_TEMPLATE.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        String output = resolver.render(request.config().path("template").asText(""), request.inputs());
        String variable = request.config().path("outputVariable").asText("prompt");
        return NodeExecutionOutcome.output(output, patch(request, variable, output));
    }
}

/**
 * 会话历史记忆窗口裁剪节点处理器（SESSION_MEMORY）。
 */
@Component
class SessionMemoryNodeExecutionHandler extends AbstractContextNodeExecutionHandler {

    /** 默认滑动窗口对话条目数 */
    private static final int DEFAULT_WINDOW = 10;

    /**
     * 构造函数注入依赖。
     */
    SessionMemoryNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.SESSION_MEMORY.code());
    }

    /**
     * 根据配置的滑动窗口大小截取近期历史对话记录。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        Object raw = request.inputs().get("context.conversationHistory");
        List<?> history = raw instanceof List<?> list ? list : List.of();
        int window = Math.max(1, Math.min(100, request.config().path("window").asInt(DEFAULT_WINDOW)));
        List<?> output = history.subList(Math.max(0, history.size() - window), history.size());
        String variable = request.config().path("outputVariable").asText("memory");
        return NodeExecutionOutcome.output(output, patch(request, variable, output));
    }
}

/**
 * 高级 Agent 团队协作模式枚举。
 */
enum AdvancedCollaborationMode {
    /** 推理分析模式 */
    REASONING,
    /** 规划分解模式 */
    PLANNING,
    /** 多角色团队协作模式 */
    MULTI_ROLE
}

/**
 * 抽象高级 Agent 协同节点执行处理器（支持 Reasoning/Planning/MultiRole 算法集群）。
 */
abstract class AbstractAdvancedCollaborationNodeExecutionHandler implements NodeExecutionHandler {

    /** 默认外部协同服务 API 路径 */
    private static final String DEFAULT_INVOCATION_PATH = "/invoke";

    /** 节点类型 */
    private final String nodeType;

    /** 协同模式 */
    private final AdvancedCollaborationMode mode;

    /** HTTP 适配器 */
    private final HttpConnectorInvocationAdapter http;

    /** JSON 映射器 */
    private final ObjectMapper objectMapper;

    /** 变量解析器 */
    private final VariableReferenceResolver resolver;

    /**
     * 构造函数初始化依赖。
     */
    protected AbstractAdvancedCollaborationNodeExecutionHandler(String nodeType,
                                                                AdvancedCollaborationMode mode,
                                                                HttpConnectorInvocationAdapter http,
                                                                ObjectMapper objectMapper,
                                                                VariableReferenceResolver resolver) {
        this.nodeType = nodeType;
        this.mode = mode;
        this.http = http;
        this.objectMapper = objectMapper;
        this.resolver = resolver;
    }

    /**
     * 返回具备外部调用的节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(nodeType, NodeExecutionDescriptor.SideEffect.EXTERNAL_CALL);
    }

    /**
     * 执行高级 Agent 系统的分布式算法协作。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        long connectorId = request.config().path("connectorId").asLong(0);
        if (connectorId <= 0) {
            throw new IllegalArgumentException("高级 Agent 协作节点必须指定一个已启用的运行连接器 connectorId。");
        }

        CollaborationRequest payload = new CollaborationRequest(
                mode,
                request.config().path("graph"),
                request.config().path("agents"),
                request.inputs().values()
        );

        ConnectorInvocationPort.InvocationRequest invocation = new ConnectorInvocationPort.InvocationRequest(
                request.tenantId(),
                connectorId,
                "POST",
                request.config().path("path").asText(DEFAULT_INVOCATION_PATH),
                Map.of(),
                write(payload),
                request.config().path("idempotencyKey").asText(null),
                true
        );

        ConnectorInvocationPort.InvocationResult call = http.invoke(invocation);
        if (call.error() != null || call.statusCode() >= 400) {
            throw new IllegalStateException("高级 Agent 协作集群节点调用失败，状态码 [" + call.statusCode() + "]，错误信息：" + call.error());
        }

        CollaborationResult output = new CollaborationResult(mode, call.statusCode(), call.body(), call.attempts());
        return NodeExecutionOutcome.output(output, VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), output));
    }

    /** 序列化请求体 JSON */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("高级 Agent 协作请求序列化失败。", exception);
        }
    }

    /** 协作请求 Record */
    private record CollaborationRequest(AdvancedCollaborationMode mode, JsonNode graph, JsonNode participants, Map<String, Object> input) {
    }

    /** 协作结果 Record */
    private record CollaborationResult(AdvancedCollaborationMode mode, int statusCode, String body, int attempts) {
    }
}

/**
 * 深度推理 Agent 节点执行处理器（REASONING_AGENT）。
 */
@Component
class ReasoningAgentNodeExecutionHandler extends AbstractAdvancedCollaborationNodeExecutionHandler {
    ReasoningAgentNodeExecutionHandler(HttpConnectorInvocationAdapter http, ObjectMapper mapper, VariableReferenceResolver resolver) {
        super(NodeType.REASONING_AGENT.code(), AdvancedCollaborationMode.REASONING, http, mapper, resolver);
    }
}

/**
 * 任务规划 Agent 节点执行处理器（PLANNING_AGENT）。
 */
@Component
class PlanningAgentNodeExecutionHandler extends AbstractAdvancedCollaborationNodeExecutionHandler {
    PlanningAgentNodeExecutionHandler(HttpConnectorInvocationAdapter http, ObjectMapper mapper, VariableReferenceResolver resolver) {
        super(NodeType.PLANNING_AGENT.code(), AdvancedCollaborationMode.PLANNING, http, mapper, resolver);
    }
}

/**
 * 图规划 Agent 节点执行处理器（GRAPH_PLANNING）。
 */
@Component
class GraphPlanningNodeExecutionHandler extends AbstractAdvancedCollaborationNodeExecutionHandler {
    GraphPlanningNodeExecutionHandler(HttpConnectorInvocationAdapter http, ObjectMapper mapper, VariableReferenceResolver resolver) {
        super(NodeType.GRAPH_PLANNING.code(), AdvancedCollaborationMode.PLANNING, http, mapper, resolver);
    }
}

/**
 * Agent 团队协同节点执行处理器（AGENT_TEAM）。
 */
@Component
class AgentTeamNodeExecutionHandler extends AbstractAdvancedCollaborationNodeExecutionHandler {
    AgentTeamNodeExecutionHandler(HttpConnectorInvocationAdapter http, ObjectMapper mapper, VariableReferenceResolver resolver) {
        super(NodeType.AGENT_TEAM.code(), AdvancedCollaborationMode.MULTI_ROLE, http, mapper, resolver);
    }
}

/**
 * 多角色 Agent 节点执行处理器（MULTI_ROLE_AGENT）。
 */
@Component
class MultiAgentNodeExecutionHandler extends AbstractAdvancedCollaborationNodeExecutionHandler {
    MultiAgentNodeExecutionHandler(HttpConnectorInvocationAdapter http, ObjectMapper mapper, VariableReferenceResolver resolver) {
        super(NodeType.MULTI_ROLE_AGENT.code(), AdvancedCollaborationMode.MULTI_ROLE, http, mapper, resolver);
    }
}

