package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeOutboxEvent 数据库持久化实体对象。
 * 对应数据库中 RuntimeOutboxEvent 数据表的字段结构映射。
 */
@Data
@TableName("runtime_outbox_event")
/**
 * RuntimeOutboxEvent 数据表持久化实体类。
 * 映射数据库对应的 RuntimeOutboxEvent 表结构。
 */
public class RuntimeOutboxEventEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private String aggregateType; private String aggregateId; private String eventType;
    private String payloadJson; private String outboxStatus; private Integer retryCount;
    private LocalDateTime availableAt; private LocalDateTime sentAt; private LocalDateTime createdAt;
}
