package com.acme.agentstudio.domain.knowledge.model;

/**
 * 检索识别的语种来源枚举（Retrieval Language Source）。
 */
public enum RetrievalLanguageSource {

    /** 来自目标文档的元数据标记 */
    DOCUMENT,

    /** 来自用户 API 显式传入参数 */
    REQUEST,

    /** 来自系统语言检测器自动判定 */
    DETECTED,

    /** 降级退回默认兜底语种 */
    FALLBACK
}

