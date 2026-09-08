package com.acme.agentstudio.domain.runtime.model;

/**
 * 运行任务 Run 控制命令（暂停 PAUSE、恢复 RESUME、取消 CANCEL、重试 RETRY、重放 REPLAY）指令执行结果 Record（Run Control Result）。
 * 包含原 Run ID runId、操作后状态 status、执行动作 action (RunControlAction)、重试/重放所产生的新 Run ID targetRunId 及说明文案 message。
 *
 * @param runId 操作的目标源 Run ID
 * @param status 命令执行后的 Run 状态
 * @param action 执行的控制指令动作（RunControlAction）
 * @param targetRunId 如果是重试或重放动作，衍生出的全新目标 Run ID
 * @param message 控制操作结果的中文反馈提示
 */
public record RunControlResult(
        String runId,
        String status,
        RunControlAction action,
        String targetRunId,
        String message
) {
}

