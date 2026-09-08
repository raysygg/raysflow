package com.acme.agentstudio.application.runtime.react;

import com.acme.agentstudio.domain.runtime.model.RunStatus;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 运行时 ReAct 思考-行动-观察循环驱动引擎（ReAct Execution Engine）。
 * 在给定的 RuntimeContext 运行上下文约束（预算 maxSteps 与终止条件 maxToolCalls）下，
 * 循环调用 ReActModelPort 做出推理决策，支持工具重试机制（invokeWithRetry：最多 RETRY_LIMIT = 2 次）、
 * 观察积累（observations）、中断响应 cancellationRequested、人工审批挂起（WAITING_HUMAN）以及实时检查点持久化推送（checkpointSink）。
 */
@Component
public class ReActExecutionEngine {

    /** 默认起始重试步骤次数 */
    private static final int MIN_STEP = 1;

    /** 工具失败自动重试最大限制次数 */
    private static final int RETRY_LIMIT = 2;

    /**
     * 驱动 ReAct 循环迭代，直到生成 FINAL_ANSWER、耗尽预算步骤、取消或触发失败。
     *
     * @param context 运行上下文 RuntimeContext
     * @param model 模型推理适配端口 ReActModelPort
     * @param tool 工具调用执行端口 ReActToolPort
     * @param cancellationRequested 外部取消信号检查 BooleanSupplier（可选）
     * @param checkpointSink 检查点实时回调消费 Consumer（可选）
     * @return ReAct 执行终态结果对象 ReActExecutionResult
     */
    public ReActExecutionResult execute(
            RuntimeContext context,
            ReActModelPort model,
            ReActToolPort tool,
            BooleanSupplier cancellationRequested,
            Consumer<ReActCheckpoint> checkpointSink
    ) {
        if (context == null || model == null || tool == null) {
            throw new IllegalArgumentException("执行 ReAct 驱动引擎时，上下文 RuntimeContext、模型端口 ReActModelPort 与工具端口 ReActToolPort 均不能为空。");
        }

        List<String> observations = new ArrayList<>();
        int step = 0;
        int toolCalls = 0;

        while (step < context.budget().maxSteps()) {
            if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
                return result(RunStatus.CANCELLED, "", context.runId(), step, toolCalls, observations, "", "用户或系统手动取消了运行任务。");
            }

            step++;
            ReActDecision decision;
            try {
                decision = model.decide(context, List.copyOf(observations));
            } catch (RuntimeException exception) {
                return result(RunStatus.FAILED, "", context.runId(), step, toolCalls, observations, "",
                        "ReAct 模型推理决策过程发生异常：" + exception.getMessage());
            }

            if (decision.type() == ReActDecision.DecisionType.FINAL_ANSWER) {
                return result(RunStatus.SUCCEEDED, decision.text(), context.runId(), step, toolCalls, observations, "", "");
            }

            if (decision.requiresHumanApproval()) {
                ReActCheckpoint checkpoint = checkpoint(
                        context.runId(),
                        step,
                        toolCalls,
                        RunStatus.WAITING_HUMAN,
                        observations,
                        decision.toolId()
                );
                persist(checkpoint, checkpointSink);
                return new ReActExecutionResult(RunStatus.WAITING_HUMAN, "", checkpoint, "高风险工具调用触发人工审批，已挂起等待。");
            }

            toolCalls++;
            if (toolCalls > context.termination().maxToolCalls()) {
                return result(RunStatus.FAILED, "", context.runId(), step, toolCalls, observations, decision.toolId(),
                        "已超过允许的最大工具调用次数上限 (" + context.termination().maxToolCalls() + ")。");
            }

            ReActObservation observation = invokeWithRetry(context, tool, decision, observations);
            observations.add(observation.toolId() + ": " + observation.content());

            ReActCheckpoint checkpoint = checkpoint(
                    context.runId(),
                    step,
                    toolCalls,
                    RunStatus.RUNNING,
                    observations,
                    ""
            );
            persist(checkpoint, checkpointSink);

            if (!observation.success() && !observation.retryable()) {
                return new ReActExecutionResult(RunStatus.FAILED, "", checkpoint, "工具执行遭遇不可重试的失败：" + observation.content());
            }
        }

        return result(RunStatus.FAILED, "", context.runId(), step, toolCalls, observations, "", "已超过允许的最大运行步骤上限 (" + context.budget().maxSteps() + ")。");
    }

    /** 工具失败包含自动重试的重试调用机制 */
    private ReActObservation invokeWithRetry(
            RuntimeContext context,
            ReActToolPort tool,
            ReActDecision decision,
            List<String> observations
    ) {
        ReActObservation last = null;
        for (int attempt = MIN_STEP; attempt <= RETRY_LIMIT; attempt++) {
            last = tool.invoke(context, decision.toolId(), decision.arguments());
            if (last.success() || !last.retryable()) {
                return last;
            }
            observations.add("工具 [" + decision.toolId() + "] 重试第 " + attempt + " 次：" + last.content());
        }
        return last;
    }

    /** 构建 ReActExecutionResult 辅助方法 */
    private ReActExecutionResult result(
            RunStatus status,
            String answer,
            String runId,
            int step,
            int toolCalls,
            List<String> observations,
            String pendingToolId,
            String error
    ) {
        return new ReActExecutionResult(
                status,
                answer,
                checkpoint(runId, step, toolCalls, status, observations, pendingToolId),
                error
        );
    }

    /** 构建 ReActCheckpoint 辅助方法 */
    private ReActCheckpoint checkpoint(
            String runId,
            int step,
            int toolCalls,
            RunStatus status,
            List<String> observations,
            String pendingToolId
    ) {
        return new ReActCheckpoint(runId, step, toolCalls, status, observations, pendingToolId);
    }

    /** 触发检查点回调持久化 */
    private void persist(ReActCheckpoint checkpoint, Consumer<ReActCheckpoint> checkpointSink) {
        if (checkpointSink != null) {
            checkpointSink.accept(checkpoint);
        }
    }
}

