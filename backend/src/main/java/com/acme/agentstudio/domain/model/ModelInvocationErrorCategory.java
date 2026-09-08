package com.acme.agentstudio.domain.model;

/**
 * 模型服务在线调用与探针诊断异常稳定分类枚举（Model Invocation Error Category）。
 * 用于屏蔽底层的厂商异构异常细节，暴露统一归一化后的状态与故障定位分类。
 */
public enum ModelInvocationErrorCategory {

    /** 无异常（成功） */
    NONE,

    /** 无效模型配置（如 Base URL 缺失或 ModelKey 为空） */
    INVALID_CONFIGURATION,

    /** 模型能力不匹配（如尝试用仅支持 Embed 的模型进行聊天对话） */
    CAPABILITY_MISMATCH,

    /** 身份鉴权失败（401 / 403 API Key 无效或过期） */
    AUTHENTICATION_FAILED,

    /** 频率/配额超限（429 Rate Limited） */
    RATE_LIMITED,

    /** 网络连接建立失败（如 DNS 无法解析、拒绝连接） */
    CONNECTION_FAILED,

    /** 调用超时（Connect / Read Socket Timeout） */
    TIMEOUT,

    /** 向量维度不匹配（如 768 维向量传入 1536 维索引） */
    DIMENSION_MISMATCH,

    /** 无法解析响应或返回了空 Payload */
    INVALID_RESPONSE
}

