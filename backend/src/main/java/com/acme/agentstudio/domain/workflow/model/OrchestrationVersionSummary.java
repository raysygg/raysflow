package com.acme.agentstudio.domain.workflow.model;

import java.time.LocalDateTime;

/**
 * 不可变发布版本摘要信息 Record（Orchestration Version Summary）。
 * 包含应用物理 ID、不可变 Version ID、版本序号、环境编码、发布状态、发布者用户 ID、发布时间戳、冻结的 Graph JSON、版本 Hash 及是否为活动（Active）生产版本。
 *
 * @param appId 对应的应用主键 ID
 * @param versionId 不可变发布版本 ID
 * @param versionNo 版本序号
 * @param environmentCode 部署环境编码
 * @param status 发布版本状态
 * @param releasedBy 发布操作人用户 ID
 * @param releasedAt 发布时间
 * @param graphJson 冻结的版本拓扑 JSON
 * @param releaseBundleHash 发布包防篡改 Hash 签名
 * @param current 是否为当前活动生产版本
 */
public record OrchestrationVersionSummary(
        Long appId,
        String versionId,
        Integer versionNo,
        String environmentCode,
        String status,
        Long releasedBy,
        LocalDateTime releasedAt,
        String graphJson,
        String releaseBundleHash,
        boolean current
) {
}

