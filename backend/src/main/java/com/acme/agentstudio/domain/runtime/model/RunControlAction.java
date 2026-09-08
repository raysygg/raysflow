package com.acme.agentstudio.domain.runtime.model;

/**
 * 针对运行中 Run 任务的生命周期控制命令动作枚举（Run Control Action）。
 */
public enum RunControlAction {

    /** 取消终止运行 */
    CANCEL,

    /** 暂停运行 */
    PAUSE,

    /** 恢复运行 */
    RESUME,

    /** 节点重试 */
    RETRY,

    /** 故障恢复 */
    RECOVER,

    /** 心跳续约维持 */
    HEARTBEAT,

    /** 人工审批同意 */
    APPROVE,

    /** 人工审批驳回 */
    REJECT,

    /** 任务转移指派 */
    TRANSFER,

    /** 任务重新重放 */
    REPLAY
}

