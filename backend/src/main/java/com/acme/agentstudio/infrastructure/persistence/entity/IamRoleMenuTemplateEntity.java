package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IamRoleMenuTemplate 数据库持久化实体对象。
 * 对应数据库中 IamRoleMenuTemplate 数据表的字段结构映射。
 */
@Data
@TableName("iam_role_menu_template")
/**
 * 数据库实体：映射表 `iam_role_menu_template`，承载 IamRoleMenuTemplate 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * IamRoleMenuTemplate 数据表持久化实体类。
 * 映射数据库对应的 IamRoleMenuTemplate 表结构。
 */
public class IamRoleMenuTemplateEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** role 业务编码 属性 */
    private String roleCode;
    /** menu 主键 ID 标识 属性 */
    private Long menuId;
    /** sort Order 属性 */
    private Integer sortOrder;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
