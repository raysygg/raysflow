package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SaasEntitlement 数据库持久化实体对象。
 * 对应数据库中 SaasEntitlement 数据表的字段结构映射。
 */
@Data
@TableName("saas_entitlement")
/**
 * SaasEntitlement 数据表持久化实体类。
 * 映射数据库对应的 SaasEntitlement 表结构。
 */
public class SaasEntitlementEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long subscriptionId; private String featureCode;
    private Long hardLimit; private Long softLimit; private String unit; private String overagePolicy;
    private LocalDateTime resetAt; private Integer versionNo; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
