package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysUser 数据库持久化实体对象。
 * 对应数据库中 SysUser 数据表的字段结构映射。
 */
@Data
@TableName("sys_user")
/**
 * 数据库实体：映射表 `sys_user`，承载 SysUser 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysUser 数据表持久化实体类。
 * 映射数据库对应的 SysUser 表结构。
 */
public class SysUserEntity {
    @TableId(type = IdType.AUTO)
    /** 用户主键。 */
    private Long id;
    /** 用户所属租户主键，用于所有业务查询的租户隔离。 */
    private Long tenantId;
    /** 登录用户名。 */
    private String username;
    /** BCrypt 密码摘要，禁止保存明文密码。 */
    private String password;
    /** 用户显示昵称。 */
    private String nickname;
    /** 用户邮箱。 */
    private String email;
    /** 用户手机号。 */
    private String phone;
    /** 直属上级用户主键，可为空。 */
    private Long managerUserId;
    /** 用户状态，例如 ACTIVE、DISABLED。 */
    private String status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最近更新时间。 */
    private LocalDateTime updatedAt;
}
