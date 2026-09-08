package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.AgentRoleDefinition;
import com.acme.agentstudio.domain.runtime.model.CollaborationStep;
import com.acme.agentstudio.domain.runtime.model.CollaborationTopologyPlan;
import com.acme.agentstudio.domain.runtime.model.RunStatus;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多 Agent 协作运行时策略实现（Multi-Agent Runtime Strategy）。
 * 负责多智能体（SEQUENTIAL 串行 / SUPERVISOR 督导委派 / DEBATE 辩论竞争）拓扑排程、角色级共享上下文过滤隔离（Shared Context Scoping）
 * 以及初始化运行事件生成。
 */
@Component
public class MultiAgentRuntimeStrategy implements RuntimeStrategy {

    /** 多 Agent 拓扑排程引擎 */
    private final CollaborationTopologyEngine topologyEngine;

    /**
     * 构造函数注入拓扑调度引擎。
     *
     * @param topologyEngine 拓扑调度引擎 CollaborationTopologyEngine
     */
    public MultiAgentRuntimeStrategy(CollaborationTopologyEngine topologyEngine) {
        this.topologyEngine = topologyEngine;
    }

    /**
     * 获取策略支持的运行模式（MULTI_AGENT）。
     *
     * @return 运行模式枚举 RuntimeMode.MULTI_AGENT
     */
    @Override
    public RuntimeMode mode() {
        return RuntimeMode.MULTI_AGENT;
    }

    /**
     * 导出 Multi-Agent 策略的自我描述契约（描述符 Descriptor）。
     *
     * @return 策略描述符 Descriptor
     */
    @Override
    public RuntimeStrategyContracts.Descriptor descriptor() {
        return new RuntimeStrategyContracts.Descriptor(
                RuntimeMode.MULTI_AGENT,
                "多 Agent 协作",
                "RuntimeContext.input + CollaborationTopologyPlan",
                "RuntimeStrategyContracts.Result.output",
                "RuntimeContext.layers + AgentRoleDefinition.sharedContextKeys",
                List.of(
                        RuntimeStrategyContracts.EventKind.STRATEGY_STARTED,
                        RuntimeStrategyContracts.EventKind.CONTEXT_RESOLVED,
                        RuntimeStrategyContracts.EventKind.AGENT_DELEGATED,
                        RuntimeStrategyContracts.EventKind.STEP_STARTED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_COMPLETED,
                        RuntimeStrategyContracts.EventKind.STRATEGY_FAILED
                ),
                false
        );
    }

    /**
     * 初始化启动多 Agent 运行并构建首条 RUN_STARTED 事件。
     *
     * @param context 运行上下文 RuntimeContext
     * @return 事件列表 List&lt;RuntimeEvent&gt;
     */
    @Override
    public List<RuntimeEvent> start(RuntimeContext context) {
        return List.of(new RuntimeEvent(
                UUID.randomUUID().toString(),
                context.runId(),
                0,
                RuntimeEventType.RUN_STARTED,
                RunStatus.RUNNING,
                Map.of("mode", RuntimeMode.MULTI_AGENT.name()),
                Instant.now()
        ));
    }

    /**
     * 根据多 Agent 拓扑计划排程生成执行步进节点列表。
     *
     * @param context 运行上下文 RuntimeContext
     * @param plan 拓扑计划对象 CollaborationTopologyPlan
     * @return 步骤列表 List&lt;CollaborationStep&gt;
     */
    public List<CollaborationStep> schedule(RuntimeContext context, CollaborationTopologyPlan plan) {
        if (context == null || plan == null) {
            throw new IllegalArgumentException("Multi-Agent Runtime 上下文 context 与拓扑计划 plan 均不能为空。");
        }
        return topologyEngine.schedule(plan);
    }

    /**
     * 依据 Agent 角色定义白名单过滤提取该角色允许读取的共享上下文变量。
     *
     * @param context 运行上下文 RuntimeContext
     * @param role Agent 角色定义实体 AgentRoleDefinition
     * @return 提取的共享变量 Map
     */
    public Map<String, Object> sharedContext(RuntimeContext context, AgentRoleDefinition role) {
        if (context == null || role == null) {
            throw new IllegalArgumentException("运行上下文 context 与 Agent 角色定义 role 均不能为空。");
        }
        Map<String, Object> shared = new LinkedHashMap<>();
        for (String key : role.sharedContextKeys()) {
            if (context.variables().containsKey(key)) {
                shared.put(key, context.variables().get(key));
            }
        }
        return Map.copyOf(shared);
    }
}

