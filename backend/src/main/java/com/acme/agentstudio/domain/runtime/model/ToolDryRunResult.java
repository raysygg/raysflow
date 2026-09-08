package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;

/**
 * 外部 API 与工具预检 Dry Run 试运行 Mock 模拟结果 Record（Tool Dry-Run Result）。
 * 明确记录工具 ID toolId、执行模式 mode (必须为 ToolExecutionMode.DRY_RUN)、
 * 真实副作用是否已跳过 sideEffectSkipped (必须为 true) 以及 Mock 模拟输出结果 Map simulatedOutput。
 *
 * @param toolId 被调用的工具唯一 ID
 * @param mode 工具执行模式（固定为 ToolExecutionMode.DRY_RUN）
 * @param sideEffectSkipped 真实网络请求与副作用写操作是否已跳过
 * @param simulatedOutput Mock 沙箱返回的模拟响应数据 Map
 */
public record ToolDryRunResult(
        String toolId,
        ToolExecutionMode mode,
        boolean sideEffectSkipped,
        Map<String, Object> simulatedOutput
) {
    /** 紧凑构造函数做 Dry Run 安全规则校验 */
    public ToolDryRunResult {
        if (toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException("工具标识不能为空");
        }
        if (mode != ToolExecutionMode.DRY_RUN || !sideEffectSkipped) {
            throw new IllegalArgumentException("模拟结果必须使用 DRY_RUN 且跳过副作用");
        }
        simulatedOutput = (simulatedOutput == null) ? Map.of() : Map.copyOf(simulatedOutput);
    }
}

