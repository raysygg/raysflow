package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory;

import java.util.EnumSet;
import java.util.Set;

/**
 * 运行时任务自动重试策略与异常分类诊断工具（Runtime Retry Policy）。
 * 针对 Worker 执行过程抛出的异常进行分类归因（TRANSIENT 瞬时网络错误 / TIMEOUT 超时 / RATE_LIMIT 触发限流 / AUTHENTICATION 认证失效 / CONFIGURATION 配置错误）。
 * 规定只有瞬时错误、超时与限流（RETRYABLE 集合）允许触发自动重试，认证失效与配置错误等确定性代码/配置缺陷禁止无脑重试，直接入死信队列。
 */
public final class RuntimeRetryPolicy {

    /** 允许自动重试的错误分类集合 */
    private static final Set<ErrorCategory> RETRYABLE = EnumSet.of(
            ErrorCategory.TRANSIENT,
            ErrorCategory.TIMEOUT,
            ErrorCategory.RATE_LIMIT
    );

    /**
     * 私有构造函数，防止实例化工具类。
     */
    private RuntimeRetryPolicy() {
    }

    /**
     * 判断特定的错误分类 ErrorCategory 是否允许触发自动重试。
     *
     * @param category 错误分类枚举 ErrorCategory
     * @return true 表示允许重试，false 表示不可重试
     */
    public static boolean retryable(ErrorCategory category) {
        if (category == null) {
            return false;
        }
        return RETRYABLE.contains(category);
    }

    /**
     * 对捕获的 Throwable 运行时异常进行分类识别归因。
     *
     * @param error 捕获的异常对象 Throwable
     * @return 识别出的错误分类 ErrorCategory
     */
    public static ErrorCategory classify(Throwable error) {
        if (error == null) {
            return ErrorCategory.UNKNOWN;
        }
        String message = (error.getMessage() == null) ? "" : error.getMessage().toLowerCase();

        if (message.contains("auth") || message.contains("认证") || message.contains("unauthorized") || message.contains("forbidden")) {
            return ErrorCategory.AUTHENTICATION;
        }
        if (message.contains("timeout") || message.contains("超时") || message.contains("timed out")) {
            return ErrorCategory.TIMEOUT;
        }
        if (message.contains("rate") || message.contains("限流") || message.contains("too many requests") || message.contains("429")) {
            return ErrorCategory.RATE_LIMIT;
        }
        if (message.contains("config") || message.contains("配置") || message.contains("invalid configuration")) {
            return ErrorCategory.CONFIGURATION;
        }

        return ErrorCategory.UNKNOWN;
    }
}

