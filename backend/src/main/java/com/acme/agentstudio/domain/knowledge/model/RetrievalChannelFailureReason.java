package com.acme.agentstudio.domain.knowledge.model;

/**
 * 召回通道未正常执行或跳过原因枚举（Retrieval Channel Failure Reason）。
 */
public enum RetrievalChannelFailureReason {

    /** 无失败异常，正常执行 */
    NONE,

    /** 查询文本特征为空，跳过本通道 */
    QUERY_FEATURE_EMPTY,

    /** 供应商或底座服务异常失败 */
    PROVIDER_FAILURE
}

