package com.acme.agentstudio.domain.application.model;

/**
 * 新建 Agent/Workflow 应用及其初始草稿版本创建结果实体 Record（Application Draft Result）。
 * 包含应用物理 ID id、应用唯一编码 code、应用名称 name、状态 status 及默认工作流是否一并自动初始化创建标志 defaultWorkflowCreated。
 *
 * @param id 应用物理 ID
 * @param code 应用业务编码
 * @param name 应用展示名称
 * @param status 初始生命周期状态（DRAFT）
 * @param defaultWorkflowCreated 是否自动建立了关联的主 Workflow 画布节点
 */
public record ApplicationDraftResult(
        Long id,
        String code,
        String name,
        String status,
        boolean defaultWorkflowCreated
) {
}

