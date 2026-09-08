package com.acme.agentstudio.common.exception;

/**
 * AuthorizationDenied 业务处理异常类。
 */
/** 统一表示当前用户没有目标资源或编排动作的授权。 */
public class AuthorizationDeniedException extends RuntimeException {
    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
