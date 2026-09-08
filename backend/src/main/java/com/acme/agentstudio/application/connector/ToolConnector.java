package com.acme.agentstudio.application.connector;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;

/**
 * 统一工具连接器高阶执行端口（Tool Connector Interface）。
 * 定义各类型 Tool Connector（OpenAPI / HTTP / Webhook / MCP / Internal API）的通用类型标识获取与高层 invoke 抽象契约。
 */
public interface ToolConnector {

    /**
     * 获取当前工具连接器的协议类型编码（如 HTTP、OPENAPI、MCP 等）。
     *
     * @return 协议类型字符串
     */
    String type();

    /**
     * 执行具体的工具调用与参数解析。
     *
     * @param request 高阶连接器请求对象
     * @return 高阶连接器响应结果（包含 JsonNode 输出与异常码）
     */
    ConnectorResult invoke(ConnectorRequest request);

    /** 高阶工具连接器请求契约 Record */
    record ConnectorRequest(Long tenantId, Long connectorId, String idempotencyKey,
                            Map<String, JsonNode> arguments, Duration timeout) {
        public ConnectorRequest {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("租户不能为空");
            }
            if (connectorId == null || connectorId <= 0) {
                throw new IllegalArgumentException("连接器不能为空");
            }
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("工具调用幂等键不能为空");
            }
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        }
    }

    /** 高阶工具连接器响应契约 Record */
    record ConnectorResult(boolean success, JsonNode output, String errorCode, String errorMessage, long latencyMs) {}
}

