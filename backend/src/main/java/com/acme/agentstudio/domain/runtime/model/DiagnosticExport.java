package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 脱敏后的系统诊断与健康度导出一体化打包 Record（Diagnostic Export）。
 * 导出 ID exportId、租户 ID tenantId、脱敏指标 metrics、组件健康快照 health、生成时间 createdAt 及失效时间 expiresAt，严禁包含私钥凭证。
 *
 * @param exportId 导出记录唯一 ID
 * @param tenantId 租户物理 ID
 * @param metrics 脱敏后的诊断指标 Map
 * @param health 运维健康度状态 Map
 * @param createdAt 包生成时间
 * @param expiresAt 文件过期时间
 */
public record DiagnosticExport(
        String exportId,
        long tenantId,
        Map<String, Object> metrics,
        Map<String, Object> health,
        Instant createdAt,
        Instant expiresAt
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public DiagnosticExport {
        if (exportId == null || exportId.isBlank() || tenantId <= 0 || createdAt == null || expiresAt == null) {
            throw new IllegalArgumentException("诊断导出参数无效");
        }
        metrics = (metrics == null) ? Map.of() : Map.copyOf(metrics);
        health = (health == null) ? Map.of() : Map.copyOf(health);
    }
}

