package com.acme.agentstudio.domain.application;

/**
 * 运行任务 Run 执行结果响应交付方式枚举（Run Delivery Mode）。
 */
public enum RunDeliveryMode {

    /** 同步阻塞式直接一次性交付 */
    IMMEDIATE,

    /** WebSocket / SSE 流式实时推送 */
    REALTIME,

    /** 异步后台离线任务处理 */
    BACKGROUND
}

