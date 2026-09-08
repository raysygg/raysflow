package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.connector.ConnectorInvocationPort;
import com.acme.agentstudio.application.connector.HttpConnectorInvocationAdapter;
import com.acme.agentstudio.application.connector.McpConnectorInvocationAdapter;
import com.acme.agentstudio.domain.workflow.model.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 抽象连接器节点执行处理器。
 * 为基于 HttpConnector / McpConnector 的外部接口调用节点提供通用的请求构建、结果解析与异常处理抽象。
 */
abstract class AbstractConnectorNodeExecutionHandler implements NodeExecutionHandler {

    /** 默认 HTTP 请求方法 */
    protected static final String DEFAULT_HTTP_METHOD = "POST";

    /** 变量引用解析器 */
    protected final VariableReferenceResolver resolver;

    /** 节点类型编码 */
    private final String nodeType;

    /**
     * 构造函数初始化类型与解析器。
     */
    protected AbstractConnectorNodeExecutionHandler(VariableReferenceResolver resolver, String nodeType) {
        this.resolver = resolver;
        this.nodeType = nodeType;
    }

    /**
     * 返回带有外部调用侧效应（EXTERNAL_CALL）的节点执行描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(nodeType, NodeExecutionDescriptor.SideEffect.EXTERNAL_CALL);
    }

    /**
     * 根据节点配置参数构建通用的 InvocationRequest。
     */
    protected ConnectorInvocationPort.InvocationRequest request(NodeExecutionRequest request) {
        JsonNode config = request.config();
        Map<String, String> headers = new LinkedHashMap<>();
        config.path("headers").fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
        String body = config.path("body").isMissingNode() ? null : config.path("body").toString();

        return new ConnectorInvocationPort.InvocationRequest(
                request.tenantId(),
                config.path("connectorId").asLong(0),
                config.path("method").asText(DEFAULT_HTTP_METHOD),
                config.path("path").asText(""),
                headers,
                body,
                config.path("idempotencyKey").asText(null),
                true
        );
    }

    /**
     * 解析连接器调用结果，构造节点输出小结并更新变量补丁。
     */
    protected NodeExecutionOutcome result(NodeExecutionRequest request,
                                          ConnectorInvocationPort.InvocationResult invocation) {
        if (invocation.error() != null || invocation.statusCode() >= 400) {
            throw new IllegalStateException("第三方连接器调用失败，状态码 [" + invocation.statusCode() + "]，错误原因：" + invocation.error());
        }
        ConnectorOutput output = new ConnectorOutput(invocation.statusCode(), invocation.body(), invocation.attempts());
        return NodeExecutionOutcome.output(output, VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), output));
    }

    /** 连接器输出 Record */
    public record ConnectorOutput(int statusCode, String body, int attempts) {
    }
}

/**
 * 抽象 HTTP 协议连接器节点处理器。
 */
abstract class AbstractHttpConnectorNodeExecutionHandler extends AbstractConnectorNodeExecutionHandler {

    /** HTTP 连接器调用适配器 */
    private final HttpConnectorInvocationAdapter http;

    /**
     * 构造函数注入依赖组件。
     */
    protected AbstractHttpConnectorNodeExecutionHandler(HttpConnectorInvocationAdapter http,
                                                         VariableReferenceResolver resolver,
                                                         String nodeType) {
        super(resolver, nodeType);
        this.http = http;
    }

