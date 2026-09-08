package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AuditLog 数据库持久化实体对象。
 * 对应数据库中 AuditLog 数据表的字段结构映射。
 */
@Data
@TableName("audit_log")
/**
 * 数据库实体：映射表 `audit_log`，承载 AuditLog 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * AuditLog 数据表持久化实体类。
 * 映射数据库对应的 AuditLog 表结构。
 */
public class AuditLogEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 当前操作人用户标识 */
    private String operatorId;
    /** action 业务分类类型 属性 */
    private String actionType;
    /** target 业务分类类型 属性 */
    private String targetType;
    /** target 主键 ID 标识 属性 */
    private String targetId;
    /** risk Level 属性 */
    private String riskLevel;
    /** detail Json 属性 */
    private String detailJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
