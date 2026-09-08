package com.acme.agentstudio.application.security;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.security.model.UserSessionSummary;
import com.acme.agentstudio.infrastructure.persistence.entity.UserSessionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.UserSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 用户多设备登录会话（User Device Session）全生命周期管理服务。
 * 负责在登录时保存 Refresh Token 的 SHA-256 哈希摘要（物理隔离明文）、处理多设备并发会话隔离、令牌轮转（Token Rotation）以及单点/全端强制下线吊销（Revocation）。
 */
@Service
public class UserSessionService {

    /** 用户设备会话 Persistence Mapper */
    private final UserSessionMapper mapper;

    /**
     * 构造函数注入会话 Mapper 组件。
     */
    public UserSessionService(UserSessionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 创建并持久化用户登录设备会话记录（安全存储 SHA-256 哈希摘要，绝不上库 Token 明文）。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param token 刷新令牌明文
     * @param deviceId 客户端设备唯一标识
     * @param ip 客户端登录 IP 地址
     * @param userAgent 客户端浏览器/应用 User-Agent 标头
     */
    public void create(Long tenantId, Long userId, String token, String deviceId, String ip, String userAgent) {
        UserSessionEntity session = new UserSessionEntity();
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setRefreshTokenHash(hash(token));
        session.setDeviceId(deviceId);
        session.setIpAddress(ip);
        session.setUserAgent(userAgent);
        session.setStatus(BusinessStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastUsedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        mapper.insert(session);
    }

    /**
     * 校验刷新令牌对应的设备会话必须处于 ACTIVE 活动状态且未超出 7 天有效期。
     *
     * @param token 刷新令牌明文
     * @return 活动中的设备会话实体对象
     */
    public UserSessionEntity requireActive(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少刷新令牌，请重新登录。");
        }
        UserSessionEntity session = mapper.selectOne(new LambdaQueryWrapper<UserSessionEntity>()
                .eq(UserSessionEntity::getRefreshTokenHash, hash(token))
                .eq(UserSessionEntity::getStatus, BusinessStatus.ACTIVE));
        if (session == null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("刷新令牌已吊销或已过有效期，请重新登录账户。");
        }
        return session;
    }

    /**
     * 轮转/滚动更新刷新令牌摘要，并刷新最近使用时间（Token Rotation 防重放）。
     *
     * @param session 活动中的设备会话实体对象
     * @param newToken 签发给客户端的全新刷新令牌
     */
    public void rotate(UserSessionEntity session, String newToken) {
        session.setRefreshTokenHash(hash(newToken));
        session.setLastUsedAt(LocalDateTime.now());
        mapper.updateById(session);
    }

    /**
     * 根据特定刷新令牌主动吊销下线对应的设备会话。
     *
     * @param token 待吊销下线的刷新令牌
     */
    public void revoke(String token) {
        mapper.update(null, Wrappers.<UserSessionEntity>lambdaUpdate()
                .eq(UserSessionEntity::getRefreshTokenHash, hash(token))
                .set(UserSessionEntity::getStatus, "REVOKED")
                .set(UserSessionEntity::getRevokedAt, LocalDateTime.now()));
    }

    /**
     * 查询指定用户的所有登录设备会话摘要列表（按最近活跃时间倒序）。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @return 设备会话摘要列表
     */
    public List<UserSessionSummary> list(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            throw new IllegalArgumentException("查询用户会话列表时租户与用户标识不能为空。");
        }
        return mapper.selectList(new LambdaQueryWrapper<UserSessionEntity>()
                        .eq(UserSessionEntity::getTenantId, tenantId)
                        .eq(UserSessionEntity::getUserId, userId)
                        .orderByDesc(UserSessionEntity::getLastUsedAt))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 强行下线并吊销指定的单个设备会话（如在设备安全中心点击“下线此设备”）。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param sessionId 待下线的会话记录 ID
     */
    public void revoke(Long tenantId, Long userId, Long sessionId) {
        int updated = mapper.update(null, Wrappers.<UserSessionEntity>lambdaUpdate()
                .eq(UserSessionEntity::getId, sessionId)
                .eq(UserSessionEntity::getTenantId, tenantId)
                .eq(UserSessionEntity::getUserId, userId)
                .eq(UserSessionEntity::getStatus, BusinessStatus.ACTIVE)
                .set(UserSessionEntity::getStatus, "REVOKED")
                .set(UserSessionEntity::getRevokedAt, LocalDateTime.now()));
        if (updated == 0) {
            throw new IllegalArgumentException("指定的设备会话不存在、已处于下线状态或不属于当前用户。");
        }
    }

    /**
     * 一键下线吊销当前用户在全平台所有设备上的活动登录会话。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @return 被强制吊销下线的会话条数
     */
    public int revokeAll(Long tenantId, Long userId) {
        return mapper.update(null, Wrappers.<UserSessionEntity>lambdaUpdate()
                .eq(UserSessionEntity::getTenantId, tenantId)
                .eq(UserSessionEntity::getUserId, userId)
                .eq(UserSessionEntity::getStatus, BusinessStatus.ACTIVE)
                .set(UserSessionEntity::getStatus, "REVOKED")
                .set(UserSessionEntity::getRevokedAt, LocalDateTime.now()));
    }

    /**
     * 转化实体为 UserSessionSummary 契约结构。
     */
    private UserSessionSummary toSummary(UserSessionEntity session) {
        return new UserSessionSummary(
                session.getId(),
                session.getDeviceId(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getStatus(),
                session.getExpiresAt(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getRevokedAt()
        );
    }

    /**
     * 计算字符串的 SHA-256 Hex 哈希摘要。
     */
    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("刷新令牌 SHA-256 哈希摘要计算失败。", e);
        }
    }
}

