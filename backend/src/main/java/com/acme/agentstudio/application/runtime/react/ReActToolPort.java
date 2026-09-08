package com.acme.agentstudio.application.runtime.react;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;

import java.util.Map;

/**
 * 运行时 ReAct 循环中的工具执行适配端口接口（ReAct Tool Port）。
 * 解耦内部 MCP 工具、外部 Webhook 与系统内置 Tool 连接器，
 * 执行工具调用并返回标准的观察回执 ReActObservation。
 */
@FunctionalInterface
public interface ReActToolPort {

    /**
     * 根据 Tool ID 与参数 Map 触发工具执行，返回观察结果 ReActObservation。
     *
     * @param context 运行上下文 RuntimeContext
     * @param toolId 工具 ID
     * @param arguments 工具调用入参 Map
     * @return 工具观察回执 ReActObservation
     */
    ReActObservation invoke(RuntimeContext context, String toolId, Map<String, Object> arguments);
}

