package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasBillingSync 数据库持久化实体对象。
 * 对应数据库中 SaasBillingSync 数据表的字段结构映射。
 */
@Data @TableName("saas_billing_sync")
/**
 * SaasBillingSync 数据表持久化实体类。
 * 映射数据库对应的 SaasBillingSync 表结构。
 */
public class SaasBillingSyncEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long invoiceId;
    private String adapterType; private String idempotencyKey; private String syncStatus; private String externalReference;
    private String safeSummary; private Integer retryCount; private LocalDateTime nextRetryAt;
    private LocalDateTime confirmedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
