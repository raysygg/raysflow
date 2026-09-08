package com.acme.agentstudio.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防暴破限流组件。
 * 基于内存 ConcurrentHashMap 记录“用户名@租户编码”维度的连续登录失败次数。
 * 当连续失败达到设定的最大阈值（默认 5 次）时，锁定账号对应维度的登录尝试（默认锁定 15 分钟），防止密码字典爆破攻击。
 */
@Component
public class LoginRateLimiter {

    /** 允许的最大连续登录失败尝试次数 */
    private static final int MAX_ATTEMPTS = 5;

    /** 触发锁定后的持续时间（默认 15 分钟，单位：毫秒） */
    private static final long LOCK_TIME_DURATION_MS = 15 * 60 * 1000L;

    /** 保存每一个登录主体（Key 格式：username@tenantCode）的失败尝试与锁定信息 */
    private final ConcurrentHashMap<String, AttemptTracker> trackers = new ConcurrentHashMap<>();

    /**
     * 内部记录追踪器结构。
     */
    private static class AttemptTracker {
        /** 当前连续失败次数 */
        int attempts;
        /** 锁定截止的毫秒时间戳（0 表示未被锁定） */
        long lockTime;

        AttemptTracker(int attempts, long lockTime) {
            this.attempts = attempts;
            this.lockTime = lockTime;
        }
    }

    /**
     * 判断指定登录主体当前是否处于被锁定期。
     *
     * @param key 登录主体唯一 Key（username@tenantCode）
     * @return 若仍然处于锁定时间段内则返回 true；否则返回 false
     */
    public boolean isLocked(String key) {
        AttemptTracker tracker = trackers.get(key);
        if (tracker == null) {
            return false;
        }
        if (tracker.lockTime > 0) {
            if (System.currentTimeMillis() < tracker.lockTime) {
                return true;
            } else {
                // 锁定期结束后自动清理记录，允许用户重新尝试
                trackers.remove(key);
                return false;
            }
        }
        return false;
    }

    /**
     * 记录一次登录失败尝试；若达到连续失败上限，自动设置锁定截止时间。
     *
     * @param key 登录主体唯一 Key
     */
    public void loginFailed(String key) {
        trackers.compute(key, (k, tracker) -> {
            if (tracker == null) {
                return new AttemptTracker(1, 0);
            }
            tracker.attempts++;
            if (tracker.attempts >= MAX_ATTEMPTS) {
                tracker.lockTime = System.currentTimeMillis() + LOCK_TIME_DURATION_MS;
            }
            return tracker;
        });
    }

    /**
     * 登录成功后重置清除该主体的失败尝试计数。
     *
     * @param key 登录主体唯一 Key
     */
    public void loginSucceeded(String key) {
        trackers.remove(key);
    }
}

