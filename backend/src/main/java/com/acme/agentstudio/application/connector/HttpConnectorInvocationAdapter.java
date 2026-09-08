package com.acme.agentstudio.application.connector;

import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * 通用 HTTP REST API 工具连接器调用适配器（HttpConnectorInvocationAdapter）。
 * 实现了 ConnectorInvocationPort 接口，负责发起 HTTP/HTTPS 协议调用、处理超时重试策略、封装 Idempotency-Key 及记录链路性能审计。
 */
@Component
public class HttpConnectorInvocationAdapter implements ConnectorInvocationPort {

    /** 连接器与凭据资源解析服务 */
    private final ConnectorResourceService resources;

    /** 连接器调用审计服务 */
    private final ConnectorAuditService audit;

    /** JSON 解析映射组件 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数注入资源与审计服务。
     */
    public HttpConnectorInvocationAdapter(ConnectorResourceService resources, ConnectorAuditService audit) {
        this.resources = resources;
        this.audit = audit;
    }

    /**
     * 执行真正的 HTTP / REST API 协议远程调用。
     *
     * @param request 标准化调用请求参数
     * @return 包含响应码、Body 与耗时的 InvocationResult 对象
     */
    @Override
    public InvocationResult invoke(InvocationRequest request) {
        PlatformToolConnectorEntity connector = resources.resolve(request.tenantId(), request.connectorId()).connector();
        if (!request.verifySsl()) {
            throw new IllegalArgumentException("工作流节点出于安全考虑，不允许关闭 SSL 证书校验。");
        }
        if (!Set.of("HTTP", "OPENAPI", "WEBHOOK", "INTERNAL_API", "WEB_CRAWLER", "CODE_SANDBOX").contains(connector.getConnectorType())) {
            throw new IllegalArgumentException("该连接器类型需要使用其他专用的协议适配器。");
        }
        if (connector.getEndpoint() == null || connector.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("连接器未配置目标 Endpoint 访问地址。");
        }

        String target = (request.path() == null || request.path().isBlank())
                ? connector.getEndpoint()
                : connector.getEndpoint() + request.path();

        int maxAttempts = retryAttempts(connector.getRetryPolicyJson(), request.idempotencyKey());
        int attempts = 0;
        long started = System.nanoTime();
        String error = null;

        while (attempts++ < maxAttempts) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(target))
                        .timeout(Duration.ofMillis(connector.getTimeoutMs()))
                        .method(
                                request.method() == null ? "GET" : request.method().toUpperCase(),
                                HttpRequest.BodyPublishers.ofString(request.body() == null ? "" : request.body())
                        );

                if (request.headers() != null) {
                    request.headers().forEach(builder::header);
                }
                if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
                    builder.header("Idempotency-Key", request.idempotencyKey());
                }

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connector.getTimeoutMs()))
                        .build();

                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

                boolean retriable = response.statusCode() == 408 || response.statusCode() == 429 || response.statusCode() >= 500;
                if (!retriable || attempts >= maxAttempts) {
                    InvocationResult result = new InvocationResult(
                            response.statusCode(),
                            response.body(),
                            elapsed(started),
                            attempts,
                            retriable,
                            null
                    );
                    audit.record(
                            request.tenantId(),
                            connector.getId(),
                            null,
                            request.body(),
                            response.body(),
                            response.statusCode(),
                            retriable ? "RETRY_EXHAUSTED" : "SUCCEEDED",
                            result.latencyMs(),
                            attempts,
                            null
                    );
                    return result;
                }
            } catch (Exception ex) {
                error = ex.getMessage();
                if (attempts >= maxAttempts) {
                    InvocationResult result = new InvocationResult(0, null, elapsed(started), attempts, true, error);
                    audit.record(
                            request.tenantId(),
                            connector.getId(),
                            null,
                            request.body(),
                            null,
                            null,
                            "FAILED",
                            result.latencyMs(),
                            attempts,
                            error
                    );
                    return result;
                }
            }
        }
        return new InvocationResult(0, null, elapsed(started), attempts - 1, true, error);
    }

    /**
     * 从重试策略 JSON 解析最大重试次数（仅当存在幂等 Key 时才启用重试）。
     */
    private int retryAttempts(String policy, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return 1;
        }
        if (policy == null || policy.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(5, objectMapper.readTree(policy).path("maxAttempts").asInt(1)));
        } catch (Exception ex) {
            return 1;
        }
    }

    /**
     * 计算自起点 started 算起的耗时毫秒数。
     */
    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}

