package com.acme.agentstudio.domain.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

/**
 * 跨工作流节点传递的标准执行上下文 Record（Execution Context）。
 * 封装单次运行的核心标识、租户边界、幂等键、运行变量映射 Map、启动时间戳及重试计数，支持事件持久化与断线恢复。
 *
 * @param executionId 执行唯一标识字符串
 * @param tenantId 租户 ID
 * @param appType 应用类型标识
 * @param versionId 绑定的发布版本 ID
 * @param idempotencyKey 幂等校验 Key
 * @param variables 变量节点 Map 容器
 * @param startedAt 执行开始时间戳
 * @param attempt 重试尝试次数
 */
public record ExecutionContext(
        String executionId,
        Long tenantId,
        String appType,
        String versionId,
        String idempotencyKey,
        Map<String, JsonNode> variables,
        Instant startedAt,
        int attempt
) {
    /** 紧凑构造方法校验 */
    public ExecutionContext {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("工作流执行标识 executionId 不能为空。");
        }
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("租户标识 tenantId 不能为空。");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("工作流执行幂等键 idempotencyKey 不能为空。");
        }

        variables = (variables == null) ? Map.of() : Map.copyOf(variables);
        startedAt = (startedAt == null) ? Instant.now() : startedAt;
    }
}

