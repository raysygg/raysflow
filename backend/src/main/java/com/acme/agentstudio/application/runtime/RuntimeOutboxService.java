package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeOutboxEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeOutboxEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 运行时事务发件箱事件持久化服务（Runtime Outbox Service）。
 * 实现 Transactional Outbox 模式，在本地业务事务中原子写入事件记录（RuntimeOutboxEventEntity，状态 PENDING），
 * 解决分布式异步 Task 或远程 MQ 调度时事务不一致导致的“只依赖内存事件丢失”风险。
 */
@Service
public class RuntimeOutboxService {

    /** 事务发件箱事件 Mapper */
    private final RuntimeOutboxEventMapper outboxMapper;

    /**
     * 构造函数注入依赖 Outbox Mapper。
     *
     * @param outboxMapper 发件箱事件 Mapper
     */
    public RuntimeOutboxService(RuntimeOutboxEventMapper outboxMapper) {
        this.outboxMapper = outboxMapper;
    }

    /**
     * 追加写入一条 PENDING 状态的事务发件箱事件记录。
     *
     * @param tenantId 租户 ID
     * @param aggregateType 聚合根类型（如 "RUN", "TASK"）
     * @param aggregateId 聚合根 ID
     * @param eventType 事件类型字符串
     * @param payloadJson 事件 JSON 载荷
     * @return 持久化成功的发件箱实体 RuntimeOutboxEventEntity
     */
    public RuntimeOutboxEventEntity append(
            Long tenantId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson
    ) {
        RuntimeOutboxEventEntity event = new RuntimeOutboxEventEntity();
        event.setTenantId(tenantId);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayloadJson(payloadJson);
        event.setOutboxStatus("PENDING");
        event.setRetryCount(0);
        event.setAvailableAt(LocalDateTime.now());
        event.setCreatedAt(LocalDateTime.now());

        outboxMapper.insert(event);
        return event;
    }
}

