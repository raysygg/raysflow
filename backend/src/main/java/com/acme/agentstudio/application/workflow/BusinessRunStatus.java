package com.acme.agentstudio.application.workflow;

import java.util.Set;

/**
 * 面向业务与前端界面展示的工作流运行状态常量集合。
 * 统一屏蔽底层引擎技术事件类型，提供清晰的用户视角状态定义与终态校验。
 */
public final class BusinessRunStatus {

    /** 状态：等待中（已入队待调度） */
    public static final String WAITING = "WAITING";

    /** 状态：运行中（正在执行节点逻辑） */
    public static final String RUNNING = "RUNNING";

    /** 状态：等待人工干预/审批（挂起中） */
    public static final String WAITING_FOR_PERSON = "WAITING_FOR_PERSON";

    /** 状态：运行成功（已顺利完成） */
    public static final String SUCCEEDED = "SUCCEEDED";

    /** 状态：运行失败（发生无法恢复的异常） */
    public static final String FAILED = "FAILED";

    /** 状态：已取消（用户或系统主动打断） */
    public static final String CANCELLED = "CANCELLED";

    /** 状态：已拒绝（审批驳回） */
    public static final String REJECTED = "REJECTED";

    /** 终态集合（不可再变更状态） */
    private static final Set<String> TERMINAL = Set.of(SUCCEEDED, FAILED, CANCELLED, REJECTED);

    /** 私有构造函数防止实例化 */
    private BusinessRunStatus() {
    }

    /**
     * 判断指定的运行状态是否属于不可逆的终态（Terminal State）。
     *
     * @param status 状态字符串
     * @return 若属于 SUCCEEDED / FAILED / CANCELLED / REJECTED 则返回 true
     */
    public static boolean isTerminal(String status) {
        return TERMINAL.contains(status);
    }
}

