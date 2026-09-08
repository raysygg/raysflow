package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库 Index Generation 向量索引版本构建状态枚举（Index Generation Status）。
 */
public enum IndexGenerationStatus {

    /** 尚未创建索引版本 */
    NOT_CREATED,

    /** 异步向量解析与索引构建中 */
    BUILDING,

    /** 构建完成且正作为生产活动版本生效 */
    ACTIVE,

    /** 索引生成失败 */
    FAILED,

    /** 已废弃退役 */
    RETIRED
}

