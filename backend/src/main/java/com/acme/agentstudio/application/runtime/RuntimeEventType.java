package com.acme.agentstudio.application.runtime;

/**
 * 运行时事件类型枚举（Runtime Event Type）。
 * 定义 Agent 运行生命周期中的各个阶段与通知事件类型，供 SSE/WebSocket 订阅端或后台审计日志消费判断。
 */
public enum RuntimeEventType {

    /** 运行启动事件 */
    RUN_STARTED,

    /** 上下文解析与层叠构建完成事件 */
    CONTEXT_BUILT,

    /** 大语言模型发起调用与响应事件 */
    MODEL_CALLED,

    /** 工具调用动作开始事件 */
    TOOL_CALL_STARTED,

    /** 工具调用动作成功/失败完成事件 */
    TOOL_CALL_COMPLETED,

    /** 记忆读取加载完成事件 */
    MEMORY_READ,

    /** 长期/短期对话记忆写入持久化事件 */
    MEMORY_WRITTEN,

    /** 知识库 RAG 检索命中完成事件 */
    RAG_RETRIEVED,

    /** 内部 Agent 子线程/策略开始执行事件 */
    AGENT_STARTED,

    /** 人工审批/补充输入暂停等待事件 */
    HUMAN_INPUT_REQUIRED,

    /** Run 任务正常成功结束事件 */
    RUN_COMPLETED,

    /** Run 任务异常失败事件 */
    RUN_FAILED
}

