package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SysRoleMenu 数据库持久化实体对象。
 * 对应数据库中 SysRoleMenu 数据表的字段结构映射。
 */
@Data
@TableName("sys_role_menu")
/**
 * 数据库实体：映射表 `sys_role_menu`，承载 SysRoleMenu 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysRoleMenu 数据表持久化实体类。
 * 映射数据库对应的 SysRoleMenu 表结构。
 */
public class SysRoleMenuEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** role 主键 ID 标识 属性 */
    private Long roleId;
    /** menu 主键 ID 标识 属性 */
    private Long menuId;
}
