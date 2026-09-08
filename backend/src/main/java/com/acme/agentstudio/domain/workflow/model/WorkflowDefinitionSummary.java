package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流编排定义摘要信息 Record（Workflow Definition Summary）。
 *
 * @param id 工作流唯一 ID
 * @param code 工作流编码 slug
 * @param name 工作流名称
 * @param version 版本序号
 * @param status 工作流状态
 */
public record WorkflowDefinitionSummary(
        Long id,
        String code,
        String name,
        Integer version,
        String status
) {
}

