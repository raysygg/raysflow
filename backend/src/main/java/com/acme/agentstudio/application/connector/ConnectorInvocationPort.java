package com.acme.agentstudio.application.connector;

import java.util.Map;

/**
 * 连接器工具统一调用端口（Connector Invocation Port）。
 * 端口定义了底层不同传输协议与服务适配器（如 REST/HTTP 适配器、MCP Model Context Protocol 适配器）的标准化调用接口契约。
 */
public interface ConnectorInvocationPort {

    /**
     * 发起一次连接器工具的同步/异步协议调用。
     *
     * @param request 标准化调用请求参数（含认证 Token、Headers、Body、幂等 Key 等）
     * @param <InvocationResult> 调用响应结果（状态码、响应体、延迟毫秒、重试次数）
     * @return 调用结果
     */
    InvocationResult invoke(InvocationRequest request);

    /** 连接器调用请求传输契约 */
    record InvocationRequest(Long tenantId, Long connectorId, String method, String path, Map<String, String> headers,
                             String body, String idempotencyKey, boolean verifySsl) {}

    /** 连接器调用响应结果传输契约 */
    record InvocationResult(int statusCode, String body, long latencyMs, int attempts, boolean retriable, String error) {}
}

