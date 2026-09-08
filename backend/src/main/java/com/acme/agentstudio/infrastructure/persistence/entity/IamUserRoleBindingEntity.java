package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IamUserRoleBinding 数据库持久化实体对象。
 * 对应数据库中 IamUserRoleBinding 数据表的字段结构映射。
 */
@Data
@TableName("iam_user_role_binding")
/**
 * 数据库实体：映射表 `iam_user_role_binding`，承载 IamUserRoleBinding 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * IamUserRoleBinding 数据表持久化实体类。
 * 映射数据库对应的 IamUserRoleBinding 表结构。
 */
public class IamUserRoleBindingEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** user 主键 ID 标识 属性 */
    private Long userId;
    /** role 主键 ID 标识 属性 */
    private Long roleId;
    /** source 业务编码 属性 */
    private String sourceCode;
    /** is Primary 属性 */
    private Boolean isPrimary;
    /** valid From 属性 */
    private LocalDateTime validFrom;
    /** valid Until 属性 */
    private LocalDateTime validUntil;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
