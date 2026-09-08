package com.acme.agentstudio.domain.workflow.model;

import java.time.LocalDateTime;

/**
 * 工作流应用摘要信息 Record（Orchestration App Summary）。
 * 包含应用主键 ID、slug 唯一编码、应用名称、拓扑图类型、草稿修订号、发布状态、环境编码以及活动生产版本号与发布时间。
 *
 * @param id 应用主键 ID
 * @param code 应用编码 slug
 * @param name 应用展示名称
 * @param graphType 拓扑图类型
 * @param revision 最新草稿修订版本号
 * @param status 应用状态
 * @param environmentCode 部署环境编码
 * @param currentVersionId 当前活动 Release 版本 ID
 * @param currentVersionNo 当前活动 Release 版本号
 * @param currentVersionReleasedAt 当前版本发布时间
 */
public record OrchestrationAppSummary(
        Long id,
        String code,
        String name,
        String graphType,
        Integer revision,
        String status,
        String environmentCode,
        String currentVersionId,
        Integer currentVersionNo,
        LocalDateTime currentVersionReleasedAt
) {
}

