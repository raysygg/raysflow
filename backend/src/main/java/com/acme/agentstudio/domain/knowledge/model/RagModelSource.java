package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 向量与重排模型来源分类枚举（RAG Model Source）。
 */
public enum RagModelSource {

    /** 租户私有配置的模型凭证 */
    TENANT_PRIVATE,

    /** 平台全局共享托管模型 */
    PLATFORM_SHARED,

    /** 本地/内网私有化模型服务 */
    LOCAL
}

