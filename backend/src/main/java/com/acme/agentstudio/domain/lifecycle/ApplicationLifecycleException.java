package com.acme.agentstudio.domain.lifecycle;

/**
 * ApplicationLifecycle 业务处理异常类。
 */
/** 生命周期业务异常，错误码用于前端定位修复动作。 */
public class ApplicationLifecycleException extends RuntimeException {
    private final String code;

    public ApplicationLifecycleException(String code, String message) {
        super(message);
        this.code = code;
    }

        /**
         * 获取getCode 业务逻辑处理。
         * @return String 返回对象
         */
    public String getCode() { return code; }
}
