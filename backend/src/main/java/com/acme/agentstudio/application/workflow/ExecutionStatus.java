package com.acme.agentstudio.application.workflow;

import java.util.Set;

/**
 * 平台底层工作流引擎执行生命周期状态（Execution Status）标准规范。
 * 包含完整的分布式状态机：排队中（QUEUED）、运行中（RUNNING）、暂停中（PAUSING）、挂起中（PAUSED）、等待审批（WAITING_APPROVAL）与终态集合。
 */
public final class ExecutionStatus {

    /** 状态：已入队，等待 Worker 抢占调度 */
    public static final String QUEUED = "QUEUED";

    /** 状态：运行中，Worker 正执行节点 */
    public static final String RUNNING = "RUNNING";

    /** 状态：正在请求暂停中 */
    public static final String PAUSING = "PAUSING";

    /** 状态：已暂停挂起 */
    public static final String PAUSED = "PAUSED";

    /** 状态：等待人工节点审批 */
    public static final String WAITING_APPROVAL = "WAITING_APPROVAL";

    /** 状态：成功运行结束 */
    public static final String SUCCEEDED = "SUCCEEDED";

    /** 状态：发生不可逆异常失败 */
    public static final String FAILED = "FAILED";

    /** 状态：已被取消中断 */
    public static final String CANCELLED = "CANCELLED";

    /** 状态：已被审批驳回 */
    public static final String REJECTED = "REJECTED";

    /** 不可逆的终态集合 */
    private static final Set<String> TERMINAL = Set.of(SUCCEEDED, FAILED, CANCELLED, REJECTED);

    /** 私有构造函数防止实例化 */
    private ExecutionStatus() {
    }

    /**
     * 判断引擎执行状态是否处于不可逆的终态。
     *
     * @param status 状态字符串
     * @return 若属于 SUCCEEDED / FAILED / CANCELLED / REJECTED 则返回 true
     */
    public static boolean isTerminal(String status) {
        return TERMINAL.contains(status);
    }
}

