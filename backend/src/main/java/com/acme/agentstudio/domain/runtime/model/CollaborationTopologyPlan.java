package com.acme.agentstudio.domain.runtime.model;

import java.util.List;

/**
 * 一次多智能体 Agent 运行的完整拓扑配置计划快照 Record（Collaboration Topology Plan）。
 * 校验拓扑模式 topology、角色定义列表 roles、Supervisor 模式下的主控 Agent ID 以及终止判断逻辑 termination。
 *
 * @param topology 协作拓扑模式（CollaborationTopology）
 * @param roles 团队内的角色定义列表 List&lt;AgentRoleDefinition&gt;
 * @param supervisorAgentId 主控 Agent 标识 ID（仅 SUPERVISOR 模式时有效）
 * @param termination 终止防循环规则（CollaborationTermination）
 */
public record CollaborationTopologyPlan(
        CollaborationTopology topology,
        List<AgentRoleDefinition> roles,
        String supervisorAgentId,
        CollaborationTermination termination
) {
    /** 紧凑构造函数做防空防护与拓扑规则校验 */
    public CollaborationTopologyPlan {
        if (topology == null || roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("协作拓扑和角色不能为空");
        }
        roles = List.copyOf(roles);
        termination = (termination == null) ? CollaborationTermination.defaults() : termination;
        if (topology == CollaborationTopology.SUPERVISOR
                && (supervisorAgentId == null || supervisorAgentId.isBlank())) {
            throw new IllegalArgumentException("Supervisor 拓扑必须指定主管 Agent");
        }
        if (topology == CollaborationTopology.DEBATE && roles.size() < 2) {
            throw new IllegalArgumentException("Debate 拓扑至少需要两个 Agent");
        }
    }
}

