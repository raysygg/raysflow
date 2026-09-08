package com.acme.agentstudio.application.connector;

import com.acme.agentstudio.application.saas.TenantEntitlementService;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 持久化工具调用的统一应用适配器（Unified Tool Connector Adapter）。
 * 作为工作流 Execution 引擎节点与底座协议适配器（HTTP / MCP）之间的统一代理入口，负责按 ConnectorType 路由至具体协议实现并统一序列化入参。
 */
@Component
public class UnifiedToolConnectorAdapter implements ToolConnector {

    /** 连接器与加密凭据资源服务 */
    private final ConnectorResourceService resources;

    /** HTTP REST API 适配器 */
    private final HttpConnectorInvocationAdapter http;

    /** MCP 协议适配器 */
    private final McpConnectorInvocationAdapter mcp;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /** 统一租户权益准入服务 */
    private final TenantEntitlementService entitlementService;

    /**
     * 构造函数注入资源与底座协议适配器。
     */
    public UnifiedToolConnectorAdapter(ConnectorResourceService resources,
                                       HttpConnectorInvocationAdapter http,
                                       McpConnectorInvocationAdapter mcp,
                                       ObjectMapper objectMapper,
                                       TenantEntitlementService entitlementService) {
        this.resources = resources;
        this.http = http;
        this.mcp = mcp;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    /**
     * 获取统一适配器的类型标识符（UNIFIED）。
     *
     * @return "UNIFIED"
     */
    @Override
    public String type() {
        return "UNIFIED";
    }

    /**
     * 统一入口：按租户与连接器配置自动路由至 MCP 或 HTTP 协议适配器发起调用。
     *
     * @param request 高阶连接器请求
     * @return 统一高阶连接器执行结果
     */
    @Override
    public ConnectorResult invoke(ConnectorRequest request) {
        var admission = entitlementService.admitForConsumption(new AdmissionRequest(
                request.tenantId(), null, "CONNECTOR_CALL", 1,
                "连接器调用", request.idempotencyKey()), true);
        if (admission.decision() == AdmissionDecision.DENY) {
            throw new IllegalStateException(admission.reason() + " " + admission.remediation());
        }
        ConnectorResourceService.ConnectorResource resource = resources.resolve(request.tenantId(), request.connectorId());
        Map<String, JsonNode> arguments = request.arguments() == null ? Map.of() : request.arguments();
        String body;
        try {
            body = objectMapper.writeValueAsString(new LinkedHashMap<>(arguments));
        } catch (Exception ex) {
            throw new IllegalArgumentException("工具参数无法正常序列化为 JSON 字符串。", ex);
        }

        ConnectorInvocationPort.InvocationRequest invocation = new ConnectorInvocationPort.InvocationRequest(
                request.tenantId(), request.connectorId(), "POST", null, Map.of(), body,
                request.idempotencyKey(), true);

        ConnectorInvocationPort.InvocationResult result = "MCP".equals(resource.connector().getConnectorType())
                ? mcp.invoke(invocation)
                : http.invoke(invocation);

        return new ConnectorResult(
                result.statusCode() >= 200 && result.statusCode() < 300,
                parse(result.body()),
                result.error(),
                result.error(),
                result.latencyMs()
        );
    }

    /**
     * 解析字符串为 JsonNode 对象（若无法解析为 JSON 则封装为 TextNode）。
     */
    private JsonNode parse(String body) {
        if (body == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return objectMapper.getNodeFactory().textNode(body);
        }
    }
}

