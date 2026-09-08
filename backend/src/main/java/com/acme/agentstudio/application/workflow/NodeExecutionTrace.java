package com.acme.agentstudio.application.workflow;

/**
 * 模型节点与 AI 节点向工作流调度器返回的可观测 Trace 追溯信息 Record。
 * 记录本次节点执行实际调用的模型 Key、重试尝试次数以及消费的检索上下文条目数量。
 *
 * @param modelKey 实际调用的模型 Key 标识
 * @param attempt 重试与尝试序号
 * @param contextUsage 召回或使用的上下文数量
 */
public record NodeExecutionTrace(String modelKey, int attempt, int contextUsage) {
}

