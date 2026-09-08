package com.acme.agentstudio.common.exception;

/**
 * OrchestrationConflict 业务处理异常类。
 */
/**
 * 编排版本并发冲突异常类。
 * 当多名管理员同时修改同一个 Agent/工作流画布，或者发布请求使用了过期的修订版本号时抛出。
 */
/**
 * OrchestrationConflict 业务处理异常类。
 */
public class OrchestrationConflictException extends RuntimeException {

    /**
     * 构造编排冲突异常。
     *
     * @param message 友好中文提示文案
     */
    public OrchestrationConflictException(String message) {
        super(message);
    }
}

