package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SaasBillingPeriod 数据库持久化实体对象。
 * 对应数据库中 SaasBillingPeriod 数据表的字段结构映射。
 */
@Data
@TableName("saas_billing_period")
/**
 * SaasBillingPeriod 数据表持久化实体类。
 * 映射数据库对应的 SaasBillingPeriod 表结构。
 */
public class SaasBillingPeriodEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private LocalDate periodStart; private LocalDate periodEnd; private String currency;
    private String periodStatus; private LocalDateTime finalizedAt; private LocalDateTime createdAt;
}
