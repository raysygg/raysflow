package com.acme.agentstudio.domain.application;

/**
 * 应用入口版本解析路由策略枚举（Entrypoint Version Policy）。
 */
public enum EntrypointVersionPolicy {

    /** 自动跟随生产环境活动发布版本（Live Production Pointer） */
    FOLLOW_PRODUCTION,

    /** 强行固定为特定的历史 Release 版本 (Pinned Version) */
    PINNED_VERSION
}

