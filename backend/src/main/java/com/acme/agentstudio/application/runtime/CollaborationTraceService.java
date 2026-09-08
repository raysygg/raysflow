package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.CollaborationTraceEntry;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多 Agent 协作轨迹持久化服务（Collaboration Trace Service）。
 * 负责将多智能体协作明细（包含子 Agent 消息传递、Token 计费、工具调用及共享状态变更）记录并写入平台统一的执行事件流中。
 */
@Service
public class CollaborationTraceService {

    /** 协作 Trace 事件类型标识 */
    private static final String EVENT_COLLABORATION_TRACE = "COLLABORATION_TRACE";

    /** 平台执行事件 Mapper */
    private final PlatformExecutionEventMapper eventMapper;

    /** Jackson JSON 映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖项目。
     *
     * @param eventMapper 执行事件 Mapper
     * @param objectMapper JSON 映射工具
     */
    public CollaborationTraceService(PlatformExecutionEventMapper eventMapper, ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 将协作明细条目追加写入平台的持久化执行事件流。
     *
     * @param tenantId 租户 ID
     * @param sequence 事件逻辑序号
     * @param entry 协作轨迹条目 CollaborationTraceEntry
     */
    public void append(long tenantId, long sequence, CollaborationTraceEntry entry) {
        if (tenantId <= 0 || entry == null) {
            throw new IllegalArgumentException("租户 ID 与协作 Trace 记录实体不能为空。");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentId", entry.agentId());
        payload.put("parentAgentId", entry.parentAgentId());
        payload.put("messageId", entry.messageId());
        payload.put("tokenCost", entry.tokenCost());
        payload.put("costMicros", entry.costMicros());
        payload.put("toolId", entry.toolId());
        payload.put("sharedStateChanges", entry.sharedStateChanges());

        PlatformExecutionEventEntity event = new PlatformExecutionEventEntity();
        event.setTenantId(tenantId);
        event.setExecutionId(entry.runId());
        event.setEventType(EVENT_COLLABORATION_TRACE);
        event.setSequenceNo(sequence);
        event.setPayloadJson(write(payload));
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
    }

    /** 将对象转换为 JSON 字符串 */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("多 Agent 协作 Trace 明细 JSON 序列化失败。", exception);
        }
    }
}

