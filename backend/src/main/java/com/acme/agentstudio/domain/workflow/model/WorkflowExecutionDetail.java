package com.acme.agentstudio.domain.workflow.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流任务一次真实执行的完整链路详情 Record（Workflow Execution Detail）。
 * 包含工作流运行主键 ID、会话 ID、工作流编码、输入参数、执行 Key、当前状态、错误消息、
 * 启动时间、心跳维持时间、完成时间以及所有下辖节点的执行过程明细节点列表 List&lt;WorkflowExecutionNodeDetail&gt;。
 *
 * @param id 执行主键物理 ID
 * @param sessionId 会话物理 ID
 * @param workflowCode 工作流编码
 * @param inputMessage 输入消息参数 JSON 串
 * @param executionKey 唯一执行标识 Key
 * @param status 执行状态
 * @param errorMessage 失败时的异常信息文案
 * @param startedAt 开始执行时间
 * @param heartbeatAt 心跳刷新时间
 * @param finishedAt 完成时间
 * @param nodes 节点级执行明细列表
 */
public record WorkflowExecutionDetail(
        Long id,
        Long sessionId,
        String workflowCode,
        String inputMessage,
        String executionKey,
        String status,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime heartbeatAt,
        LocalDateTime finishedAt,
        List<WorkflowExecutionNodeDetail> nodes
) {
}

