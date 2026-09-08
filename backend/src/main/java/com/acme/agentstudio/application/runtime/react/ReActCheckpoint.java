package com.acme.agentstudio.application.runtime.react;

import com.acme.agentstudio.domain.runtime.model.RunStatus;

import java.util.List;

/**
 * 运行时 ReAct 循环推理检查点 Record（ReAct Checkpoint）。
 * 保存单次思考-行动-观察（Reasoning-Action-Observation）迭代节点快照，供中途挂起恢复使用。
 *
 * @param runId 关联运行 ID
 * @param step 当前 ReAct 循环步数（stepCount）
 * @param toolCalls 累计已调用的工具次数
 * @param status 当前运行状态 RunStatus
 * @param observations 迄今为止累积的观察回执文本列表 List&lt;String&gt;
 * @param pendingToolId 当前已发起挂起等待人工确认的工具 ID（若有）
 */
public record ReActCheckpoint(
        String runId,
        int step,
        int toolCalls,
        RunStatus status,
        List<String> observations,
        String pendingToolId
) {
    /**
     * 紧凑构造函数校验参数合法性。
     */
    public ReActCheckpoint {
        if (runId == null || runId.isBlank() || step < 0 || toolCalls < 0 || status == null) {
            throw new IllegalArgumentException("创建 ReAct 检查点时，runId 不能为空，且步数 step 与工具数 toolCalls 不能为负数。");
        }
        observations = (observations == null) ? List.of() : List.copyOf(observations);
        pendingToolId = (pendingToolId == null) ? "" : pendingToolId;
    }
}

