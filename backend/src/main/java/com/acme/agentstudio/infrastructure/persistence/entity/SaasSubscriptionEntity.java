package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SaasSubscription 数据库持久化实体对象。
 * 对应数据库中 SaasSubscription 数据表的字段结构映射。
 */
@Data
@TableName("saas_subscription")
/**
 * SaasSubscription 数据表持久化实体类。
 * 映射数据库对应的 SaasSubscription 表结构。
 */
public class SaasSubscriptionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long planVersionId; private String status; private String overagePolicy;
    private LocalDate startsOn; private LocalDate endsOn; private LocalDate billingAnchor;
    private String externalReference; private Long createdBy; private LocalDateTime cancelledAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
