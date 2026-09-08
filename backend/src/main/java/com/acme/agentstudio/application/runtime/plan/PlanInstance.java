package com.acme.agentstudio.application.runtime.plan;

import java.util.List;
import java.util.Map;

/**
 * 运行时 Plan 模式绑定的计划实例只读快照 Record（Plan Instance）。
 *
 * @param planId 计划 ID
 * @param runId 关联的 Run ID
 * @param steps 包含的步骤列表 List&lt;PlanStep&gt;
 * @param revision 计划版本修订号（每次 replace/replan 递增 +1）
 * @param replannable 是否允许动态重新规划
 */
public record PlanInstance(String planId, String runId, List<PlanStep> steps, int revision, boolean replannable) {

    /**
     * 紧凑构造函数校验非空与索引合法性。
     */
    public PlanInstance {
        if (planId == null || planId.isBlank() || runId == null || runId.isBlank() || revision < 0) {
            throw new IllegalArgumentException("创建计划实例时，planId 和 runId 均不能为空，且修订版本号 revision 不能为负数。");
        }
        steps = (steps == null) ? List.of() : List.copyOf(steps);
    }

    /**
     * 根据替换步骤 Map 创建一个升级版本号（revision + 1）的新 PlanInstance。
     *
     * @param replacements 待替换的步骤映射 Map&lt;String, PlanStep&gt;
     * @return 新生成的 PlanInstance 快照
     */
    public PlanInstance replace(Map<String, PlanStep> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return this;
        }

        List<PlanStep> updated = steps.stream()
                .map(step -> replacements.getOrDefault(step.stepId(), step))
                .toList();

        return new PlanInstance(planId, runId, updated, revision + 1, replannable);
    }
}

