package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent Runtime 生产分布式部署微服务组件分类枚举（Runtime Component）。
 */
public enum RuntimeComponent {

    /** REST/WebSocket API 接入网关组件 */
    API,

    /** 定时任务与 Cron 触发调度组件 */
    SCHEDULER,

    /** 持久化高可靠队列组件 */
    DURABLE_QUEUE,

    /** Agent 工作流图节点 Runner 执行节点组件 */
    WORKER,

    /** 模型 LLM/Embedding 调用执行器组件 */
    MODEL_EXECUTOR,

    /** 外部 Tool/MCP 工具隔离沙箱执行器组件 */
    TOOL_EXECUTOR,

    /** 事件与 Trace 日志持久化存储组件 */
    EVENT_STORE,

    /** 运行观测与 SLI/SLO 监控报警组件 */
    OBSERVABILITY
}

