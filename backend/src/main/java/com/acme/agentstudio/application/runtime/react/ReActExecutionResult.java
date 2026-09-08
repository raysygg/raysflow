package com.acme.agentstudio.application.runtime.react;

import com.acme.agentstudio.domain.runtime.model.RunStatus;

/**
 * 运行时 ReAct 循环引擎最终输出结果 Record（ReAct Execution Result）。
 *
 * @param status 运行最终状态 RunStatus（SUCCEEDED / FAILED / CANCELLED / WAITING_HUMAN）
 * @param answer 生成的最终回答文本内容
 * @param checkpoint 当前的 ReAct 检查点快照
 * @param errorMessage 失败或挂起原因说明
 */
public record ReActExecutionResult(RunStatus status, String answer, ReActCheckpoint checkpoint, String errorMessage) {

    /**
     * 紧凑构造函数校验参数合规性。
     */
    public ReActExecutionResult {
        if (status == null) {
            throw new IllegalArgumentException("创建 ReAct 执行结果时，运行状态 status 不能为空。");
        }
        answer = (answer == null) ? "" : answer;
        errorMessage = (errorMessage == null) ? "" : errorMessage;
    }
}

