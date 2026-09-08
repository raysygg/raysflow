package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * SaasUsageAdjustment 数据库持久化实体对象。
 * 对应数据库中 SaasUsageAdjustment 数据表的字段结构映射。
 */
@Data @TableName("saas_usage_adjustment")
/**
 * SaasUsageAdjustment 数据表持久化实体类。
 * 映射数据库对应的 SaasUsageAdjustment 表结构。
 */
public class SaasUsageAdjustmentEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long usageEventId;
    private String adjustmentType; private BigDecimal quantity; private BigDecimal amount; private String currency;
    private String reason; private String idempotencyKey; private Long createdBy; private LocalDateTime createdAt;
}
