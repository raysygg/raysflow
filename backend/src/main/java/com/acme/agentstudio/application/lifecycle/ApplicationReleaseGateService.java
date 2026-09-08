package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.config.ApplicationLifecycleProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EvaluationStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateCategory;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateFinding;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateLevel;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateReport;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleException;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEvaluationRunEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseCandidateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseGateFindingEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationReleaseGateReportEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEvaluationRunMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationReleaseGateFindingMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationReleaseGateReportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 应用生产发布质量门禁服务（Release Gate Service）。
 * 负责在将候选版本（Candidate）发布为正式线上版本之前，强制审查不可变快照结构完整性、依赖链完整性、脱敏安全、Runtime 生产就绪度以及绑定的评测报告（Evaluation Run），输出门禁缺陷（Blocker / Warning）与管理特权豁免（Override）。
 */
@Service
public class ApplicationReleaseGateService {

    /** 错误码：候选版本指纹不一致 */
    private static final String FINDING_FINGERPRINT = "CANDIDATE_FINGERPRINT_MISMATCH";

    /** 错误码：缺少评测证据 */
    private static final String FINDING_EVALUATION_REQUIRED = "EVALUATION_REQUIRED";

    /** 错误码：评测未通过 */
    private static final String FINDING_EVALUATION_FAILED = "EVALUATION_NOT_PASSED";

    /** 错误码：评测结果过期 */
    private static final String FINDING_EVALUATION_EXPIRED = "EVALUATION_EXPIRED";

    /** 错误码：快照结构无效 */
    private static final String FINDING_STRUCTURE = "STRUCTURE_INVALID";

    /** 错误码：依赖项未固定 */
    private static final String FINDING_DEPENDENCY = "DEPENDENCY_SNAPSHOT_MISSING";

    /** 错误码：成本证据缺失 */
    private static final String FINDING_COST = "COST_EVIDENCE_MISSING";

    /** 错误码：包含敏感凭据字段 */
    private static final String FINDING_SECURITY = "SENSITIVE_SNAPSHOT_FIELD";

    /** 错误码：Runtime 未生产就绪 */
    private static final String FINDING_RUNTIME = "RUNTIME_NOT_READY";

    /** 警告码：延迟偏高 */
    private static final String FINDING_LATENCY = "LATENCY_WARNING";

    /** 警告码：成本偏高 */
    private static final String FINDING_COST_LIMIT = "COST_WARNING";

    /** 评测任务 Persistence Mapper */
    private final ApplicationEvaluationRunMapper runMapper;

    /** 门禁报告 Persistence Mapper */
    private final ApplicationReleaseGateReportMapper reportMapper;

    /** 门禁缺陷发现项 Persistence Mapper */
    private final ApplicationReleaseGateFindingMapper findingMapper;

    /** 候选发布版本服务 */
    private final ReleaseCandidateService candidateService;

