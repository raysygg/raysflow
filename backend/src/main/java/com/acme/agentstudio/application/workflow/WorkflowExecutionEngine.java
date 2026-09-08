package com.acme.agentstudio.application.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一工作流拓扑图执行引擎（Workflow Execution Engine）。
 * 采用 DAG 拓扑遍历与分支压栈机制，负责节点步进调度（Step-by-Step Step Execution）、分支变量隔离恢复、
 * 循环重入控制以及等待人工审批（WAITING_APPROVAL）时的挂起暂停逻辑。
 */
@Component
public class WorkflowExecutionEngine {

    /** 节点执行策略注册表 */
    private final NodeExecutionHandlerRegistry handlerRegistry;

    /**
     * 构造函数注入节点处理器注册表。
     */
    public WorkflowExecutionEngine(NodeExecutionHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * 运行工作流拓扑图，直到所有节点执行完成，或遇到需要挂起的等待节点（如人工审批节点）。
     *
     * @param state 工作流当前的执行状态包裹对象 ExecutionState
     * @param processor 单步节点执行逻辑回调句柄 NodeProcessor
     * @return 最终包含输出对象、挂起状态与事件序号的 ExecutionResult
     */
    public ExecutionResult execute(ExecutionState state, NodeProcessor processor) {
        String current = state.currentNodeId();
        Object output = state.variables().mutableState();
        boolean waiting = false;
        long sequence = state.initialSequence();

        Map<String, Integer> visitCounts = new HashMap<>();
        Deque<String> pending = new ArrayDeque<>();
        Map<String, Map<String, Object>> branchValues = new HashMap<>();
        Map<String, Map<String, Object>> branchResults = new HashMap<>();

        while (canEnter(current, state, visitCounts)) {
            JsonNode node = state.nodes().get(current);
            restoreBranchVariables(current, state.variables(), branchValues);
            int visitCount = visitCounts.merge(current, 1, Integer::sum);

            NodeStepResult step = processor.execute(new NodeStep(
                    current,
                    node,
                    visitCount,
                    sequence,
                    state.variables(),
                    state.visited(),
                    pending,
                    branchValues,
                    branchResults
            ));

            output = step.output();
            waiting = step.waiting();
            sequence = step.sequence();

            if (waiting) {
                break;
            }

            branchResults.put(current, new LinkedHashMap<>(state.variables().mutableState()));
            if (step.completed() && pending.isEmpty()) {
                break;
            }

            current = nextNode(state.next(), current, step.selectedPort(), step.forcedNext(), pending);
        }

        return new ExecutionResult(output, waiting, sequence);
    }

    /** 校验是否符合进入节点的条件（非空、存在且未重复访问或节点声明为可重复进入） */
    private boolean canEnter(String nodeId, ExecutionState state, Map<String, Integer> visitCounts) {
        if (nodeId == null) {
            return false;
        }
        JsonNode node = state.nodes().get(nodeId);
        if (node == null) {
            throw new IllegalStateException("工作流拓扑图中引用了不存在的节点 ID：" + nodeId);
        }
        boolean firstVisit = state.visited().add(nodeId);
        String nodeType = node.path("nodeType").asText(node.path("type").asText(""));
        return firstVisit || handlerRegistry.descriptor(nodeType).repeatable();
    }

    /** 还原分支进入节点前的隔离变量快照 */
    private void restoreBranchVariables(
            String nodeId,
            WorkflowVariableStore variables,
            Map<String, Map<String, Object>> branchValues
    ) {
        Map<String, Object> snapshot = branchValues.remove(nodeId);
        if (snapshot == null) {
            return;
        }
        variables.mutableState().clear();
        variables.mutableState().putAll(snapshot);
    }

    /** 根据连线选择端口与强制跳转选择下一个入栈推进节点 */
    private String nextNode(
            Map<String, List<JsonNode>> next,
            String current,
            String selectedPort,
            String forcedNext,
            Deque<String> pending
    ) {
        if (forcedNext != null) {
            return forcedNext;
        }
        String nextNode = next.getOrDefault(current, List.of()).stream()
                .filter(edge -> selectedPort == null || selectedPort.equals(edge.path("sourcePort").asText()))
                .findFirst()
                .map(edge -> edge.path("targetNodeId").asText())
                .orElse(null);

        return nextNode == null && !pending.isEmpty() ? pending.pollFirst() : nextNode;
    }

    /** 单步节点执行逻辑回调函数式接口 */
    @FunctionalInterface
    public interface NodeProcessor {

        /**
         * 处理单个节点的步进执行逻辑。
         *
         * @param step 当前节点单步信息 NodeStep
         * @return 单步执行结果 NodeStepResult
         */
        NodeStepResult execute(NodeStep step);
    }

    /** 工作流引擎运行初始状态 Record */
    public record ExecutionState(
            Map<String, JsonNode> nodes,
            Map<String, List<JsonNode>> next,
            String currentNodeId,
            WorkflowVariableStore variables,
            Set<String> visited,
            long initialSequence
    ) {
    }

    /** 节点单步执行入参上下文 Record */
    public record NodeStep(
            String nodeId,
            JsonNode node,
            int visitCount,
            long sequence,
            WorkflowVariableStore variables,
            Set<String> visited,
            Deque<String> pending,
            Map<String, Map<String, Object>> branchValues,
            Map<String, Map<String, Object>> branchResults
    ) {
    }

    /** 节点单步执行返回值 Record */
    public record NodeStepResult(
            Object output,
            String selectedPort,
            String forcedNext,
            boolean waiting,
            boolean completed,
            long sequence
    ) {
    }

    /** 流程引擎最终运行结果 Record */
    public record ExecutionResult(
            Object output,
            boolean waiting,
            long sequence
    ) {
    }
}

