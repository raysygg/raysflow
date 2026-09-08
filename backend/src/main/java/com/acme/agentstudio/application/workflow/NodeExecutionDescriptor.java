package com.acme.agentstudio.application.workflow;

import java.util.Set;

/**
 * 节点执行能力描述符 Record（Node Execution Descriptor）。
 * 在节点注册表注册时声明该类节点的执行类型（计算能力/控制流）、侧效应（模型调用/知识检索/外部 HTTP/等待人工审批等）、可重入性及输入输出契约。
 *
 * @param nodeType 节点类型
 * @param category 执行分类（能力节点 CAPABILITY / 控制流节点 CONTROL_FLOW）
 * @param sideEffect 侧效应枚举
 * @param repeatable 是否允许在单个 Run 中重复访问执行
 * @param inputContracts 输入契约集合
 * @param outputContracts 输出契约集合
 * @param controlSignals 支持的控制信号集合
 */
public record NodeExecutionDescriptor(
        String nodeType,
        ExecutionCategory category,
        SideEffect sideEffect,
        boolean repeatable,
        Set<String> inputContracts,
        Set<String> outputContracts,
        Set<NodeControlSignal> controlSignals
) {
    /** 构造函数防御性拷贝集合 */
    public NodeExecutionDescriptor {
        inputContracts = inputContracts == null ? Set.of() : Set.copyOf(inputContracts);
        outputContracts = outputContracts == null ? Set.of() : Set.copyOf(outputContracts);
        controlSignals = controlSignals == null ? Set.of() : Set.copyOf(controlSignals);
    }

    /** 节点执行大类 */
    public enum ExecutionCategory {
        /** 普通计算/数据处理能力节点 */
        CAPABILITY,

        /** 控制流调度节点 */
        CONTROL_FLOW
    }

    /** 节点执行侧效应 */
    public enum SideEffect {
        /** 无侧效应（纯计算/数据转换） */
        NONE,

        /** LLM 大模型调用 */
        MODEL_CALL,

        /** 知识库/向量检索 */
        KNOWLEDGE_RETRIEVAL,

        /** 外部 HTTP/MCP 工具调用 */
        EXTERNAL_CALL,

        /** 人工挂起等待审批 */
        HUMAN_WAIT,

        /** 唤起子流程 Agent 运行 */
        CHILD_RUN
    }
}

