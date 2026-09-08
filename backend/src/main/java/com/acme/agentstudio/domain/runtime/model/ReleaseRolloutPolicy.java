package com.acme.agentstudio.domain.runtime.model;

/**
 * 生产环境版本灰度金丝雀发布、健康度持续观测与自动回滚策略 Record（Release Rollout Policy）。
 * 包含首批金丝雀流量比例 canaryPercentage、每步增量比例 rolloutStepPercentage、
 * 容忍的最低成功率基线 minimumSuccessRate、健康检查窗口毫秒数 healthCheckWindowMillis 及指标异常时自动回滚标志 autoRollback。
 *
 * @param canaryPercentage 首次金丝雀分流百分比（1 ~ 100）
 * @param rolloutStepPercentage 每次全量推进的步进百分比（1 ~ 100）
 * @param minimumSuccessRate 触发回滚前的最小成功率阈值（0.0 ~ 1.0）
 * @param healthCheckWindowMillis 观察期健康度检查时间窗口（毫秒）
 * @param autoRollback 成功率跌破基线时是否自动将指针回滚至上一稳定版本
 */
public record ReleaseRolloutPolicy(
        int canaryPercentage,
        int rolloutStepPercentage,
        double minimumSuccessRate,
        long healthCheckWindowMillis,
        boolean autoRollback
) {
    /** 默认金丝雀发布比例：5% */
    public static final int DEFAULT_CANARY_PERCENTAGE = 5;

    /** 默认灰度推进步长：25% */
    public static final int DEFAULT_ROLLOUT_STEP_PERCENTAGE = 25;

    /** 默认最小成功率阈值：98% */
    public static final double DEFAULT_MINIMUM_SUCCESS_RATE = 0.98D;

    /** 默认观察窗口时长：5 分钟（300,000 毫秒） */
    public static final long DEFAULT_HEALTH_CHECK_WINDOW_MILLIS = 300_000L;

    /** 紧凑构造函数做灰度参数断言校验 */
    public ReleaseRolloutPolicy {
        if (canaryPercentage < 1 || canaryPercentage > 100 || rolloutStepPercentage < 1 || rolloutStepPercentage > 100
                || minimumSuccessRate < 0D || minimumSuccessRate > 1D || healthCheckWindowMillis < 1) {
            throw new IllegalArgumentException("灰度发布策略参数无效");
        }
    }

    /**
     * 构建默认灰度发布策略。
     *
     * @return 默认的 ReleaseRolloutPolicy 实例
     */
    public static ReleaseRolloutPolicy defaults() {
        return new ReleaseRolloutPolicy(DEFAULT_CANARY_PERCENTAGE, DEFAULT_ROLLOUT_STEP_PERCENTAGE,
                DEFAULT_MINIMUM_SUCCESS_RATE, DEFAULT_HEALTH_CHECK_WINDOW_MILLIS, true);
    }
}

