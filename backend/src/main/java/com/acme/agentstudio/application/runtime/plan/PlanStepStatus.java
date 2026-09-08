package com.acme.agentstudio.application.runtime.plan;

/**
 * 运行时 Plan 模式步骤生命周期状态枚举（Plan Step Status）。
 */
public enum PlanStepStatus {

    /** 待执行（满足依赖后即可被调度执行） */
    PENDING,

    /** 运行中（正处于 Model/Tool 执行计算过程） */
    RUNNING,

    /** 执行成功 */
    SUCCEEDED,

    /** 执行失败（超过重试上限置为终态失败） */
    FAILED,

    /** 挂起等待人工确认或介入决策 */
    WAITING_HUMAN,

    /** 已跳过（由于前置依赖失败或动态 Replan 被跳过） */
    SKIPPED
}

