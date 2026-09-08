package com.acme.agentstudio.domain.workflow.model;

import java.time.LocalDateTime;

/**
 * 工作流执行记录聚合摘要 Record（Workflow Execution Summary）。
 * 供运行监控与历史列表分页展现使用，不包含深层的节点级 Trace 明细（明细需单独调用详情接口）。
 *
 * @param id 执行主键物理 ID
 * @param sessionId 会话物理 ID
 * @param workflowCode 工作流编码
 * @param executionKey 执行 Key
 * @param status 执行状态
 * @param errorMessage 错误消息
 * @param startedAt 开始时间
 * @param finishedAt 完成时间
 */
public record WorkflowExecutionSummary(
        Long id,
        Long sessionId,
        String workflowCode,
        String executionKey,
        String status,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

