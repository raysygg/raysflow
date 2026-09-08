package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysMenu 数据库持久化实体对象。
 * 对应数据库中 SysMenu 数据表的字段结构映射。
 */
@Data
@TableName("sys_menu")
/**
 * 数据库实体：映射表 `sys_menu`，承载 SysMenu 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysMenu 数据表持久化实体类。
 * 映射数据库对应的 SysMenu 表结构。
 */
public class SysMenuEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** parent 主键 ID 标识 属性 */
    private Long parentId;
    /** menu 展示名称 属性 */
    private String menuName;
    /** path 属性 */
    private String path;
    /** component 属性 */
    private String component;
    /** perms 属性 */
    private String perms;
    /** menu 业务分类类型 属性 */
    private String menuType;
    /** icon 属性 */
    private String icon;
    /** sort Order 属性 */
    private Integer sortOrder;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
