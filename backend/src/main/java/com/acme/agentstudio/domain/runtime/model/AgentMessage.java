package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * Agent 智能体之间拓扑通讯的标准传输消息实体 Record（Agent Message）。
 * 保留链路血缘 parentMessageId 与明确的收发 Agent 角色 (fromAgentId, toAgentId)，禁止通过全局共享状态隐式传输。
 *
 * @param messageId 消息唯一标识 ID
 * @param runId 关联的运行任务 Run ID
 * @param fromAgentId 发送方 Agent 标识编号
 * @param toAgentId 接收方 Agent 标识编号
 * @param messageType 消息类型
 * @param payload 携带的消息载荷 Payload Map
 * @param parentMessageId 父消息链路血缘 ID
 * @param createdAt 消息产生时间
 */
public record AgentMessage(
        String messageId,
        String runId,
        String fromAgentId,
        String toAgentId,
        String messageType,
        Map<String, Object> payload,
        String parentMessageId,
        Instant createdAt
) {
    /** 紧凑构造函数做消息属性强校验防空保护 */
    public AgentMessage {
        if (messageId == null || messageId.isBlank() || runId == null || runId.isBlank()
                || fromAgentId == null || fromAgentId.isBlank() || toAgentId == null || toAgentId.isBlank()) {
            throw new IllegalArgumentException("Agent 消息标识、Run 和收发角色不能为空");
        }
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("Agent 消息类型不能为空");
        }
        payload = (payload == null) ? Map.of() : Map.copyOf(payload);
        createdAt = (createdAt == null) ? Instant.now() : createdAt;
    }
}

