package com.acme.agentstudio.application.workflow;

/**
 * 节点执行器重试策略（Retry Policy Record）。
 * 规定单个节点在发生临时故障时的最大重试次数与退避等待时间，节点自定义配置只可缩小重试限制，无法突破平台硬上限。
 *
 * @param maxAttempts 最大重试尝试次数
 * @param backoffMillis 重试退避等待时间（毫秒）
 */
public record RetryPolicy(int maxAttempts, long backoffMillis) {

    /** 平台允许的最大重试次数硬上限 */
    public static final int PLATFORM_MAX_ATTEMPTS = 5;

    /** 平台允许的最大退避等待时间硬上限（30 秒） */
    public static final long PLATFORM_MAX_BACKOFF_MILLIS = 30_000L;

    /**
     * 紧凑构造函数，自动将配置参数裁剪在合法平台范围 [1, 5] 以及 [0ms, 30000ms] 内。
     */
    public RetryPolicy {
        maxAttempts = Math.min(PLATFORM_MAX_ATTEMPTS, Math.max(1, maxAttempts));
        backoffMillis = Math.min(PLATFORM_MAX_BACKOFF_MILLIS, Math.max(0L, backoffMillis));
    }
}

