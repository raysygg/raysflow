package com.acme.agentstudio.application.workflow;

import java.util.Set;

/**
 * 工作流节点执行描述符（Node Execution Descriptor）静态构建工厂。
 * 集中维护各种基础节点、分支路由节点及控制流节点的注册元数据契约。
 */
final class NodeDescriptors {

    /** 私有构造函数防止实例化 */
    private NodeDescriptors() {
    }

    /**
     * 创建普通计算/数据处理能力节点描述符。
     *
     * @param nodeType 节点类型
     * @param sideEffect 侧效应枚举
     * @return 节点描述符
     */
    static NodeExecutionDescriptor capability(String nodeType, NodeExecutionDescriptor.SideEffect sideEffect) {
        return new NodeExecutionDescriptor(nodeType, NodeExecutionDescriptor.ExecutionCategory.CAPABILITY,
                sideEffect, false, Set.of("input", "variables", "nodes", "context"), Set.of("output"),
                Set.of(NodeControlSignal.CONTINUE));
    }

    /**
     * 创建分支路由能力节点描述符。
     *
     * @param nodeType 节点类型
     * @param sideEffect 侧效应枚举
     * @return 节点描述符
     */
    static NodeExecutionDescriptor routingCapability(String nodeType, NodeExecutionDescriptor.SideEffect sideEffect) {
        return new NodeExecutionDescriptor(nodeType, NodeExecutionDescriptor.ExecutionCategory.CAPABILITY,
                sideEffect, false, Set.of("input", "variables", "nodes", "context"), Set.of("output"),
                Set.of(NodeControlSignal.SELECT_PORT));
    }

    /**
     * 创建控制流节点描述符（如 LOOP、PARALLEL、HUMAN 等）。
     *
     * @param nodeType 节点类型
     * @param sideEffect 侧效应枚举
     * @param repeatable 是否可重入重复执行
     * @param signal 控制信号枚举
     * @return 节点描述符
     */
    static NodeExecutionDescriptor control(String nodeType, NodeExecutionDescriptor.SideEffect sideEffect,
                                           boolean repeatable, NodeControlSignal signal) {
        return new NodeExecutionDescriptor(nodeType, NodeExecutionDescriptor.ExecutionCategory.CONTROL_FLOW,
                sideEffect, repeatable, Set.of("variables", "nodes", "context"), Set.of("control"),
                Set.of(signal));
    }
}

