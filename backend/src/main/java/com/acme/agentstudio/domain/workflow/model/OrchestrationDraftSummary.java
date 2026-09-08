package com.acme.agentstudio.domain.workflow.model;

/**
 * 工作流编排草稿摘要与拓扑 JSON 数据实体 Record（Orchestration Draft Summary）。
 *
 * @param appId 对应的应用主键 ID
 * @param revisionId 草稿修订物理 ID
 * @param revisionNo 草稿修订版本序号
 * @param appCode 应用 slug 编码
 * @param appName 应用名称
 * @param graphType 拓扑图类型
 * @param status 草稿状态
 * @param graphJson 节点与连线拓扑图 JSON 字符串
 * @param baseVersionId 衍生基线版本 ID
 */
public record OrchestrationDraftSummary(
        Long appId,
        Long revisionId,
        Integer revisionNo,
        String appCode,
        String appName,
        String graphType,
        String status,
        String graphJson,
        String baseVersionId
) {
}

