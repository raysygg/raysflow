package com.acme.agentstudio.application.connector;

import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anthropic Model Context Protocol (MCP) 上下文与工具协议连接器调用适配器。
 * 实现了 ConnectorInvocationPort 接口，负责将通用工具调用请求封装为符合 JSON-RPC 2.0 规范的 `tools/call` 请求并发送给 MCP 服务器。
 */
@Component
public class McpConnectorInvocationAdapter implements ConnectorInvocationPort {

    /** 连接器与凭据资源解析服务 */
    private final ConnectorResourceService resources;

    /** JSON 序列化映射组件 */
    private final ObjectMapper objectMapper;

    /** 连接器调用审计服务 */
    private final ConnectorAuditService audit;

    /**
     * 构造函数注入资源、序列化与审计组件。
     */
    public McpConnectorInvocationAdapter(ConnectorResourceService resources, ObjectMapper objectMapper, ConnectorAuditService audit) {
        this.resources = resources;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    /**
     * 发起 MCP 协议标准的 JSON-RPC 2.0 远程工具调用。
     *
     * @param request 标准化调用请求参数
     * @return 包含 JSON-RPC 响应体与耗时的 InvocationResult 对象
     */
    @Override
    public InvocationResult invoke(InvocationRequest request) {
        PlatformToolConnectorEntity connector = resources.resolve(request.tenantId(), request.connectorId()).connector();
        if (!"MCP".equals(connector.getConnectorType())) {
            throw new IllegalArgumentException("MCP 适配器仅支持 MCP 类型连接器。");
        }
        if (connector.getEndpoint() == null || connector.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("MCP 连接器未配置访问 Endpoint 地址。");
        }
        if (!request.verifySsl()) {
            throw new IllegalArgumentException("出于安全控制要求，不允许关闭 SSL 证书校验。");
        }

        long started = System.nanoTime();
        try {
            Map<String, Object> rpc = new LinkedHashMap<>();
            rpc.put("jsonrpc", "2.0");
            rpc.put("id", request.idempotencyKey() == null ? UUID.randomUUID().toString() : request.idempotencyKey());
            rpc.put("method", request.path() == null || request.path().isBlank() ? "tools/call" : request.path());
            rpc.put("params", request.body() == null ? Map.of() : objectMapper.readTree(request.body()));

            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(connector.getEndpoint()))
                    .timeout(Duration.ofMillis(connector.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(rpc)));

            HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());

            InvocationResult result = new InvocationResult(
                    response.statusCode(),
                    response.body(),
                    elapsed(started),
                    1,
                    response.statusCode() >= 500,
                    null
            );
            audit.record(
                    request.tenantId(),
                    connector.getId(),
                    null,
                    request.body(),
                    response.body(),
                    response.statusCode(),
                    response.statusCode() >= 400 ? "FAILED" : "SUCCEEDED",
                    result.latencyMs(),
                    1,
                    null
            );
            return result;
        } catch (Exception ex) {
            InvocationResult result = new InvocationResult(0, null, elapsed(started), 1, true, ex.getMessage());
            audit.record(
                    request.tenantId(),
                    connector.getId(),
                    null,
                    request.body(),
                    null,
                    null,
                    "FAILED",
                    result.latencyMs(),
                    1,
                    ex.getMessage()
            );
            return result;
        }
    }

    /**
     * 计算自起点 started 算起的耗时毫秒数。
     */
    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}

