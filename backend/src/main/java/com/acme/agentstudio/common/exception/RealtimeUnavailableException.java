package com.acme.agentstudio.common.exception;

/**
 * RealtimeUnavailable 业务处理异常类。
 */
/**
 * 实时通信与消息链路不可用异常类。
 * 当 Redis 票据服务未就绪、WebSocket 快照分发挂起或实时广播通道异常时抛出。
 */
/**
 * RealtimeUnavailable 业务处理异常类。
 */
public class RealtimeUnavailableException extends RuntimeException {

    /**
     * 构造实时链路不可用异常。
     *
     * @param message 友好中文提示文案
     * @param cause 根本原因异常对象
     */
    public RealtimeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

