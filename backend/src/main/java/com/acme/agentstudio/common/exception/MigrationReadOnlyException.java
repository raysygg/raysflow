package com.acme.agentstudio.common.exception;

/**
 * MigrationReadOnly 业务处理异常类。
 */
/**
 * 数据迁移只读模式异常类。
 * 当系统处于冷热数据迁移或数据库结构升级期间，写接口被调用时抛出该异常，提示用户当前处于只读维护状态。
 */
/**
 * MigrationReadOnly 业务处理异常类。
 */
public class MigrationReadOnlyException extends RuntimeException {

    /**
     * 构造数据迁移只读异常。
     *
     * @param message 友好中文提示文案
     */
    public MigrationReadOnlyException(String message) {
        super(message);
    }
}

