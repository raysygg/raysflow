package com.acme.agentstudio.domain.common;

/**
 * 运行任务与工作流执行状态机稳定编码枚举（Execution Status）。
 */
public enum ExecutionStatus {

    /** 队列等待中 */
    QUEUED,

    /** 运行执行中 */
    RUNNING,

    /** 挂起等待人工确认审批 */
    WAITING_HUMAN,

    /** 已暂停 */
    PAUSED,

    /** 成功执行完成 */
    SUCCEEDED,

    /** 执行异常失败 */
    FAILED,

    /** 已手动取消 */
    CANCELLED,

    /** 触发逆向补偿回滚中 */
    COMPENSATING
}

