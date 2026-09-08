package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent 平台支持的底层引擎运行模式枚举（Runtime Mode）。
 * 约定运行策略 Routing，对应对话 CHAT、思考循环 REACT、规划执行 PLAN、拓扑工作流 WORKFLOW 与多 Agent 协作 MULTI_AGENT。
 */
public enum RuntimeMode {

    /** 直接进行多轮业务对话模式 */
    CHAT,

    /** 通过思考 (Reasoning) 和工具调用循环完成任务模式（ReAct） */
    REACT,

    /** 先拆解计划目标，再按步骤执行模式（Plan-Execute） */
    PLAN,

    /** 按确定性可视化拓扑流程图驱动执行模式（Workflow） */
    WORKFLOW,

    /** 由多个特定角色 Agent 团队协作分工完成复杂任务模式 */
    MULTI_AGENT
}

