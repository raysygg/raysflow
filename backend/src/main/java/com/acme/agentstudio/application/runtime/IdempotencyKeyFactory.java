package com.acme.agentstudio.application.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 运行幂等键工厂工具类（Idempotency Key Factory）。
 * 基于给定的 Run 运行标识、外部 Side Effect 动作标识与尝试序号，生成稳定的 SHA-256 幂等 Digest Key，
 * 防止网络抖动重试或节点挂起恢复时触发重复的侧效应（如重复付款、重复发送通知等）。
 */
public final class IdempotencyKeyFactory {

    /** 散列 Hash 算法 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** 拼接分隔符 */
    private static final String KEY_SEPARATOR = ":";

    /**
     * 私有构造函数，防止实例化工具类。
     */
    private IdempotencyKeyFactory() {
    }

    /**
     * 根据运行标识、动作标识和尝试序号计算 SHA-256 散列幂等键字符串。
     *
     * @param runId 运行执行 ID
     * @param actionId 外部动作或 side-effect 标识 ID
     * @param attempt 重试尝试次数 (>= 1)
     * @return 导出的 64 位 Hex 形式幂等 Key 字符串
     */
    public static String create(String runId, String actionId, int attempt) {
        if (runId == null || runId.isBlank() || actionId == null || actionId.isBlank() || attempt < 1) {
            throw new IllegalArgumentException("生成幂等 Key 必须提供有效的 runId、actionId 以及大于零的 attempt 次数。");
        }
        String raw = runId + KEY_SEPARATOR + actionId + KEY_SEPARATOR + attempt;
        try {
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM)
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前系统环境不支持标准的 SHA-256 散列算法。", exception);
        }
    }
}

