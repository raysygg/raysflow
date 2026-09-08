package com.acme.agentstudio.application.runtime.react;

/**
 * 运行时 ReAct 循环中的工具观察回执 Record（ReAct Observation）。
 *
 * @param toolId 执行的工具 ID
 * @param success 是否执行成功
 * @param content 工具返回的内容摘要或错误提示信息
 * @param retryable 失败时是否允许进行自动重试
 */
public record ReActObservation(String toolId, boolean success, String content, boolean retryable) {

    /**
     * 紧凑构造函数校验参数。
     */
    public ReActObservation {
        if (toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException("创建工具观察回执时，工具标识 toolId 不能为空。");
        }
        content = (content == null) ? "" : content;
    }
}

