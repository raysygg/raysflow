package com.acme.agentstudio.application.model;

/**
 * AI 模型连接探针探测出的能力就绪状态枚举。
 * 标识模型在当前配置（API Key、Base URL、Model Code）下各项能力（如对话、流式、Function Calling 工具调用、Vision 视觉）的真实支持度。
 */
public enum ModelProbeCapabilityStatus {

    /** 能力可用且已通过连通性验证 */
    AVAILABLE,

    /** 能力不可用或端点未支持 */
    UNAVAILABLE,

    /** 能力配置不匹配或模型响应格式与预期不符 */
    MISMATCH
}

