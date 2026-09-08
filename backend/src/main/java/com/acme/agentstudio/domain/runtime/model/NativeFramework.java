package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台原生 Agent 协作框架扩展协议枚举（Native Framework）。
 * 支持集成 Microsoft AutoGen、LangChain LangGraph、CAMEL 及 AgentScope 多智能体运行能力。
 */
public enum NativeFramework {

    /** Microsoft AutoGen 协作框架 */
    AUTOGEN,

    /** LangChain LangGraph 状态图框架 */
    LANGGRAPH,

    /** CAMEL Communicative Agents 框架 */
    CAMEL,

    /** Alibaba AgentScope 智能体协作框架 */
    AGENTSCOPE
}

