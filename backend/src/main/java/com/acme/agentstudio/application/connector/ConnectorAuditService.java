package com.acme.agentstudio.application.connector;

import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolInvocationEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolInvocationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 外部 API / MCP 工具连接器调用链路与性能耗时审计日志记录服务。
 * 负责在工作流执行或 Agent Tool Calling 节点调用前后记录请求入参摘要、响应出参、HTTP 状态码、网络延迟耗时及重试次数。
 */
@Service
public class ConnectorAuditService {

    /** 工具调用日志持久化 Mapper */
    private final PlatformToolInvocationMapper mapper;

    /**
     * 构造函数注入持久化 Mapper 依赖。
     */
    public ConnectorAuditService(PlatformToolInvocationMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录一次工具/API 连接器调用的入参、出参摘要、响应码、耗时与重试次数。
     *
     * @param tenantId 租户 ID
     * @param connectorId 连接器 ID
     * @param executionId 工作流执行实例 ID
     * @param request 请求入参序列化摘要
     * @param response 响应出参序列化摘要
     * @param status HTTP/协议响应码
     * @param result 结果状态（SUCCESS / FAILED）
     * @param latency 调用耗时毫秒
     * @param attempts 重试尝试次数
     * @param error 发生的异常错误明细
     */
    public void record(Long tenantId, Long connectorId, String executionId, String request, String response, Integer status,
                       String result, long latency, int attempts, String error) {
        PlatformToolInvocationEntity entity = new PlatformToolInvocationEntity();
        entity.setTenantId(tenantId);
        entity.setConnectorId(connectorId);
        entity.setExecutionId(executionId);
        entity.setRequestSummary(limit(request));
        entity.setResponseSummary(limit(response));
        entity.setStatusCode(status);
        entity.setResultStatus(result);
        entity.setLatencyMs(latency);
        entity.setAttemptCount(attempts);
        entity.setErrorMessage(limit(error));
        entity.setCreatedAt(LocalDateTime.now());
        mapper.insert(entity);
    }

    /**
     * 限制摘要字符串长度在 2000 字符以内防止数据库字段溢出报错。
     */
    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}

