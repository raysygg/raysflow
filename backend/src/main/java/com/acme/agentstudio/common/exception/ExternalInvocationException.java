package com.acme.agentstudio.common.exception;

import org.springframework.http.HttpStatus;

/**
 * ExternalInvocation 业务处理异常类。
 */
/**
 * 外部 API / 应用入口调用失败异常类。
 * 用于传递稳定的业务错误码（code）与 HTTP 响应状态码（status），方便网关与前端精准识别。
 */
/**
 * ExternalInvocation 业务处理异常类。
 */
public class ExternalInvocationException extends RuntimeException {

    /** 业务错误编码 */
    private final String code;

    /** 目标 HTTP 响应状态码 */
    private final HttpStatus status;

    /**
     * 构造外部调用失败异常对象。
     *
     * @param code 业务错误编码
     * @param status HTTP 响应状态
     * @param message 面向前端的友好中文错误文案
     */
    public ExternalInvocationException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /** 获取业务错误编码 */
    public String code() {
        return code;
    }

    /** 获取 HTTP 响应状态 */
    public HttpStatus status() {
        return status;
    }
}

