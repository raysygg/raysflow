package com.acme.agentstudio.domain.workflow.model;

import java.time.LocalDateTime;

/**
 * 单个工作流节点的节点级真实执行过程明细 Record（Workflow Execution Node Detail）。
 * 包含节点执行纪录物理 ID、节点 ID、节点类型、重试次数 attempt、状态、错误信息、输入/输出摘要、Trace 日志 JSON 以及起止时间。
 *
 * @param id 节点执行纪录 ID
 * @param nodeId 节点 ID
 * @param nodeType 节点类型编码
 * @param attempt 当前重试尝试次数
 * @param status 节点执行状态
 * @param errorMessage 失败异常文案
 * @param inputSummary 输入属性摘要
 * @param outputSummary 输出结果摘要
 * @param traceJson 节点级追踪 Trace JSON
 * @param startedAt 节点启动时间
 * @param finishedAt 节点完成时间
 */
public record WorkflowExecutionNodeDetail(
        Long id,
        String nodeId,
        String nodeType,
        Integer attempt,
        String status,
        String errorMessage,
        String inputSummary,
        String outputSummary,
        String traceJson,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

