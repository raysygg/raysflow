package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台支持的多智能体 Agent 协作拓扑模式枚举（Collaboration Topology）。
 */
public enum CollaborationTopology {

    /** 顺序流水线接力传递模式（Sequential Pipeline） */
    SEQUENTIAL,

    /** Supervisor 主控分发调度模式 */
    SUPERVISOR,

    /** Debate 多方自由辩论交叉审查模式 */
    DEBATE
}

