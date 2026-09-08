package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysOrgUnit 数据库持久化实体对象。
 * 对应数据库中 SysOrgUnit 数据表的字段结构映射。
 */
@Data
@TableName("sys_org_unit")
/**
 * 数据库实体：映射表 `sys_org_unit`，承载 SysOrgUnit 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysOrgUnit 数据表持久化实体类。
 * 映射数据库对应的 SysOrgUnit 表结构。
 */
public class SysOrgUnitEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** parent 主键 ID 标识 属性 */
    private Long parentId;
    /** org 业务编码 属性 */
    private String orgCode;
    /** org 展示名称 属性 */
    private String orgName;
    /** org 业务分类类型 属性 */
    private String orgType;
    /** leader User 主键 ID 标识 属性 */
    private Long leaderUserId;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** sort Order 属性 */
    private Integer sortOrder;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
