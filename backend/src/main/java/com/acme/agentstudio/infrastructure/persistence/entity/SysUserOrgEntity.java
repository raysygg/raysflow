package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysUserOrg 数据库持久化实体对象。
 * 对应数据库中 SysUserOrg 数据表的字段结构映射。
 */
@Data
@TableName("sys_user_org")
/**
 * 数据库实体：映射表 `sys_user_org`，承载 SysUserOrg 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysUserOrg 数据表持久化实体类。
 * 映射数据库对应的 SysUserOrg 表结构。
 */
public class SysUserOrgEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** user 主键 ID 标识 属性 */
    private Long userId;
    /** org Unit 主键 ID 标识 属性 */
    private Long orgUnitId;
    /** is Primary 属性 */
    private Integer isPrimary;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
