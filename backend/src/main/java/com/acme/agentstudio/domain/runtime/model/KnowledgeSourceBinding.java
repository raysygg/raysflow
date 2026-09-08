package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent 应用、Flow 工作流或具体 Node 节点绑定关联知识源与 RAG 检索策略快照 Record（Knowledge Source Binding）。
 * 包含绑定作用域 scope (RetrievalBindingScope)、拥有者唯一标识 ID ownerId、知识库 ID knowledgeSourceId、
 * 挂载的检索策略 policy (RetrievalPolicy) 及启停标志 enabled。
 *
 * @param scope 检索绑定作用域级别（RetrievalBindingScope）
 * @param ownerId 作用域宿主 ID（如 App ID, Flow ID 或 Node ID）
 * @param knowledgeSourceId 挂载关联的知识库物理主键 ID
 * @param policy 采用的检索与重排策略（RetrievalPolicy）
 * @param enabled 是否启用到当前链路中
 */
public record KnowledgeSourceBinding(
        RetrievalBindingScope scope,
        String ownerId,
        long knowledgeSourceId,
        RetrievalPolicy policy,
        boolean enabled
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public KnowledgeSourceBinding {
        if (scope == null || ownerId == null || ownerId.isBlank() || knowledgeSourceId <= 0) {
            throw new IllegalArgumentException("知识源绑定范围、所有者和知识源标识无效");
        }
        policy = (policy == null) ? RetrievalPolicy.defaults() : policy;
    }
}

