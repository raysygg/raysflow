package com.acme.agentstudio.application.workflow;

/**
 * 流程可恢复/重试任务的原因枚举（Recovery Reason）。
 * 供系统审计日志、Worker 引擎故障恢复重试以及前端提示共同使用。
 */
public enum RecoveryReason {

    /** 初始正常首次调度执行 */
    INITIAL_EXECUTION,

    /** 人工审批节点接收审批决策后继续调度 */
    APPROVAL_DECISION,

    /** 用户在控制台手动点击重试/重算 */
    MANUAL_RETRY,

    /** Worker 租约超时失效后被定时器扫表并自动重新领占恢复 */
    LEASE_EXPIRED
}

