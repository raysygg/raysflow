package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 针对故障排查与全链路 Debug 历史 Run 任务的完全确定性 Replay 重放快照 Record（Runtime Replay Snapshot）。
 * 保存捕获时刻的输入 input、Prompt 快照 prompt、知识库快照 knowledge、工具模型快照 tools、
 * 路由策略 policy 与捕获时间 capturedAt，确保重放调试完全脱离当下实时发布的破坏性改动。
 *
 * @param runId 原 Run 任务 ID
 * @param releaseId 原强绑定的 Release ID
 * @param input 原始触发输入 Map
 * @param prompt 原始 Prompt 模板与变量快照 Map
 * @param knowledge 原始知识库检索快照 Map
 * @param tools 原始工具定义快照 Map
 * @param policy 原始治理策略快照 Map
 * @param capturedAt 快照捕获时间
 */
public record RuntimeReplaySnapshot(
        String runId,
        String releaseId,
        Map<String, Object> input,
        Map<String, Object> prompt,
        Map<String, Object> knowledge,
        Map<String, Object> tools,
        Map<String, Object> policy,
        Instant capturedAt
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public RuntimeReplaySnapshot {
        if (runId == null || runId.isBlank() || releaseId == null || releaseId.isBlank()) {
            throw new IllegalArgumentException("重放快照的 Run 和发布版本不能为空");
        }
        input = (input == null) ? Map.of() : Map.copyOf(input);
        prompt = (prompt == null) ? Map.of() : Map.copyOf(prompt);
        knowledge = (knowledge == null) ? Map.of() : Map.copyOf(knowledge);
        tools = (tools == null) ? Map.of() : Map.copyOf(tools);
        policy = (policy == null) ? Map.of() : Map.copyOf(policy);
        capturedAt = (capturedAt == null) ? Instant.now() : capturedAt;
    }
}

