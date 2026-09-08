package com.acme.agentstudio.application.workflow;

/**
 * 平台工作流与节点执行事件类型（Execution Event Type）常量集中定义。
 * 统一前后端与事件驱动架构的核心事件协议，避免散落硬编码字符串产生状态飘移。
 */
public final class ExecutionEventType {

    /** 执行成功 */
    public static final String EXECUTION_SUCCEEDED = "EXECUTION_SUCCEEDED";

    /** 执行失败 */
    public static final String EXECUTION_FAILED = "EXECUTION_FAILED";

    /** 执行取消 */
    public static final String EXECUTION_CANCELLED = "EXECUTION_CANCELLED";

    /** 执行拒绝 */
    public static final String EXECUTION_REJECTED = "EXECUTION_REJECTED";

    /** 节点开始执行 */
    public static final String NODE_STARTED = "NODE_STARTED";

    /** 节点执行成功 */
    public static final String NODE_SUCCEEDED = "NODE_SUCCEEDED";

    /** 节点执行失败 */
    public static final String NODE_FAILED = "NODE_FAILED";

    /** 节点失败重试已调度 */
    public static final String NODE_RETRY_SCHEDULED = "NODE_RETRY_SCHEDULED";

    /** LLM 上下文与 RAG 召回来源解析完成 */
    public static final String LLM_CONTEXT_RESOLVED = "LLM_CONTEXT_RESOLVED";

    /** 流式 Token 增量事件 */
    public static final String MODEL_DELTA = "MODEL_DELTA";

    /** 人工节点审批通过 */
    public static final String APPROVAL_APPROVE = "APPROVAL_APPROVE";

    /** 人工节点审批驳回 */
    public static final String APPROVAL_REJECT = "APPROVAL_REJECT";

    /** 租约超时回收事件 */
    public static final String LEASE_RECLAIMED = "LEASE_RECLAIMED";

    /** 高级 Agent 策略节点：策略启动 */
    public static final String STRATEGY_STARTED = "STRATEGY_STARTED";

    /** 高级 Agent 策略节点：单步开始 */
    public static final String STRATEGY_STEP_STARTED = "STRATEGY_STEP_STARTED";

    /** 高级 Agent 策略节点：策略完成 */
    public static final String STRATEGY_COMPLETED = "STRATEGY_COMPLETED";

    /** 高级 Agent 策略节点：策略失败 */
    public static final String STRATEGY_FAILED = "STRATEGY_FAILED";

    /** 私有构造函数防止实例化 */
    private ExecutionEventType() {
    }

    /**
     * 判断事件类型是否代表整体工作流的终态结束事件。
     *
     * @param eventType 事件类型字符串
     * @return 若为成功/失败/取消/拒绝事件则返回 true
     */
    public static boolean isTerminal(String eventType) {
        return EXECUTION_SUCCEEDED.equals(eventType)
                || EXECUTION_FAILED.equals(eventType)
                || EXECUTION_CANCELLED.equals(eventType)
                || EXECUTION_REJECTED.equals(eventType);
    }
}

