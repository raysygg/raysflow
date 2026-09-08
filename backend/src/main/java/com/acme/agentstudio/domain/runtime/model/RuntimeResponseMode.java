package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent 应用对外部调用方的响应交付方式模式枚举（Runtime Response Mode）。
 * 明确同步即时返回 (IMMEDIATE)、SSE/WebSocket 实时流式生成 (REALTIME)、异步后台队列处理 (BACKGROUND) 或自动推断 (AUTO)。
 */
public enum RuntimeResponseMode {

    /** 平台根据流程节点能力自动选择交付方式 */
    AUTO,

    /** 同步阻塞式直接返回最终文本/结构化结果 */
    IMMEDIATE,

    /** 建立 SSE/WebSocket 长连接持续推送增量流式 Content 与节点事件 */
    REALTIME,

    /** 创建后台异步 Run 任务队列执行，后续通过轮询或 Webhook 接收通知 */
    BACKGROUND;

    /**
     * 安全解析响应模式字符串。
     *
     * @param value 输入字符串
     * @return 对应的 RuntimeResponseMode 枚举值，为空时默认返回 AUTO
     * @throws IllegalArgumentException 当传入非法格式字符串时抛出友好中文提示异常
     */
    public static RuntimeResponseMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("不支持的运行响应方式：" + value + "，可选即时返回、实时生成或后台处理", exception);
        }
    }
}

