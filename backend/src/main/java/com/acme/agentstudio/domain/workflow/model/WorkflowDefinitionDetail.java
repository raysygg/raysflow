package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流编排定义完整详情 Record（Workflow Definition Detail）。
 *
 * @param id 工作流唯一 ID
 * @param code 工作流编码 slug
 * @param name 工作流名称
 * @param version 版本序号
 * @param status 工作流状态
 * @param graphJson 节点与连线拓扑图 JSON 字符串
 */
public record WorkflowDefinitionDetail(
        Long id,
        String code,
        String name,
        Integer version,
        String status,
        String graphJson
) {
}

