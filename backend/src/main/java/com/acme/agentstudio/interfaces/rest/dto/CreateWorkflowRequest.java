package com.acme.agentstudio.interfaces.rest.dto;

/**
 * CreateWorkflow 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 CreateWorkflow 操作参数。
 */
/**
 * 创建工作流请求对象。
 *
 * @param tenantId 租户 ID
 * @param workflowCode 工作流唯一编码
 * @param workflowName 工作流展示名称
 * @param graphJson 画布节点与连线定义 JSON
 * @param operatorId 操作人用户标识
 */
/**
 * CreateWorkflow 业务请求数据传输对象 (DTO)。
 */
public record CreateWorkflowRequest(
        Long tenantId,
        String workflowCode,
        String workflowName,
        String graphJson,
        String operatorId
) {
}

