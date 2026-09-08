package com.acme.agentstudio.application.runtime.react;

import java.util.Map;

/**
 * 运行时 ReAct 模型推理决策结果 Record（ReAct Decision）。
 * 模型单步推理输出为两种决策类型之一：工具调用（TOOL_CALL）或最终回答（FINAL_ANSWER）。
 *
 * @param type 决策类型 DecisionType
 * @param text 最终回答文本内容（当 type 为 FINAL_ANSWER 时有效）
 * @param toolId 工具 ID（当 type 为 TOOL_CALL 时必须提供）
 * @param arguments 工具调用入参 Map
 * @param requiresHumanApproval 是否需要人工确认批准后再触发执行
 */
public record ReActDecision(
        DecisionType type,
        String text,
        String toolId,
        Map<String, Object> arguments,
        boolean requiresHumanApproval
) {
    /** 决策类型枚举 */
    public enum DecisionType {
        /** 发起工具调用 */
        TOOL_CALL,

        /** 产生最终回答结单 */
        FINAL_ANSWER
    }

    /**
     * 紧凑构造函数校验决策类型与必要属性。
     */
    public ReActDecision {
        if (type == null) {
            throw new IllegalArgumentException("创建 ReAct 决策时，决策类型 type 不能为空。");
        }

        text = (text == null) ? "" : text;
        toolId = (toolId == null) ? "" : toolId;
        arguments = (arguments == null) ? Map.of() : Map.copyOf(arguments);

        if (type == DecisionType.TOOL_CALL && toolId.isBlank()) {
            throw new IllegalArgumentException("类型为 TOOL_CALL 时，目标工具标识 toolId 不能为空。");
        }
    }

    /**
     * 静态工厂方法：构建 FINAL_ANSWER 最终回答决策。
     *
     * @param text 回答文本
     * @return 最终回答 ReActDecision 对象
     */
    public static ReActDecision finalAnswer(String text) {
        return new ReActDecision(DecisionType.FINAL_ANSWER, text, "", Map.of(), false);
    }

    /**
     * 静态工厂方法：构建 TOOL_CALL 工具调用决策。
     *
     * @param toolId 工具 ID
     * @param arguments 入参 Map
     * @param requiresHumanApproval 是否需要人工确认
     * @return 工具调用 ReActDecision 对象
     */
    public static ReActDecision toolCall(String toolId, Map<String, Object> arguments, boolean requiresHumanApproval) {
        return new ReActDecision(DecisionType.TOOL_CALL, "", toolId, arguments, requiresHumanApproval);
    }
}

