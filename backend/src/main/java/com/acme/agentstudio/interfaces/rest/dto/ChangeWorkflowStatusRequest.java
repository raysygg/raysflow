package com.acme.agentstudio.interfaces.rest.dto;

/**
 * ChangeWorkflowStatus 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 ChangeWorkflowStatus 操作参数。
 */
/**
 * 变更工作流运行状态请求对象。
 *
 * @param tenantId 租户 ID
 * @param operatorId 操作人用户标识
 */
/**
 * ChangeWorkflowStatus 业务请求数据传输对象 (DTO)。
 */
public record ChangeWorkflowStatusRequest(
        Long tenantId,
        String operatorId
) {
}

