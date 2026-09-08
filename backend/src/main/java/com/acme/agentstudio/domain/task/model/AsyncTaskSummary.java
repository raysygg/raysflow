package com.acme.agentstudio.domain.task.model;

import java.time.LocalDateTime;

/**
 * 异步后台处理任务摘要信息 Record（Async Task Summary）。
 * 包含异步任务 ID、任务类型 taskType、状态 status、重试与最大重试次数、预计执行时间、创建/完成时间以及异常提示，脱敏避开敏感路径。
 *
 * @param id 异步任务主键物理 ID
 * @param taskType 任务类型编码
 * @param status 任务状态
 * @param retryCount 已重试次数
 * @param maxRetries 最大重试次数
 * @param availableAt 调度可用时间
 * @param createdAt 任务创建时间
 * @param finishedAt 任务完成时间
 * @param errorMessage 失败时的错误信息文案
 */
public record AsyncTaskSummary(
        Long id,
        String taskType,
        String status,
        Integer retryCount,
        Integer maxRetries,
        LocalDateTime availableAt,
        LocalDateTime createdAt,
        LocalDateTime finishedAt,
        String errorMessage
) {
}

