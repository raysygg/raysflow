package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 单次 Agent/Workflow 任务运行解析后的只读上下文快照实体 Record（Runtime Context）。
 * 集中关联 Run ID runId、租户 ID tenantId、应用 ID applicationId、不可变 Release ID releaseId、
 * 操作人 actorId、会话 ID conversationId、运行模式 mode (RuntimeMode)、分层上下文数据 layers (RuntimeContextLayers)、
 * 可调用的工具字典 tools、安全与治理策略 policy、运行限额预算 budget (RuntimeBudget)、终止规则 termination (TerminationPolicy)、
 * 解析后的依赖快照 Map resolvedSnapshot 及上下文创建时刻 createdAt。
 *
 * @param runId 运行任务唯一 ID
 * @param tenantId 归属租户物理 ID
 * @param applicationId 归属应用 ID
 * @param releaseId 绑定的发布版本 ID
 * @param actorId 触发调用的操作主体账号 ID
 * @param conversationId 关联的会话 ID
 * @param mode 运行模式（RuntimeMode）
 * @param layers 上下文分层只读数据集合（RuntimeContextLayers）
 * @param tools 授权调用的工具列表 Map
 * @param policy 路由与策略配置 Map
 * @param budget 运行硬性预算限额（RuntimeBudget）
 * @param termination 任务终止规则（TerminationPolicy）
 * @param resolvedSnapshot 依赖的模型/Prompt/知识库快照数据
 * @param createdAt 上下文生成时间
 */
public record RuntimeContext(
        String runId,
        long tenantId,
        long applicationId,
        String releaseId,
        String actorId,
        String conversationId,
        RuntimeMode mode,
        RuntimeContextLayers layers,
        Map<String, Object> tools,
        Map<String, Object> policy,
        RuntimeBudget budget,
        TerminationPolicy termination,
        Map<String, Object> resolvedSnapshot,
        Instant createdAt
) {
    /** 紧凑构造函数做上下文初始化防空与强校验 */
    public RuntimeContext {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run 标识不能为空");
        }
        if (tenantId <= 0 || applicationId <= 0) {
            throw new IllegalArgumentException("租户标识和应用标识必须有效");
        }
        if (releaseId == null || releaseId.isBlank()) {
            throw new IllegalArgumentException("发布版本标识不能为空");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("操作者标识不能为空");
        }
        if (mode == null) {
            throw new IllegalArgumentException("运行模式不能为空");
        }
        layers = (layers == null) ? new RuntimeContextLayers(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()) : layers;
        tools = immutable(tools);
        policy = immutable(policy);
        budget = (budget == null) ? RuntimeBudget.defaults() : budget;
        termination = (termination == null) ? TerminationPolicy.defaults() : termination;
        resolvedSnapshot = immutable(resolvedSnapshot);
        createdAt = (createdAt == null) ? Instant.now() : createdAt;
    }

    /**
     * 获取输入参数层字典 Map。
     *
     * @return 不可变输入参数 Map
     */
    public Map<String, Object> input() {
        return layers.input();
    }

    /**
     * 获取 Prompt 变量层字典 Map。
     *
     * @return 不可变 Prompt 变量 Map
     */
    public Map<String, Object> variables() {
        return layers.promptVariables();
    }

    /**
     * 获取短期会话记忆字典 Map。
     *
     * @return 不可变短期记忆 Map
     */
    public Map<String, Object> shortTermMemory() {
        return layers.shortTermMemory();
    }

    /**
     * 获取长期记忆事实字典 Map。
     *
     * @return 不可变长期记忆 Map
     */
    public Map<String, Object> longTermMemory() {
        return layers.longTermMemory();
    }

    /**
     * 获取 RAG 检索召回的知识库上下文 Map。
     *
     * @return 不可变召回知识 Map
     */
    public Map<String, Object> retrievedKnowledge() {
        return layers.retrievedKnowledge();
    }

    /**
     * 获取全量分层上下文容器对象。
     *
     * @return RuntimeContextLayers 对象
     */
    public RuntimeContextLayers layers() {
        return layers;
    }

    private static Map<String, Object> immutable(Map<String, Object> value) {
        return (value == null) ? Map.of() : Map.copyOf(value);
    }
}

