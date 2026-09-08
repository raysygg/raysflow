package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
/**
 * PlatformEventFact 数据库持久化实体对象。
 * 对应数据库中 PlatformEventFact 数据表的字段结构映射。
 */
@Data @TableName("platform_event_fact")
/**
 * 数据库实体：映射表 `platform_event_fact`，承载 PlatformEventFact 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformEventFact 数据表持久化实体类。
 * 映射数据库对应的 PlatformEventFact 表结构。
 */
public class PlatformEventFactEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private Long orgUnitId; private Long userId; private Long roleId;
    private Long agentId; private Long workflowId; private Long modelId; private String eventType; private String resultStatus;
    private Long tokenCount; private BigDecimal costAmount; private Long latencyMs; private LocalDateTime occurredAt; private String payloadJson;
}
