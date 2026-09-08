package com.acme.agentstudio.domain.knowledge.model;

/**
 * 知识库批量重建向量索引提交异步协调任务响应实体 Record（Knowledge Batch Reindex Submission）。
 * 提交后台批处理任务 taskId、当前任务状态 status 及提示响应消息 message。
 *
 * @param taskId 后台异步重建协调任务唯一 ID
 * @param status 任务调度状态（如 SUBMITTED, RUNNING）
 * @param message 提交结果提示文本
 */
public record KnowledgeBatchReindexSubmission(
        Long taskId,
        String status,
        String message
) {
}

