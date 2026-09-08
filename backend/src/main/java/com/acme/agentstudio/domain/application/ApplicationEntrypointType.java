package com.acme.agentstudio.domain.application;

/**
 * 生产环境 Agent/Workflow 对外应用入口类型枚举（Application Entrypoint Type）。
 */
public enum ApplicationEntrypointType {

    /** 多轮对话卡片入口 */
    CONVERSATION,

    /** 结构化表单提交入口 */
    FORM,

    /** 标准 REST API 接口入口 */
    API,

    /** 外部 Webhook 事件回调入口 */
    WEBHOOK,

    /** 定时 Cron 调度任务入口 */
    SCHEDULE,

    /** 调试与测试入口 */
    TEST
}