    /** 生命周期限额与超时配置 */
    private final ApplicationLifecycleProperties properties;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入门禁服务所需依赖组件。
     */
    public ApplicationReleaseGateService(ApplicationEvaluationRunMapper runMapper,
                                         ApplicationReleaseGateReportMapper reportMapper,
                                         ApplicationReleaseGateFindingMapper findingMapper,
                                         ReleaseCandidateService candidateService,
                                         ApplicationLifecycleProperties properties,
                                         ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.reportMapper = reportMapper;
        this.findingMapper = findingMapper;
        this.candidateService = candidateService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 对候选版本（Candidate）执行严苛的发布前综合门禁审查评估。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @param candidateFingerprint 候选快照指纹
     * @param evaluationRunId 绑定的评测任务 ID
     * @return 导出的门禁报告契约（包含总体通过等级与 Finding 缺陷明细）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GateReport evaluate(SecurityUser user, Long applicationId, Long candidateId,
                               String candidateFingerprint, Long evaluationRunId) {
        ApplicationReleaseCandidateEntity candidate = candidateService.requireCandidate(user, applicationId, candidateId);
        List<GateFinding> findings = new ArrayList<>();

        if (candidate.getSnapshotJson() == null || candidate.getSnapshotJson().isBlank()) {
            findings.add(new GateFinding(FINDING_STRUCTURE, GateCategory.STRUCTURE, GateLevel.BLOCKER,
                    "候选快照为空", "无法确认运行图和策略引用。", "snapshot missing", "candidate", false));
        } else {
            try {
                JsonNode snapshot = objectMapper.readTree(candidate.getSnapshotJson());
                if (snapshot.path("graph").isMissingNode() || snapshot.path("graph").isNull()) {
                    findings.add(new GateFinding(FINDING_STRUCTURE, GateCategory.STRUCTURE, GateLevel.BLOCKER,
                            "运行图缺失", "候选没有可执行的 Runtime 图。", "graph missing", "candidate", false));
                }
                if (snapshot.path("dependencyFingerprint").asText("").isBlank()) {
                    findings.add(new GateFinding(FINDING_DEPENDENCY, GateCategory.DEPENDENCY, GateLevel.BLOCKER,
                            "依赖快照缺失", "模型、知识或工具依赖未固定。", "dependency fingerprint missing", "dependencies", false));
                }
                if (containsSensitiveField(snapshot)) {
                    findings.add(new GateFinding(FINDING_SECURITY, GateCategory.SECURITY, GateLevel.BLOCKER,
                            "候选快照包含敏感字段", "凭证或连接信息不得进入候选快照。", "sensitive field", "candidate", false));
                }
                String graphType = snapshot.path("graph").path("graphType").asText("");
                if ("MULTI_AGENT".equalsIgnoreCase(graphType) || "REACT".equalsIgnoreCase(graphType)
                        || "PLAN".equalsIgnoreCase(graphType)) {
                    findings.add(new GateFinding(FINDING_RUNTIME, GateCategory.RUNTIME, GateLevel.BLOCKER,
                            "Runtime 尚未达到生产就绪", "当前 Runtime 只能用于草稿测试。", "runtime readiness", "runtime", false));
                }
            } catch (Exception exception) {
                findings.add(new GateFinding(FINDING_STRUCTURE, GateCategory.STRUCTURE, GateLevel.BLOCKER,
                        "候选快照格式无效", "候选快照无法解析。", "snapshot parse failed", "candidate", false));
            }
        }

        if (!candidate.getSnapshotFingerprint().equals(candidateFingerprint)) {
            findings.add(blocker(FINDING_FINGERPRINT, "候选指纹不一致", "请求指纹与服务端快照不一致。", "candidate"));
        }

        ApplicationEvaluationRunEntity run = evaluationRunId == null ? null : runMapper.selectOne(new LambdaQueryWrapper<ApplicationEvaluationRunEntity>()
                .eq(ApplicationEvaluationRunEntity::getTenantId, user.getTenantId())
                .eq(ApplicationEvaluationRunEntity::getApplicationId, applicationId)
                .eq(ApplicationEvaluationRunEntity::getId, evaluationRunId));

        if (run == null) {
            findings.add(blocker(FINDING_EVALUATION_REQUIRED, "缺少有效评测", "发布必须绑定评测任务。", "evaluation"));
        } else {
            if (!candidate.getId().equals(run.getCandidateId()) || !candidate.getSnapshotFingerprint().equals(run.getCandidateFingerprint())) {
                findings.add(blocker(FINDING_FINGERPRINT, "评测候选不匹配", "评测结果不属于当前候选快照。", "evaluation"));
            }
            if (!EvaluationStatus.SUCCEEDED.name().equals(run.getEvaluationStatus())) {
                findings.add(blocker(FINDING_EVALUATION_FAILED, "评测未通过", "评测没有成功完成，不得作为发布证据。", "evaluation"));
            }
            if (run.getAggregateReportJson() == null || run.getAggregateReportJson().isBlank()) {
                findings.add(new GateFinding(FINDING_COST, GateCategory.COST, GateLevel.WARNING,
                        "成本证据缺失", "评测未返回可用的成本用量。", "cost unavailable", "evaluation", true));
            } else {
                addMeasurementFindings(run.getAggregateReportJson(), findings);
            }
            if (run.getCompletedAt() == null || run.getCompletedAt().isBefore(LocalDateTime.now().minusDays(properties.getEvaluationValidityDays()))) {
                findings.add(blocker(FINDING_EVALUATION_EXPIRED, "评测已过期", "评测证据超过平台有效期。", "evaluation"));
            }
        }

        ApplicationReleaseGateReportEntity activeOverride = reportMapper.selectOne(
                new LambdaQueryWrapper<ApplicationReleaseGateReportEntity>()
                        .eq(ApplicationReleaseGateReportEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationReleaseGateReportEntity::getApplicationId, applicationId)
                        .eq(ApplicationReleaseGateReportEntity::getCandidateId, candidateId)
                        .eq(ApplicationReleaseGateReportEntity::getCandidateFingerprint, candidateFingerprint)
                        .eq(ApplicationReleaseGateReportEntity::getOverrideGranted, true)
                        .gt(ApplicationReleaseGateReportEntity::getOverrideExpiresAt, LocalDateTime.now())
                        .orderByDesc(ApplicationReleaseGateReportEntity::getEvaluatedAt).last("LIMIT 1"));

        boolean overrideApplicable = activeOverride != null
                && findings.stream().filter(item -> item.level() == GateLevel.BLOCKER).allMatch(GateFinding::overridable);

        GateLevel level = !overrideApplicable && findings.stream().anyMatch(item -> item.level() == GateLevel.BLOCKER)
                ? GateLevel.BLOCKER
                : findings.stream().anyMatch(item -> item.level() == GateLevel.WARNING) ? GateLevel.WARNING : GateLevel.PASSED;

        ApplicationReleaseGateReportEntity report = new ApplicationReleaseGateReportEntity();
        report.setTenantId(user.getTenantId());
        report.setApplicationId(applicationId);
        report.setCandidateId(candidateId);
        report.setCandidateFingerprint(candidateFingerprint);
        report.setEvaluationRunId(evaluationRunId);
        report.setOverallLevel(level.name());
        report.setOverrideGranted(overrideApplicable);

        if (overrideApplicable) {
            report.setOverrideReason(activeOverride.getOverrideReason());
            report.setOverrideScopeJson(activeOverride.getOverrideScopeJson());
            report.setOverrideBy(activeOverride.getOverrideBy());
            report.setOverrideExpiresAt(activeOverride.getOverrideExpiresAt());
        }

        report.setEvaluatedBy(user.getUserId());
        report.setEvaluatedAt(LocalDateTime.now());
        reportMapper.insert(report);

        for (GateFinding finding : findings) {
            ApplicationReleaseGateFindingEntity entity = new ApplicationReleaseGateFindingEntity();
            entity.setTenantId(user.getTenantId());
            entity.setApplicationId(applicationId);
            entity.setGateReportId(report.getId());
            entity.setFindingCode(finding.code());
            entity.setCategory(finding.category().name());
            entity.setFindingLevel(finding.level().name());
            entity.setTitle(finding.title());
            entity.setReason(finding.reason());
            entity.setEvidenceSummary(finding.evidenceSummary());
            entity.setRemediationTarget(finding.remediationTarget());
            entity.setOverridable(finding.overridable());
            entity.setCreatedAt(LocalDateTime.now());
            findingMapper.insert(entity);
        }

        if (ApplicationLifecycleContracts.CandidateStatus.EVALUATING.name().equals(candidate.getCandidateStatus())) {
            candidateService.transition(user, applicationId, candidateId, ApplicationLifecycleContracts.CandidateStatus.EVALUATING,
                    level == GateLevel.BLOCKER ? ApplicationLifecycleContracts.CandidateStatus.BLOCKED : ApplicationLifecycleContracts.CandidateStatus.READY);
        }
        return new GateReport(report.getId(), candidateId, candidateFingerprint, level, findings, report.getEvaluatedAt());
    }

    /**
     * 管理员授予发布门禁特权豁免（Override）。
     *
     * @param user 当前登录用户（需具备 ADMIN 或 OWNER 角色）
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @param reason 豁免审批事由说明
     * @param scopeJson 豁免影响范围控制
     * @param expiresAt 豁免到期时间
     * @return 豁免后的门禁报告
     */
    @Transactional
    public GateReport override(SecurityUser user, Long applicationId, Long candidateId, String reason,
                               String scopeJson, LocalDateTime expiresAt) {
        if (user == null || !(user.hasRole("ADMIN") || user.hasRole("OWNER"))) {
            throw new ApplicationLifecycleException("OVERRIDE_FORBIDDEN", "当前用户没有发布门禁特权豁免权限。");
        }
        if (reason == null || reason.isBlank() || expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            throw new ApplicationLifecycleException("OVERRIDE_INVALID", "豁免申请必须提供明确的原因和未来的到期时间。");
        }
        GateReport current = latest(user, applicationId, candidateId);
        if (current.findings().stream().anyMatch(item -> item.level() == GateLevel.BLOCKER && !item.overridable())) {
            throw new ApplicationLifecycleException("OVERRIDE_NOT_ALLOWED", "当前门禁包含不可豁免的结构或安全阻断项（Non-overridable Blocker）。");
        }
        ApplicationReleaseGateReportEntity report = reportMapper.selectById(current.reportId());
        report.setOverrideGranted(true);
        report.setOverrideReason(reason.trim());
        report.setOverrideScopeJson(scopeJson);
        report.setOverrideBy(user.getUserId());
        report.setOverrideExpiresAt(expiresAt);
        report.setOverallLevel(GateLevel.PASSED.name());
        reportMapper.updateById(report);
        return new GateReport(report.getId(), candidateId, report.getCandidateFingerprint(), GateLevel.PASSED,
                current.findings(), report.getEvaluatedAt());
    }

    /**
     * 获取候选版本最新生成的门禁审查报告。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId 候选版本 ID
     * @return 最新的门禁报告契约
     */
    public GateReport latest(SecurityUser user, Long applicationId, Long candidateId) {
        candidateService.requireCandidate(user, applicationId, candidateId);
        ApplicationReleaseGateReportEntity report = reportMapper.selectOne(new LambdaQueryWrapper<ApplicationReleaseGateReportEntity>()
                .eq(ApplicationReleaseGateReportEntity::getTenantId, user.getTenantId())
                .eq(ApplicationReleaseGateReportEntity::getApplicationId, applicationId)
                .eq(ApplicationReleaseGateReportEntity::getCandidateId, candidateId)
                .orderByDesc(ApplicationReleaseGateReportEntity::getEvaluatedAt).last("LIMIT 1"));
        if (report == null) {
            throw new ApplicationLifecycleException("GATE_NOT_FOUND", "该候选版本尚未生成门禁审查报告。");
        }
        List<GateFinding> findings = findingMapper.selectList(new LambdaQueryWrapper<ApplicationReleaseGateFindingEntity>()
                        .eq(ApplicationReleaseGateFindingEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationReleaseGateFindingEntity::getApplicationId, applicationId)
                        .eq(ApplicationReleaseGateFindingEntity::getGateReportId, report.getId()))
                .stream().map(item -> new GateFinding(item.getFindingCode(), GateCategory.valueOf(item.getCategory()),
                        GateLevel.valueOf(item.getFindingLevel()), item.getTitle(), item.getReason(), item.getEvidenceSummary(),
                        item.getRemediationTarget(), Boolean.TRUE.equals(item.getOverridable()))).toList();
        return new GateReport(report.getId(), candidateId, report.getCandidateFingerprint(), GateLevel.valueOf(report.getOverallLevel()), findings, report.getEvaluatedAt());
    }

    /**
     * 构造 Blocker 级别的门禁 Finding 工厂方法。
     */
    private GateFinding blocker(String code, String title, String reason, String remediation) {
        boolean overridable = FINDING_EVALUATION_REQUIRED.equals(code)
                || FINDING_EVALUATION_FAILED.equals(code)
                || FINDING_EVALUATION_EXPIRED.equals(code);
        return new GateFinding(code, GateCategory.EVALUATION, GateLevel.BLOCKER, title, reason, "已执行服务端绑定校验。", remediation, overridable);
    }

    /**
     * 解析评测报告指标并生成延迟与成本警告（Warning Findings）。
     */
    private void addMeasurementFindings(String reportJson, List<GateFinding> findings) {
        try {
            JsonNode report = objectMapper.readTree(reportJson);
            if (report.path("averageLatencyMs").asDouble(0D) > properties.getWarningAverageLatencyMs()) {
                findings.add(new GateFinding(FINDING_LATENCY, GateCategory.LATENCY, GateLevel.WARNING,
                        "平均延迟偏高", "评测平均延迟超过平台提示阈值。", "latency measured", "performance", true));
            }
            if (report.path("estimatedCost").asDouble(0D) > properties.getWarningAverageCost()) {
                findings.add(new GateFinding(FINDING_COST_LIMIT, GateCategory.COST, GateLevel.WARNING,
                        "评测成本偏高", "评测估算成本超过平台提示阈值。", "cost measured", "cost", true));
            }
        } catch (Exception ignored) {
            findings.add(new GateFinding(FINDING_COST, GateCategory.COST, GateLevel.WARNING,
                    "成本证据无法解析", "评测聚合摘要格式不完整。", "aggregate report invalid", "evaluation", true));
        }
    }

    /**
     * 递归检查 JsonNode 中是否泄漏 API Key / Secret / Password / Token 敏感字段。
     */
    private boolean containsSensitiveField(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey().toLowerCase(Locale.ROOT);
                if (name.contains("apikey") || name.contains("secret") || name.contains("password")
                        || name.contains("token") || name.contains("credential")) {
                    return true;
                }
                if (containsSensitiveField(entry.getValue())) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                if (containsSensitiveField(item)) {
                    return true;
                }
            }
        }
        return false;
    }
}
