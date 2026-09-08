package com.acme.agentstudio.domain.runtime.model;

/**
 * 用户/Agent 长期记忆（Long-term Memory）生命周期状态枚举（Memory Lifecycle Status）。
 */
public enum MemoryLifecycleStatus {

    /** 抽取提取的记忆候选项 */
    CANDIDATE,

    /** 已人工/算法确认固化的记忆 */
    CONFIRMED,

    /** 已遗忘/拒绝的无效记忆 */
    REJECTED,

    /** 已显式删除的记忆 */
    DELETED,

    /** 超时自动失效过期的记忆 */
    EXPIRED
}

