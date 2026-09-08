package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaasUsageEvent 数据库持久化实体对象。
 * 对应数据库中 SaasUsageEvent 数据表的字段结构映射。
 */
@Data
@TableName("saas_usage_event")
/**
 * SaasUsageEvent 数据表持久化实体类。
 * 映射数据库对应的 SaasUsageEvent 表结构。
 */
public class SaasUsageEventEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long applicationId; private String releaseId; private String runId;
    private String modelKey; private String featureCode; private String costCenter; private String idempotencyKey;
    private BigDecimal quantity; private String unit; private String usageSource; private Integer inputTokens; private Integer outputTokens;
    private Long priceVersionId; private String currency; private BigDecimal unitPrice; private BigDecimal costAmount;
    private String costStatus; private String costReason; private LocalDateTime occurredAt; private LocalDateTime createdAt;
}
