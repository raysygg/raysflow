package com.acme.agentstudio.common.exception;

/**
 * VersionUnavailable 业务处理异常类。
 */
/**
 * 运行版本不可用异常类。
 * 当正式环境没有已发布的有效版本，或者版本依赖的 LLM / 向量模型资源已被物理删除或禁用时抛出。
 */
/**
 * VersionUnavailable 业务处理异常类。
 */
public class VersionUnavailableException extends RuntimeException {

    /**
     * 构造版本不可用异常。
     *
     * @param message 友好中文提示文案
     */
    public VersionUnavailableException(String message) {
        super(message);
    }
}

