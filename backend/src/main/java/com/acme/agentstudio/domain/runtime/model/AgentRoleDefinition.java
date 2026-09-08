package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;
import java.util.Set;

/**
 * 多智能体 Agent 团队协作中的独立角色定义实体 Record（Agent Role Definition）。
 * 明确 Agent 标识、角色名称、核心职责目标、输入/输出 Schema、私有与共享上下文键集合、委派目标范围以及最大委派深度与步骤上限。
 *
 * @param agentId 智能体角色唯一 ID
 * @param roleName 角色展示名称
 * @param objective 核心职责与任务目标
 * @param inputSchema 输入数据契约 Schema
 * @param outputSchema 输出数据契约 Schema
 * @param privateContextKeys 隔离的私有上下文 Key 集合
 * @param sharedContextKeys 共享的团队上下文 Key 集合
 * @param delegableAgentIds 可委派下发任务的其他 Agent ID 集合
 * @param maxDelegationDepth 允许最大层级委派深度
 * @param maxSteps 角色内单次允许的最大执行步骤数
 */
public record AgentRoleDefinition(
        String agentId,
        String roleName,
        String objective,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Set<String> privateContextKeys,
        Set<String> sharedContextKeys,
        Set<String> delegableAgentIds,
        int maxDelegationDepth,
        int maxSteps
) {
    /** 紧凑构造函数做输入参数校验与集合防空转换 */
    public AgentRoleDefinition {
        if (agentId == null || agentId.isBlank() || roleName == null || roleName.isBlank()
                || objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("Agent 标识、角色名称和目标不能为空");
        }
        if (maxDelegationDepth < 0 || maxSteps < 1) {
            throw new IllegalArgumentException("Agent 委派深度和步骤上限无效");
        }
        inputSchema = (inputSchema == null) ? Map.of() : Map.copyOf(inputSchema);
        outputSchema = (outputSchema == null) ? Map.of() : Map.copyOf(outputSchema);
        privateContextKeys = (privateContextKeys == null) ? Set.of() : Set.copyOf(privateContextKeys);
        sharedContextKeys = (sharedContextKeys == null) ? Set.of() : Set.copyOf(sharedContextKeys);
        delegableAgentIds = (delegableAgentIds == null) ? Set.of() : Set.copyOf(delegableAgentIds);
    }
}

