package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 可追溯审计的企业合规审计证据 Record（Compliance Evidence）。
 * 记录证据 ID evidenceId、租户 ID tenantId、证据类型 evidenceType、操作人 actorId、关联资源 ID resourceId、详细信息 details 与记录时间 recordedAt。
 *
 * @param evidenceId 证据唯一标识编号
 * @param tenantId 归属租户物理 ID
 * @param evidenceType 证据类型分类编码
 * @param actorId 操作主体人/系统标识 ID
 * @param resourceId 关联操作的目标资源标识 ID
 * @param details 证据扩展详细参数 Map
 * @param recordedAt 审计存证记录时间
 */
public record ComplianceEvidence(
        String evidenceId,
        long tenantId,
        String evidenceType,
        String actorId,
        String resourceId,
        Map<String, Object> details,
        Instant recordedAt
) {
    /** 紧凑构造函数做输入属性断言 */
    public ComplianceEvidence {
        if (evidenceId == null || evidenceId.isBlank() || tenantId <= 0 || evidenceType == null || evidenceType.isBlank()
                || actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("合规证据标识、租户、类型和操作者不能为空。");
        }
        details = (details == null) ? Map.of() : Map.copyOf(details);
        recordedAt = (recordedAt == null) ? Instant.now() : recordedAt;
    }
}

