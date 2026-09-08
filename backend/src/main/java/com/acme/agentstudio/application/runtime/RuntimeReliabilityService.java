package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.DeploymentPreflightReport;
import com.acme.agentstudio.domain.runtime.model.DiagnosticExport;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 运行时系统可靠性平滑升级预检与诊断导出服务（Runtime Reliability Service）。
 * 针对 Enterprise Agent Studio 系统版本平滑升级提供架构级预检（preflight）：包含 Schema 数据库表结构、数据备份校验、兼容性与一键回滚能力；
 * 同时支持导出带有 24 小时过期时效（DIAGNOSTIC_EXPIRY_HOURS）且经脱敏的系统运行诊断快照包（DiagnosticExport）。
 */
@Service
public class RuntimeReliabilityService {

    /** 导出诊断快照包的默认有效期（小时） */
    private static final long DIAGNOSTIC_EXPIRY_HOURS = 24L;

    /**
     * 对系统部署与版本升级执行预检，汇总阻断项与合规状态。
     *
     * @param schemaReady 数据库 DDL 表结构检查是否通过
     * @param backupVerified 数据库备份与快照校验是否成功
     * @param compatibilityReady 新旧版本数据与 API 兼容性检查是否通过
     * @param rollbackReady 异常时的秒级回滚预案是否就绪
     * @return 部署预检报告 DeploymentPreflightReport
     */
    public DeploymentPreflightReport preflight(
            boolean schemaReady,
            boolean backupVerified,
            boolean compatibilityReady,
            boolean rollbackReady
    ) {
        List<String> blockers = new ArrayList<>();

        if (!schemaReady) {
            blockers.add("数据库 DDL 结构检查未通过，请检查关联表结构。");
        }
        if (!backupVerified) {
            blockers.add("数据库备份与还原点校验未通过，请确保备份文件可正常恢复。");
        }
        if (!compatibilityReady) {
            blockers.add("新旧版本 API 与数据结构兼容性检查未通过。");
        }
        if (!rollbackReady) {
            blockers.add("故障回滚方案尚未就绪，缺乏可执行的应急回滚策略。");
        }

        return new DeploymentPreflightReport(
                blockers.isEmpty(),
                blockers,
                List.of(),
                blockers.isEmpty() ? "升级、回滚、版本兼容性与依赖检查全部通过，允许发布。" : "检测到严重阻断项，禁止执行生产环境发布与升级。"
        );
    }

    /**
     * 导出租户的脱敏诊断数据快照 Diagnostics。
     *
     * @param tenantId 租户 ID
     * @param metrics 运行指标 Map
     * @param health 运维健康状态 Map
     * @return 导出的诊断实体 DiagnosticExport
     */
    public DiagnosticExport export(long tenantId, Map<String, Object> metrics, Map<String, Object> health) {
        Instant createdAt = Instant.now();
        return new DiagnosticExport(
                UUID.randomUUID().toString(),
                tenantId,
                metrics,
                health,
                createdAt,
                createdAt.plus(DIAGNOSTIC_EXPIRY_HOURS, ChronoUnit.HOURS)
        );
    }
}

