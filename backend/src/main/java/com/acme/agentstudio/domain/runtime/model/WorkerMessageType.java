package com.acme.agentstudio.domain.runtime.model;

/**
 * 分布式 Worker 节点与 Agent 平台控制台之间的内部通信消息类型枚举（Worker Message Type）。
 */
public enum WorkerMessageType {

    /** 启动执行指令 */
    START,

    /** 节点中间运行事件消息 */
    EVENT,

    /** 检查点 Checkpoint 挂起/保存 */
    CHECKPOINT,

    /** 节点心跳包维持 */
    HEARTBEAT,

    /** 任务正常完成 */
    COMPLETE,

    /** 任务异常失败 */
    FAIL,

    /** 任务取消中发 */
    CANCEL
}

