package com.acme.agentstudio.application.runtime.react;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;

import java.util.List;

/**
 * 运行时 ReAct 架构 LLM 模型推理适配端口接口（ReAct Model Port）。
 * 解耦具体的模型供应商与 LangChain4j / HTTP 客户端，
 * 仅基于 RuntimeContext 与历史 ReActObservation 观察回执计算产生单步结构化决策 ReActDecision。
 */
@FunctionalInterface
public interface ReActModelPort {

    /**
     * 根据当前运行上下文与累积的观察回执进行推理，产生单步 ReAct 决策（ToolCall 或 FinalAnswer）。
     *
     * @param context 运行上下文 RuntimeContext
     * @param observations 迄今为止累积的工具观察回执列表 List&lt;String&gt;
     * @return ReAct 推理决策对象 ReActDecision
     */
    ReActDecision decide(RuntimeContext context, List<String> observations);
}

