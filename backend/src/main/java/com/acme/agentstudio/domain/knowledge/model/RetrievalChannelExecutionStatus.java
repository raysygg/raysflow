package com.acme.agentstudio.domain.knowledge.model;

/**
 * 单个召回通道的执行状态枚举（Retrieval Channel Execution Status）。
 */
public enum RetrievalChannelExecutionStatus {

    /** 召回成功 */
    SUCCESS,

    /** 召回异常失败 */
    FAILED,

    /** 规则跳过未执行 */
    SKIPPED
}

