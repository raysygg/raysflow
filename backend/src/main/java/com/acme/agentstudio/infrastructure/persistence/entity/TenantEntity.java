package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tenant 数据库持久化实体对象。
 * 对应数据库中 Tenant 数据表的字段结构映射。
 */
@Data
@TableName("tenant")
/**
 * 数据库实体：映射表 `tenant`，承载 Tenant 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * Tenant 数据表持久化实体类。
 * 映射数据库对应的 Tenant 表结构。
 */
public class TenantEntity {

    @TableId(type = IdType.AUTO)
    /** 租户主键。 */
    private Long id;
    /** 租户登录编码，作为企业空间的稳定业务标识。 */
    private String tenantCode;
    /** 租户展示名称。 */
    private String tenantName;
    /** 当前订阅套餐编码，套餐定义来自数据库基础数据。 */
    private String planCode;
    /** 租户允许创建的用户数量上限。 */
    private Integer userLimit;
    /** 租户状态，例如 ACTIVE、FROZEN。 */
    private String status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
