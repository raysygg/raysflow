package com.acme.agentstudio.domain.runtime.model;

/**
 * 知识库 RAG 检索策略绑定的生效作用域层级枚举（Retrieval Binding Scope）。
 * 支持应用级 (APPLICATION)、流程级 (FLOW) 与节点级 (NODE) 逐层覆盖配置。
 */
public enum RetrievalBindingScope {

    /** 应用全局全局默认配置 */
    APPLICATION,

    /** 子流程 Flow 级覆盖配置 */
    FLOW,

    /** 节点 Node 细粒度独立覆盖配置 */
    NODE
}

