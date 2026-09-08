package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;

/**
 * 一次应用版本灰度全量发布过程记录实体 Record（Release Rollout Record）。
 * 包含应用 ID applicationId、当前候选发布版本 ID releaseId、前一稳定版本 ID previousReleaseId、
 * 当前灰度状态 status (RolloutStatus)、当前切流流量比例 trafficPercentage (0~100) 与更新时刻 updatedAt。
 *
 * @param applicationId 绑定的应用唯一 ID
 * @param releaseId 当前推流发布的 Release ID
 * @param previousReleaseId 上一稳定状态的基线 Release ID
 * @param status 当前灰度流程状态（RolloutStatus）
 * @param trafficPercentage 承接线上真实流量的百分比（0 ~ 100）
 * @param updatedAt 最近一次调切流量或状态变更的时间
 */
public record ReleaseRolloutRecord(
        String applicationId,
        String releaseId,
        String previousReleaseId,
        RolloutStatus status,
        int trafficPercentage,
        Instant updatedAt
) {
    /** 紧凑构造函数做输入验证校验 */
    public ReleaseRolloutRecord {
        if (applicationId == null || applicationId.isBlank() || releaseId == null || releaseId.isBlank()
                || status == null || trafficPercentage < 0 || trafficPercentage > 100) {
            throw new IllegalArgumentException("发布记录参数无效");
        }
        updatedAt = (updatedAt == null) ? Instant.now() : updatedAt;
    }
}

