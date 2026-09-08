package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.AgentRoleDefinition;
import com.acme.agentstudio.domain.runtime.model.CollaborationStep;
import com.acme.agentstudio.domain.runtime.model.CollaborationTopologyPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多 Agent 协作拓扑调度引擎（Collaboration Topology Engine）。
 * 负责将多智能体协作计划（SEQUENTIAL 串行 / SUPERVISOR 督导委派 / DEBATE 辩论竞争）静态展开为有界的步骤序列（CollaborationStep），
 * 纯逻辑推导，不包含 LLM 或 Tool 调用的侧效应。
 */
@Component
public class CollaborationTopologyEngine {

    /**
     * 根据协作拓扑计划排程生成有序步进节点列表。
     *
     * @param plan 拓扑计划对象 CollaborationTopologyPlan
     * @return 导出的步进节点列表 List&lt;CollaborationStep&gt;
     */
    public List<CollaborationStep> schedule(CollaborationTopologyPlan plan) {
        return switch (plan.topology()) {
            case SEQUENTIAL -> sequential(plan);
            case SUPERVISOR -> supervisor(plan);
            case DEBATE -> debate(plan);
        };
    }

    /** 串行拓扑调度逻辑 */
    private List<CollaborationStep> sequential(CollaborationTopologyPlan plan) {
        List<CollaborationStep> steps = new ArrayList<>();
        for (int index = 0; index < plan.roles().size(); index++) {
            AgentRoleDefinition role = plan.roles().get(index);
            steps.add(new CollaborationStep(
                    index,
                    1,
                    role.agentId(),
                    index == 0 ? null : plan.roles().get(index - 1).agentId(),
                    index == plan.roles().size() - 1
            ));
        }
        return steps;
    }

    /** 督导委派拓扑调度逻辑 */
    private List<CollaborationStep> supervisor(CollaborationTopologyPlan plan) {
        AgentRoleDefinition supervisor = plan.roles().stream()
                .filter(role -> role.agentId().equals(plan.supervisorAgentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Supervisor 督导 Agent 不在配置的角色列表中。"));

        List<CollaborationStep> steps = new ArrayList<>();
        steps.add(new CollaborationStep(0, 1, supervisor.agentId(), null, true));
        int sequence = 1;
        for (AgentRoleDefinition role : plan.roles()) {
            if (!role.agentId().equals(supervisor.agentId())) {
                if (!supervisor.delegableAgentIds().contains(role.agentId())) {
                    throw new IllegalArgumentException("Supervisor 督导者未获准委派 Agent 目标：" + role.agentId());
                }
                steps.add(new CollaborationStep(sequence++, 1, role.agentId(), supervisor.agentId(), false));
            }
        }
        return steps;
    }

    /** 辩论竞争拓扑调度逻辑 */
    private List<CollaborationStep> debate(CollaborationTopologyPlan plan) {
        List<CollaborationStep> steps = new ArrayList<>();
        int sequence = 0;
        for (int round = 1; round <= plan.termination().maxRounds(); round++) {
            for (AgentRoleDefinition role : plan.roles()) {
                if (sequence >= plan.termination().maxMessages()) {
                    return steps;
                }
                steps.add(new CollaborationStep(
                        sequence++,
                        round,
                        role.agentId(),
                        null,
                        round == plan.termination().maxRounds() && role == plan.roles().get(plan.roles().size() - 1)
                ));
            }
        }
        return steps;
    }
}

