package com.acme.agentstudio.application.runtime.plan;

import java.util.List;
import java.util.Map;

/**
 * 运行时 Plan 模式单项任务步骤实体 Record（Plan Step）。
 *
 * @param stepId 步骤唯一标识 ID
 * @param objective 该步骤要达成的目标说明文本
 * @param dependencyIds 依赖的前置步骤 ID 列表 List&lt;String&gt;
 * @param status 当前步骤状态 PlanStepStatus
 * @param attempt 已尝试重试次数
 * @param maxAttempts 最大允许尝试重试次数
 * @param input 步骤输入参数 Map
 * @param output 步骤输出或执行结果 Map
 * @param errorMessage 失败说明摘要
 */
public record PlanStep(
        String stepId,
        String objective,
        List<String> dependencyIds,
        PlanStepStatus status,
        int attempt,
        int maxAttempts,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage
) {
    /**
     * 紧凑构造函数校验参数合规性并初始化默认副本 Map。
     */
    public PlanStep {
        if (stepId == null || stepId.isBlank() || objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("创建计划步骤时，步骤标识 stepId 与目标文本 objective 均不能为空。");
        }
        if (status == null || attempt < 0 || maxAttempts < 1) {
            throw new IllegalArgumentException("创建计划步骤时，状态 status 不能为空，且 attempt 不能为负数，maxAttempts 必须至少为 1。");
        }

        dependencyIds = (dependencyIds == null) ? List.of() : List.copyOf(dependencyIds);
        input = (input == null) ? Map.of() : Map.copyOf(input);
        output = (output == null) ? Map.of() : Map.copyOf(output);
        errorMessage = (errorMessage == null) ? "" : errorMessage;
    }

    /**
     * 启动当前步骤（从 PENDING 转为 RUNNING，attempt 次数加 1）。
     *
     * @return 转换状态后的新 PlanStep
     */
    public PlanStep start() {
        if (status != PlanStepStatus.PENDING) {
            throw new IllegalStateException("只有处于 PENDING 待执行状态的步骤才能被启动，当前状态为：" + status);
        }
        return copy(PlanStepStatus.RUNNING, attempt + 1, output, "");
    }

    /**
     * 完成当前步骤并记入结果（从 RUNNING 转为 SUCCEEDED）。
     *
     * @param result 步骤执行产出的输出结果 Map
     * @return 转换状态后的新 PlanStep
     */
    public PlanStep succeed(Map<String, Object> result) {
        if (status != PlanStepStatus.RUNNING) {
            throw new IllegalStateException("只有处于 RUNNING 运行中状态的步骤才能标记成功完成，当前状态为：" + status);
        }
        return copy(PlanStepStatus.SUCCEEDED, attempt, result, "");
    }

    /**
     * 当前步骤执行失败，评估是否触发重试（重新转为 PENDING）或置为终态 FAILED。
     *
     * @param error 异常失败原因
     * @param retryable 错误类型是否允许自动重试
     * @return 转换状态后的新 PlanStep
     */
    public PlanStep fail(String error, boolean retryable) {
        if (status != PlanStepStatus.RUNNING) {
            throw new IllegalStateException("只有处于 RUNNING 运行中状态的步骤才能处理失败响应，当前状态为：" + status);
        }
        PlanStepStatus next = (retryable && attempt < maxAttempts) ? PlanStepStatus.PENDING : PlanStepStatus.FAILED;
        return copy(next, attempt, output, error);
    }

    /**
     * 当前步骤需要人工确认，转为 WAITING_HUMAN 挂起等待状态。
     *
     * @param reason 需要人工介入的原因说明
     * @return 转换状态后的新 PlanStep
     */
    public PlanStep waitForHuman(String reason) {
        if (status != PlanStepStatus.RUNNING) {
            throw new IllegalStateException("只有处于 RUNNING 运行中状态的步骤才能挂起等待人工确认，当前状态为：" + status);
        }
        return copy(PlanStepStatus.WAITING_HUMAN, attempt, output, reason);
    }

    /** 私有浅拷贝构建方法 */
    private PlanStep copy(PlanStepStatus next, int nextAttempt, Map<String, Object> nextOutput, String error) {
        return new PlanStep(
                stepId,
                objective,
                dependencyIds,
                next,
                nextAttempt,
                maxAttempts,
                input,
                nextOutput,
                error
        );
    }
}

