package com.acme.agentstudio.application.runtime.plan;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运行时 Plan 模式依赖 DAG 校验与步骤拓扑编排引擎（Plan Engine）。
 * 负责 PlanInstance 计划的创建（create）、合法性拓扑校验（validate：检测重复 Step ID 与依赖死锁/循环依赖 detectCycle）、
 * 筛选处于准备就绪状态的就绪步骤（readySteps：依赖全部 SUCCEEDED 且自身 PENDING 的步骤）、
 * 局部更新步骤（apply）以及动态重新规划（replan 版本号增加 revision + 1）。
 */
@Component
public class PlanEngine {

    /**
     * 创建并校验一个新的 PlanInstance 计划实例。
     *
     * @param planId 计划 ID
     * @param runId 关联的运行 ID
     * @param steps 包含的步骤列表 List&lt;PlanStep&gt;
     * @param replannable 是否支持后续重新规划动态调整
     * @return 校验通过的 PlanInstance 实例
     */
    public PlanInstance create(String planId, String runId, List<PlanStep> steps, boolean replannable) {
        PlanInstance plan = new PlanInstance(planId, runId, steps, 0, replannable);
        validate(plan);
        return plan;
    }

    /**
     * 对 PlanInstance 执行深度结构与依赖图合法性校验（检测步骤唯一性及循环依赖）。
     *
     * @param plan 计划实例 PlanInstance
     */
    public void validate(PlanInstance plan) {
        Map<String, PlanStep> steps = index(plan.steps());
        for (PlanStep step : plan.steps()) {
            for (String dependencyId : step.dependencyIds()) {
                if (!steps.containsKey(dependencyId)) {
                    throw new IllegalArgumentException("计划步骤 [" + step.stepId() + "] 依赖的步骤 [" + dependencyId + "] 在当前计划中不存在。");
                }
            }
            detectCycle(step.stepId(), steps, new HashSet<>(), new HashSet<>());
        }
    }

    /**
     * 筛选出当前满足所有依赖且状态为 PENDING 的可并发执行就绪步骤列表。
     *
     * @param plan 计划实例 PlanInstance
     * @return 就绪的步骤列表 List&lt;PlanStep&gt;
     */
    public List<PlanStep> readySteps(PlanInstance plan) {
        Map<String, PlanStep> steps = index(plan.steps());
        return plan.steps().stream()
                .filter(step -> step.status() == PlanStepStatus.PENDING)
                .filter(step -> step.dependencyIds().stream()
                        .allMatch(dependency -> steps.get(dependency).status() == PlanStepStatus.SUCCEEDED))
                .toList();
    }

    /**
     * 对现有计划更新增量步骤，并重新校验 DAG 合法性。
     *
     * @param plan 原始计划实例
     * @param updatedStep 待替换更新的步骤对象
     * @return 更新后的 PlanInstance 实例
     */
    public PlanInstance apply(PlanInstance plan, PlanStep updatedStep) {
        PlanInstance updated = plan.replace(Map.of(updatedStep.stepId(), updatedStep));
        validate(updated);
        return updated;
    }

    /**
     * 在允许 Replan 的前提下，使用全量新步骤进行替换重规划，并增加版本号 revision。
     *
     * @param plan 原始计划实例
     * @param replacementSteps 新步骤列表
     * @return 重新规划生成的 PlanInstance 实例
     */
    public PlanInstance replan(PlanInstance plan, List<PlanStep> replacementSteps) {
        if (!plan.replannable()) {
            throw new IllegalStateException("当前计划配置了禁止重新规划（replannable = false），无法执行 Replan 覆盖。");
        }

        PlanInstance updated = new PlanInstance(
                plan.planId(),
                plan.runId(),
                replacementSteps,
                plan.revision() + 1,
                true
        );
        validate(updated);
        return updated;
    }

    /** 建立步骤 ID 至 PlanStep 的索引 Map */
    private Map<String, PlanStep> index(List<PlanStep> steps) {
        Map<String, PlanStep> index = new HashMap<>();
        for (PlanStep step : steps) {
            if (index.put(step.stepId(), step) != null) {
                throw new IllegalArgumentException("计划步骤标识 [" + step.stepId() + "] 重复定义。");
            }
        }
        return index;
    }

    /** 深度优先搜索（DFS）检测步骤依赖图中是否存在循环依赖 */
    private void detectCycle(String stepId, Map<String, PlanStep> steps, Set<String> visiting, Set<String> visited) {
        if (visited.contains(stepId)) {
            return;
        }
        if (!visiting.add(stepId)) {
            throw new IllegalArgumentException("计划依赖存在成环/循环依赖风险，步骤 ID：" + stepId);
        }
        for (String dependencyId : steps.get(stepId).dependencyIds()) {
            detectCycle(dependencyId, steps, visiting, visited);
        }
        visiting.remove(stepId);
        visited.add(stepId);
    }
}

