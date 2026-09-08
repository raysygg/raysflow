package com.acme.agentstudio.interfaces.rest.dto;

/**
 * OrchestrationRollback 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 OrchestrationRollback 操作参数。
 */
/**
 * 回滚工作流/Agent 到历史发布版本请求对象。
 *
 * @param versionId 目标历史版本的全局唯一标识 ID
 * @param environmentCode 目标环境编码
 * @param reason 回滚原因与备注说明
 */
/**
 * OrchestrationRollback 业务请求数据传输对象 (DTO)。
 */
public record OrchestrationRollbackRequest(
        String versionId,
        String environmentCode,
        String reason
) {
}

