package com.acme.agentstudio.domain.runtime.model;

/**
 * 跨会话长期记忆（Long-term Memory）自动提取、确认与保留策略 Record（Long-term Memory Policy）。
 * 包含是否自动写入 autoWrite、敏感记忆提取是否需要用户人工确认 requireConfirmationForSensitive、
 * 记忆提炼最小置信度阈值 minimumConfidence 以及保存天数 retentionDays。
 *
 * @param autoWrite 是否在对话结束时自动提取并持久化长期记忆
 * @param requireConfirmationForSensitive 涉及敏感偏好/个人隐私时是否要求二次人工确认
 * @param minimumConfidence 记忆提炼算法的最小置信度（0.0 ~ 1.0）
 * @param retentionDays 长期记忆有效保存天数
 */
public record LongTermMemoryPolicy(
        boolean autoWrite,
        boolean requireConfirmationForSensitive,
        double minimumConfidence,
        long retentionDays
) {
    /** 默认最小置信度阈值：0.8 */
    public static final double DEFAULT_MINIMUM_CONFIDENCE = 0.8D;

    /** 默认保存天数：365 天 */
    public static final long DEFAULT_RETENTION_DAYS = 365L;

    /** 紧凑构造函数做输入校验 */
    public LongTermMemoryPolicy {
        if (minimumConfidence < 0D || minimumConfidence > 1D || retentionDays < 1) {
            throw new IllegalArgumentException("长期记忆置信度或保留期限无效。");
        }
    }

    /**
     * 构建默认长期记忆管理策略。
     *
     * @return 默认的 LongTermMemoryPolicy 实例
     */
    public static LongTermMemoryPolicy defaults() {
        return new LongTermMemoryPolicy(false, true, DEFAULT_MINIMUM_CONFIDENCE, DEFAULT_RETENTION_DAYS);
    }
}

