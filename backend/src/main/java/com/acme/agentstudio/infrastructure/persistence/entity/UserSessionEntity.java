package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * UserSession 数据库持久化实体对象。
 * 对应数据库中 UserSession 数据表的字段结构映射。
 */
@Data
@TableName("user_session")
/**
 * 数据库实体：映射表 `user_session`，承载 UserSession 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * UserSession 数据表持久化实体类。
 * 映射数据库对应的 UserSession 表结构。
 */
public class UserSessionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    /** 会话所属租户和用户。 */
    private Long tenantId; private Long userId;
    /** 刷新令牌 SHA-256 摘要，不保存令牌明文。 */
    private String refreshTokenHash;
    /** 设备标识和客户端信息，用于会话管理和安全审计。 */
    private String deviceId; private String ipAddress; private String userAgent;
    /** ACTIVE 或 REVOKED。 */
    private String status;
    private LocalDateTime expiresAt; private LocalDateTime createdAt; private LocalDateTime lastUsedAt; private LocalDateTime revokedAt;
}