    /**
     * 执行 HTTP 类型的连接器请求。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return result(request, http.invoke(request(request)));
    }
}

/**
 * HTTP 请求节点执行处理器（HTTP_REQUEST）。
 */
@Component
class HttpRequestNodeExecutionHandler extends AbstractHttpConnectorNodeExecutionHandler {
    HttpRequestNodeExecutionHandler(HttpConnectorInvocationAdapter http, VariableReferenceResolver resolver) {
        super(http, resolver, NodeType.HTTP_REQUEST.code());
    }
}

/**
 * OpenAPI 规范节点执行处理器（OPENAPI）。
 */
@Component
class OpenApiNodeExecutionHandler extends AbstractHttpConnectorNodeExecutionHandler {
    OpenApiNodeExecutionHandler(HttpConnectorInvocationAdapter http, VariableReferenceResolver resolver) {
        super(http, resolver, NodeType.OPENAPI.code());
    }
}

/**
 * Webhook 触发/回调节点执行处理器（WEBHOOK）。
 */
@Component
class WebhookNodeExecutionHandler extends AbstractHttpConnectorNodeExecutionHandler {
    WebhookNodeExecutionHandler(HttpConnectorInvocationAdapter http, VariableReferenceResolver resolver) {
        super(http, resolver, NodeType.WEBHOOK.code());
    }
}

/**
 * 内部 API 调用节点执行处理器（INTERNAL_API）。
 */
@Component
class InternalApiNodeExecutionHandler extends AbstractHttpConnectorNodeExecutionHandler {
    InternalApiNodeExecutionHandler(HttpConnectorInvocationAdapter http, VariableReferenceResolver resolver) {
        super(http, resolver, NodeType.INTERNAL_API.code());
    }
}

/**
 * 网页爬虫节点执行处理器（WEB_CRAWLER）。
 */
@Component
class WebCrawlerNodeExecutionHandler extends AbstractHttpConnectorNodeExecutionHandler {
    WebCrawlerNodeExecutionHandler(HttpConnectorInvocationAdapter http, VariableReferenceResolver resolver) {
        super(http, resolver, NodeType.WEB_CRAWLER.code());
    }
}

/**
 * MCP 协议工具节点执行处理器（MCP）。
 */
@Component
class McpNodeExecutionHandler extends AbstractConnectorNodeExecutionHandler {

    /** MCP 协议适配器 */
    private final McpConnectorInvocationAdapter mcp;

    /**
     * 构造函数注入依赖。
     */
    McpNodeExecutionHandler(McpConnectorInvocationAdapter mcp, VariableReferenceResolver resolver) {
        super(resolver, NodeType.MCP.code());
        this.mcp = mcp;
    }

    /**
     * 执行 MCP 协议工具调用。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return result(request, mcp.invoke(request(request)));
    }
}

/**
 * 沙箱代码脚本执行节点处理器（CODE - Python/JS）。
 */
@Component
class CodeNodeExecutionHandler extends AbstractConnectorNodeExecutionHandler {

    /** 代码执行服务路径 */
    private static final String CODE_EXECUTION_PATH = "/execute";

    /** HTTP 适配器 */
    private final HttpConnectorInvocationAdapter http;

    /** JSON 映射组件 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖。
     */
    CodeNodeExecutionHandler(HttpConnectorInvocationAdapter http,
                             ObjectMapper objectMapper,
                             VariableReferenceResolver resolver) {
        super(resolver, NodeType.CODE.code());
        this.http = http;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行沙箱代码节点逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        CodeExecutionInput input = new CodeExecutionInput(
                request.config().path("language").asText("python"),
                request.config().path("code").asText(""),
                request.inputs().values()
        );
        ConnectorInvocationPort.InvocationRequest invocation = new ConnectorInvocationPort.InvocationRequest(
                request.tenantId(),
                request.config().path("connectorId").asLong(0),
                DEFAULT_HTTP_METHOD,
                CODE_EXECUTION_PATH,
                Map.of(),
                write(input),
                request.config().path("idempotencyKey").asText(null),
                true
        );
        return result(request, http.invoke(invocation));
    }

    /** 序列化请求体 */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("代码节点输入参数序列化 JSON 失败。", exception);
        }
    }

    /** 代码执行输入参数 Record */
    private record CodeExecutionInput(String language, String code, Map<String, Object> inputVariables) {
    }
}

