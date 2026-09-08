package com.acme.agentstudio.domain.application;

/**
 * 运行任务 Run 的可审计触发来源枚举（Run Trigger Source）。
 */
public enum RunTriggerSource {

    /** 内部用户界面触发 */
    INTERNAL_USER,

    /** 外部开放 API 接口触发 */
    EXTERNAL_API,

    /** Webhook 事件回调触发 */
    WEBHOOK,

    /** 定时 Cron 调度任务触发 */
    SCHEDULE,

    /** Studio 调试画布测试触发 */
    STUDIO_TEST
}

