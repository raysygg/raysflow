package com.acme.agentstudio.domain.security.model;

import java.time.LocalDateTime;

/**
 * 用户在线设备会话脱敏摘要 Record（User Session Summary）。
 * 包含会话主键 ID、设备标识 ID、登录 IP 地址、UserAgent、会话状态、到期时间、创建/最后使用/吊销时间，严格屏蔽凭证与刷新令牌。
 *
 * @param id 会话主键物理 ID
 * @param deviceId 客户端设备标识 ID
 * @param ipAddress 登录时的 IP 地址
 * @param userAgent 浏览器/客户端 UserAgent 字符串
 * @param status 会话当前状态
 * @param expiresAt 到期失效时间
 * @param createdAt 会话建立时间
 * @param lastUsedAt 最后一次活动时间
 * @param revokedAt 强制吊销注销时间
 */
public record UserSessionSummary(
        Long id,
        String deviceId,
        String ipAddress,
        String userAgent,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime revokedAt
) {
}

