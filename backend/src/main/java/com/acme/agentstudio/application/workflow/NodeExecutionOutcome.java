package com.acme.agentstudio.application.workflow;

/**
 * 节点执行结果标准 Outcome Record。
 * 包含节点产生的业务输出数据、写回的变量 Patch 补丁、选中的路由端口 Port、Trace 诊断信息以及向引擎发射的控制信号 Signal。
 *
 * @param output 节点产生的业务输出数据
 * @param variablePatch 需要写回流程全局变量域的补丁 Map
 * @param selectedPort 选中的路由输出端口名称（可选）
 * @param trace LLM/RAG 执行诊断追溯对象（可选）
 * @param controlSignal 向工作流引擎发射的控制信号
 */
public record NodeExecutionOutcome(
        Object output,
        VariablePatch variablePatch,
        String selectedPort,
        NodeExecutionTrace trace,
        NodeControlSignal controlSignal
) {
    /** 构造函数防护初始化 */
    public NodeExecutionOutcome {
        variablePatch = variablePatch == null ? VariablePatch.empty() : variablePatch;
        controlSignal = controlSignal == null ? NodeControlSignal.CONTINUE : controlSignal;
    }

    /**
     * 静态工厂：构造普通输出结果（控制信号默认为 CONTINUE）。
     *
     * @param output 业务输出对象
     * @param patch 变量补丁
     * @return 节点执行 Outcome
     */
    public static NodeExecutionOutcome output(Object output, VariablePatch patch) {
        return new NodeExecutionOutcome(output, patch, null, null, NodeControlSignal.CONTINUE);
    }

    /**
     * 静态工厂：构造分支路由选择结果（控制信号为 SELECT_PORT）。
     *
     * @param output 业务输出对象
     * @param port 选中的端口 Key
     * @param patch 变量补丁
     * @return 节点执行 Outcome
     */
    public static NodeExecutionOutcome route(Object output, String port, VariablePatch patch) {
        return new NodeExecutionOutcome(output, patch, port, null, NodeControlSignal.SELECT_PORT);
    }

    /**
     * 静态工厂：构造模型生成结果（带 Trace 追溯，控制信号默认为 CONTINUE）。
     *
     * @param output 业务输出对象
     * @param patch 变量补丁
     * @param trace Trace 追溯对象
     * @return 节点执行 Outcome
     */
    public static NodeExecutionOutcome model(Object output, VariablePatch patch, NodeExecutionTrace trace) {
        return new NodeExecutionOutcome(output, patch, null, trace, NodeControlSignal.CONTINUE);
    }

    /**
     * 静态工厂：构造模型分支分类路由结果（带 Trace 追溯）。
     *
     * @param output 业务输出对象
     * @param port 选中的端口 Key
     * @param patch 变量补丁
     * @param trace Trace 追溯对象
     * @return 节点执行 Outcome
     */
    public static NodeExecutionOutcome modelRoute(Object output, String port, VariablePatch patch,
                                                   NodeExecutionTrace trace) {
        NodeControlSignal signal = port == null ? NodeControlSignal.CONTINUE : NodeControlSignal.SELECT_PORT;
        return new NodeExecutionOutcome(output, patch, port, trace, signal);
    }

    /**
     * 静态工厂：构造显式控制信号结果（用于控制流节点）。
     *
     * @param signal 控制信号枚举
     * @param output 业务输出对象
     * @param port 选中的端口 Key
     * @param patch 变量补丁
     * @return 节点执行 Outcome
     */
    public static NodeExecutionOutcome control(NodeControlSignal signal, Object output, String port,
                                                VariablePatch patch) {
        return new NodeExecutionOutcome(output, patch, port, null, signal);
    }
}

