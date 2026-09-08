package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.NodeType;
import org.springframework.stereotype.Component;

/**
 * 抽象控制流节点执行处理器。
 * 控制流节点（Loop、Parallel、Join、Human 等）不直接计算或数据转换，仅向工作流引擎发射特定的控制信号（FORK / JOIN_WAIT / WAIT_APPROVAL 等）。
 */
abstract class AbstractControlFlowNodeExecutionHandler implements NodeExecutionHandler {

    /** 变量引用解析器 */
    protected final VariableReferenceResolver resolver;

    /** 节点类型 */
    private final String nodeType;

    /** 侧效应类型 */
    private final NodeExecutionDescriptor.SideEffect sideEffect;

    /** 是否可重复访问执行 */
    private final boolean repeatable;

    /** 控制信号枚举 */
    private final NodeControlSignal signal;

    /**
     * 构造函数初始化依赖与属性。
     */
    protected AbstractControlFlowNodeExecutionHandler(VariableReferenceResolver resolver,
                                                      String nodeType,
                                                      NodeExecutionDescriptor.SideEffect sideEffect,
                                                      boolean repeatable,
                                                      NodeControlSignal signal) {
        this.resolver = resolver;
        this.nodeType = nodeType;
        this.sideEffect = sideEffect;
        this.repeatable = repeatable;
        this.signal = signal;
    }

    /**
     * 返回控制节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.control(nodeType, sideEffect, repeatable, signal);
    }

    /**
     * 封装控制信号返回结果。
     */
    protected NodeExecutionOutcome signal(NodeExecutionRequest request, NodeControlSignal signal) {
        return NodeExecutionOutcome.control(signal, request.inputs().values(), null, VariablePatch.empty());
    }
}

/**
 * 循环控制节点执行处理器（LOOP）。
 * 根据 visitCount 决定是走循环体端口（body）还是结束循环端口（done）。
 */
@Component
class LoopNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {

    /** 循环体分支端口 */
    private static final String PORT_BODY = "body";

    /** 循环完成分支端口 */
    private static final String PORT_DONE = "done";

    /**
     * 构造函数初始化循环节点。
     */
    LoopNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.LOOP.code(), NodeExecutionDescriptor.SideEffect.NONE, true, NodeControlSignal.SELECT_PORT);
    }

    /**
     * 执行循环判断分支选择。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        int maxIterations = Math.max(1, request.config().path("maxIterations").asInt(1));
        String port = request.visitCount() < maxIterations ? PORT_BODY : PORT_DONE;
        VariablePatch patch = VariablePatch.of(resolver.nodeOutputKey(request.nodeId()) + ".iteration", request.visitCount());
        return NodeExecutionOutcome.control(NodeControlSignal.SELECT_PORT, request.visitCount(), port, patch);
    }
}

/**
 * 并行分叉节点执行处理器（PARALLEL）。
 * 向引擎发出 FORK 分叉信号。
 */
@Component
class ParallelNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {
    ParallelNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.PARALLEL.code(), NodeExecutionDescriptor.SideEffect.NONE, false, NodeControlSignal.FORK);
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return signal(request, NodeControlSignal.FORK);
    }
}

/**
 * 并行汇聚等待节点执行处理器（JOIN）。
 * 向引擎发出 JOIN_WAIT 等待信号。
 */
@Component
class JoinNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {
    JoinNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.JOIN.code(), NodeExecutionDescriptor.SideEffect.NONE, true, NodeControlSignal.JOIN_WAIT);
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return signal(request, NodeControlSignal.JOIN_WAIT);
    }
}

/**
 * 迭代遍历节点执行处理器（ITERATION）。
 * 向引擎发出 ITERATE 迭代信号。
 */
@Component
class IterationNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {
    IterationNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.ITERATION.code(), NodeExecutionDescriptor.SideEffect.NONE, false, NodeControlSignal.ITERATE);
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return signal(request, NodeControlSignal.ITERATE);
    }
}

/**
 * 人工确认/审批节点执行处理器（HUMAN）。
 * 向引擎发出 WAIT_APPROVAL 人工挂起等待信号。
 */
@Component
class HumanNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {
    HumanNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.HUMAN.code(), NodeExecutionDescriptor.SideEffect.HUMAN_WAIT, false, NodeControlSignal.WAIT_APPROVAL);
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return signal(request, NodeControlSignal.WAIT_APPROVAL);
    }
}

/**
 * 子流程 Agent 唤起节点执行处理器（CHILD_RUN）。
 * 向引擎发出 CHILD_RUN 唤起子进程信号。
 */
@Component
class ChildRunNodeExecutionHandler extends AbstractControlFlowNodeExecutionHandler {
    ChildRunNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.AGENT.code(), NodeExecutionDescriptor.SideEffect.CHILD_RUN, false, NodeControlSignal.CHILD_RUN);
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return signal(request, NodeControlSignal.CHILD_RUN);
    }
}

