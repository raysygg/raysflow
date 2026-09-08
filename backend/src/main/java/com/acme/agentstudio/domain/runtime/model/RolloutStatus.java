package com.acme.agentstudio.domain.runtime.model;

/**
 * 生产应用不可变 Release 版本灰度发布与流量切流状态枚举（Rollout Status）。
 */
public enum RolloutStatus {

    /** 准备中 */
    DRAFT,

    /** 金丝雀 Canary 灰度切流中 */
    CANARY,

    /** 逐步滚动发布中 */
    ROLLING_OUT,

    /** 完全发布激活，作为活动生产主指针 */
    ACTIVE,

    /** 灰度切流暂停中 */
    PAUSED,

    /** 发现异常已全量紧急回滚 */
    ROLLED_BACK
}

