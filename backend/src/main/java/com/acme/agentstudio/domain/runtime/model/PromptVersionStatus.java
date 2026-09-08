package com.acme.agentstudio.domain.runtime.model;

/**
 * Prompt 提示词资产版本生命周期状态枚举（Prompt Version Status）。
 */
public enum PromptVersionStatus {

    /** 草稿中 */
    DRAFT,

    /** 已发布固定版本 */
    PUBLISHED,

    /** 已归档下线 */
    ARCHIVED
}

