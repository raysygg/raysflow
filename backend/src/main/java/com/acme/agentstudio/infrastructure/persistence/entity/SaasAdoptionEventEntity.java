package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SaasAdoptionEvent 数据库持久化实体对象。
 * 对应数据库中 SaasAdoptionEvent 数据表的字段结构映射。
 */
@Data
@TableName("saas_adoption_event")
/**
 * SaasAdoptionEvent 数据表持久化实体类。
 * 映射数据库对应的 SaasAdoptionEvent 表结构。
 */
public class SaasAdoptionEventEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long applicationId; private String releaseId; private String runId;
    private String eventType; private Integer schemaVersion; private String eventSource; private String idempotencyKey;
    private String propertiesJson; private LocalDateTime occurredAt; private LocalDateTime createdAt;
}
