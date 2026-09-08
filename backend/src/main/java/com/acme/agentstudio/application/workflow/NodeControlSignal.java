package com.acme.agentstudio.application.workflow;

/**
 * 节点向工作流执行引擎返回的控制信号枚举（Node Control Signal）。
 * 明确解耦节点具体逻辑与引擎控制逻辑，执行引擎不再通过硬编码节点类型猜测流程推进动作。
 */
public enum NodeControlSignal {

    /** 继续按顺序执行后续关联节点 */
    CONTINUE,

    /** 根据输出端口（Port）选择特定的分支节点执行 */
    SELECT_PORT,

    /** 分发并行分支（FORK） */
    FORK,

    /** 等待所有并行分支汇聚完成（JOIN_WAIT） */
    JOIN_WAIT,

    /** 遍历迭代列表元素（ITERATE） */
    ITERATE,

    /** 暂停挂起，等待人工节点审批授权（WAIT_APPROVAL） */
    WAIT_APPROVAL,

    /** 唤起并调度子流程 Agent 执行（CHILD_RUN） */
    CHILD_RUN,

    /** 当前分支或整条流程正常结束完成（COMPLETE） */
    COMPLETE
}

