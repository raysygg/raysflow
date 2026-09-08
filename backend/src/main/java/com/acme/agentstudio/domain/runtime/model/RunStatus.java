package com.acme.agentstudio.domain.runtime.model;

import java.util.Set;

/**
 * Agent 运行时 Run 任务生命周期状态枚举（Run Status）。
 * 明确区分中间过程状态（如排队 QUEUED、运行中 RUNNING、等待工具 WAITING_TOOL、等待人工审批 WAITING_HUMAN、暂停 PAUSED）
 * 与终态集合 TERMINAL（成功 SUCCEEDED、失败 FAILED、已取消 CANCELLED、已超时过期 EXPIRED）。
 */
public enum RunStatus {

    /** 已在排队队列中等待调度 */
    QUEUED,

    /** 正在 Worker 节点运行中 */
    RUNNING,

    /** 暂停等待外部工具/MCP 回调 */
    WAITING_TOOL,

    /** 挂起等待人工二次确认审批放行 */
    WAITING_HUMAN,

    /** 已被主动暂停挂起 */
    PAUSED,

    /** 运行成功完成（终态） */
    SUCCEEDED,

    /** 运行异常失败（终态） */
    FAILED,

    /** 被取消终止（终态） */
    CANCELLED,

    /** 租约超时已失效（终态） */
    EXPIRED;

    /** 所有终态状态集合 */
    private static final Set<RunStatus> TERMINAL = Set.of(SUCCEEDED, FAILED, CANCELLED, EXPIRED);

    /**
     * 判断当前 Run 任务状态是否已达到不可逆终态（Terminal State）。
     *
     * @return true 表示为终态（SUCCEEDED / FAILED / CANCELLED / EXPIRED）
     */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}

