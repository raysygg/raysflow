package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TenantBillingQuota 数据库持久化实体对象。
 * 对应数据库中 TenantBillingQuota 数据表的字段结构映射。
 */
@Data
@TableName("tenant_billing_quota")
/**
 * 数据库实体：映射表 `tenant_billing_quota`，承载 TenantBillingQuota 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * TenantBillingQuota 数据表持久化实体类。
 * 映射数据库对应的 TenantBillingQuota 表结构。
 */
public class TenantBillingQuotaEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** monthly Token Limit 属性 */
    private Long monthlyTokenLimit;
    /** monthly Token Used 属性 */
    private Long monthlyTokenUsed;
    /** monthly Workflow Limit 属性 */
    private Integer monthlyWorkflowLimit;
    /** monthly Workflow Used 属性 */
    private Integer monthlyWorkflowUsed;
    /** storage Limit Mb 属性 */
    private Integer storageLimitMb;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
