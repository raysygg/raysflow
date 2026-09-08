package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台 Open RESTful API 对外统一异常错误数据结构 Record（Public API Error）。
 * 包含机器可读错误编码 code、中文友好错误提示 message、请求唯一追踪 ID requestId 以及客户端是否可重试标志 retryable。
 *
 * @param code 业务/平台错误代号（如 INVALID_PARAMETER, QUOTA_EXCEEDED）
 * @param message 面向调用方的中文友好错误提示文本
 * @param requestId HTTP 请求链路追踪 requestId
 * @param retryable 客户端是否允许直接重试发起请求
 */
public record PublicApiError(
        String code,
        String message,
        String requestId,
        boolean retryable
) {
    /** 紧凑构造函数做断言校验 */
    public PublicApiError {
        if (code == null || code.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("公共 API 错误编码和消息不能为空");
        }
    }
}

