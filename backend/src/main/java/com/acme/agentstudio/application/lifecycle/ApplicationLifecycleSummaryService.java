package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService.EvaluationRunSummary;
import com.acme.agentstudio.application.workflow.OrchestrationQueryService;
import com.acme.agentstudio.application.workflow.OrchestrationAuthorizationService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.EntrypointConfiguration;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.CandidateStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EvaluationStatus;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateLevel;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.LifecycleFinding;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.LifecycleNextAction;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.LifecycleSummary;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.ReleaseFact;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.EntrypointFact;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.RunFact;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleException;
import com.acme.agentstudio.domain.workflow.model.OrchestrationVersionSummary;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用生命周期只读聚合服务。
 * <p>只组合各领域服务已经确认的事实，不在前端复制候选、评测、门禁或发布状态流转。</p>
 */
@Service
public class ApplicationLifecycleSummaryService {

    private final ReleaseCandidateService candidateService;
    private final ApplicationEvaluationService evaluationService;
    private final ApplicationReleaseGateService gateService;
    private final ApplicationEntrypointService entrypointService;
    private final OrchestrationQueryService queryService;
    private final OrchestrationDraftRevisionMapper draftMapper;
    private final PlatformExecutionEventMapper eventMapper;
    private final OrchestrationAuthorizationService authorizationService;

    public ApplicationLifecycleSummaryService(ReleaseCandidateService candidateService,
                                              ApplicationEvaluationService evaluationService,
                                              ApplicationReleaseGateService gateService,
                                              ApplicationEntrypointService entrypointService,
                                              OrchestrationQueryService queryService,
                                              OrchestrationDraftRevisionMapper draftMapper,
                                              PlatformExecutionEventMapper eventMapper,
                                              OrchestrationAuthorizationService authorizationService) {
        this.candidateService = candidateService;
        this.evaluationService = evaluationService;
        this.gateService = gateService;
        this.entrypointService = entrypointService;
        this.queryService = queryService;
        this.draftMapper = draftMapper;
        this.eventMapper = eventMapper;
        this.authorizationService = authorizationService;
    }

    /**
     * 查询指定应用的租户隔离生命周期摘要。
     *
     * @param user 当前用户
     * @param applicationId 应用 ID
     * @return 生命周期摘要
     */
    public LifecycleSummary summarize(SecurityUser user, Long applicationId) {
        List<ApplicationLifecycleContracts.CandidateSummary> candidates = candidateService.list(user, applicationId);
        ApplicationLifecycleContracts.CandidateSummary candidate = candidates.isEmpty() ? null : candidates.get(0);
        EvaluationRunSummary evaluation = latestEvaluation(user, applicationId);
        ApplicationLifecycleContracts.GateReport gate = latestGate(user, applicationId, candidate);
        List<OrchestrationVersionSummary> versions = queryService.versions(user, applicationId);
        OrchestrationVersionSummary active = versions.stream().filter(OrchestrationVersionSummary::current).findFirst().orElse(null);
        List<EntrypointFact> entrypoints = entrypointService.list(user, applicationId).stream().map(this::entrypoint).toList();
        List<RunFact> recentRuns = recentRuns(user, applicationId);

        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationDraftRevisionEntity::getAppId, applicationId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo).last("LIMIT 1"));
        Integer revision = draft == null ? null : draft.getRevisionNo();

        List<LifecycleFinding> findings = findings(applicationId, candidate, evaluation, gate, active, entrypoints, revision);
        List<LifecycleNextAction> nextActions = nextActions(user, applicationId, candidate, evaluation, gate, active, revision);
        ReleaseFact release = active == null ? null : new ReleaseFact(active.versionId(), active.versionNo(),
                active.environmentCode(), active.releaseBundleHash(), active.releasedAt());
        return new LifecycleSummary(revision, candidate, evaluation == null ? null : new ApplicationLifecycleContracts.EvaluationRunFact(
                evaluation.runId(), evaluation.status(), evaluation.createdAt(), evaluation.completedAt(), evaluation.failureMessage()),
                gate, release, entrypoints, recentRuns, findings, nextActions);
    }

    private EvaluationRunSummary latestEvaluation(SecurityUser user, Long applicationId) {
        return evaluationService.listRuns(user, applicationId).stream().findFirst().orElse(null);
    }

    private ApplicationLifecycleContracts.GateReport latestGate(SecurityUser user, Long applicationId,
                                                                 ApplicationLifecycleContracts.CandidateSummary candidate) {
        if (candidate == null) return null;
        try {
            return gateService.latest(user, applicationId, candidate.candidateId());
        } catch (ApplicationLifecycleException exception) {
            if ("GATE_NOT_FOUND".equals(exception.getCode())) return null;
            throw exception;
        }
    }

    private EntrypointFact entrypoint(EntrypointConfiguration value) {
        return new EntrypointFact(value.id(), value.name(), value.type().name(), value.enabled());
    }

    private List<RunFact> recentRuns(SecurityUser user, Long applicationId) {
        Map<String, PlatformExecutionEventEntity> latest = new LinkedHashMap<>();
        eventMapper.selectList(new LambdaQueryWrapper<PlatformExecutionEventEntity>()
                        .eq(PlatformExecutionEventEntity::getTenantId, user.getTenantId())
                        .eq(PlatformExecutionEventEntity::getApplicationId, applicationId)
                        .orderByDesc(PlatformExecutionEventEntity::getCreatedAt).last("LIMIT 100"))
                .forEach(event -> latest.putIfAbsent(event.getExecutionId(), event));
        return latest.values().stream().limit(8).map(event -> new RunFact(event.getExecutionId(), event.getVersionId(),
                event.getStatus(), event.getEventType(), event.getCreatedAt())).toList();
    }

    /**
     * 生成当前应用的生命周期问题与卡点事实清单。
     */
    private List<LifecycleFinding> findings(Long applicationId,
                                             ApplicationLifecycleContracts.CandidateSummary candidate,
                                             EvaluationRunSummary evaluation,
                                             ApplicationLifecycleContracts.GateReport gate,
                                             OrchestrationVersionSummary active,
                                             List<EntrypointFact> entrypoints,
                                             Integer draftRevisionNo) {
        List<LifecycleFinding> result = new ArrayList<>();
        if (candidate == null) {
            result.add(new LifecycleFinding("CANDIDATE_MISSING", "还没有候选版本", "先固定当前草稿，才能进行评测和发布。", "BLOCKER", "candidate", "/applications/{applicationId}?tab=flow", "创建候选版本", false));
        } else if (draftRevisionNo != null && candidate.draftRevisionNo() != null && draftRevisionNo > candidate.draftRevisionNo()) {
            result.add(new LifecycleFinding("CANDIDATE_OUTDATED", "草稿已有更新", "当前草稿已更新至修订 " + draftRevisionNo + "，高于候选版本快照（修订 " + candidate.draftRevisionNo() + "）。需重新固化候选版本方可开展新配置评测。", "WARNING", "candidate", "/applications/{applicationId}?tab=release", "更新候选版本", false));
        }
        if (candidate != null && evaluation == null) result.add(new LifecycleFinding("EVALUATION_MISSING", "还没有评测记录", "候选版本需要至少完成一次评测。", "BLOCKER", "evaluation", "/evaluation?applicationId={applicationId}", "开始评测", false));
        if (candidate != null && evaluation != null && (evaluation.status() == EvaluationStatus.PARTIAL || evaluation.status() == EvaluationStatus.FAILED)) result.add(new LifecycleFinding("EVALUATION_INCOMPLETE", "评测尚未完成", evaluation.failureMessage() == null ? "部分用例未完成，请重试失败用例。" : evaluation.failureMessage(), "BLOCKER", "evaluation", "/evaluation?applicationId={applicationId}", "查看并重试评测", true));
        if (candidate != null && gate == null) result.add(new LifecycleFinding("GATE_MISSING", "尚未完成发布检查", "发布前需要生成候选版本的门禁报告。", "BLOCKER", "gate", "/applications/{applicationId}?tab=release", "运行发布检查", true));
        if (gate != null && gate.overallLevel() == GateLevel.BLOCKER) gate.findings().stream().filter(item -> item.level() == GateLevel.BLOCKER).forEach(item -> result.add(new LifecycleFinding(item.code(), item.title(), item.reason(), "BLOCKER", findingField(item), findingRoute(applicationId, item), "修复此项", item.overridable())));
        if (active == null && candidate != null && gate != null && gate.overallLevel() == GateLevel.PASSED) result.add(new LifecycleFinding("RELEASE_MISSING", "还没有生产版本", "门禁已通过，可以发布到生产环境。", "INFO", "release", "/applications/{applicationId}?tab=release", "发布应用", false));
        if (active != null && entrypoints.isEmpty()) result.add(new LifecycleFinding("ENTRYPOINT_MISSING", "还没有运行入口", "发布后需要至少配置一个入口才能调用应用。", "WARNING", "entrypoints", "/applications/{applicationId}/launch", "配置运行入口", false));
        return result;
    }

    /** 将门禁证据指向可以直接修复的产品页面，避免用户只能回到发布页猜原因。 */
    private String findingRoute(Long applicationId, ApplicationLifecycleContracts.GateFinding finding) {
        String target = finding.remediationTarget();
        if ("evaluation".equals(target)) return "/evaluation?applicationId=" + applicationId;
        if ("dependencies".equals(target)) return "/models";
        if ("runtime".equals(target)) return "/applications/" + applicationId + "/workflow";
        if ("candidate".equals(target) || "snapshot".equals(target)) return "/applications/" + applicationId + "/workflow";
        return "/applications/" + applicationId + "?tab=release";
    }

    /** 返回稳定的业务字段路径，供前端高亮对应资源或字段。 */
    private String findingField(ApplicationLifecycleContracts.GateFinding finding) {
        return switch (finding.code()) {
            case "DEPENDENCY_SNAPSHOT_MISSING" -> "workflow.dependencies";
            case "RUNTIME_NOT_READY", "STRUCTURE_INVALID" -> "workflow.graph";
            case "EVALUATION_REQUIRED", "EVALUATION_NOT_PASSED", "EVALUATION_EXPIRED" -> "evaluation.run";
            case "CANDIDATE_FINGERPRINT_MISMATCH" -> "candidate.snapshotFingerprint";
            default -> finding.remediationTarget();
        };
    }

    /**
     * 根据生命周期现状推导下一步核心推进动作。
     */
    private List<LifecycleNextAction> nextActions(SecurityUser user, Long applicationId,
                                                   ApplicationLifecycleContracts.CandidateSummary candidate,
                                                   EvaluationRunSummary evaluation,
                                                   ApplicationLifecycleContracts.GateReport gate,
                                                   OrchestrationVersionSummary active,
                                                   Integer draftRevisionNo) {
        if (candidate == null || (draftRevisionNo != null && candidate.draftRevisionNo() != null && draftRevisionNo > candidate.draftRevisionNo())) {
            return List.of(action(user, applicationId, "CREATE_CANDIDATE", candidate == null ? "创建候选版本" : "更新候选版本", "/applications/{applicationId}?tab=release", "EDIT_DRAFT"));
        }
        if (evaluation == null || evaluation.status() == EvaluationStatus.PARTIAL || evaluation.status() == EvaluationStatus.FAILED) return List.of(action(user, applicationId, "EVALUATE", "开始或重试评测", "/evaluation?applicationId={applicationId}", "PUBLISH"));
        if (gate == null || gate.overallLevel() == GateLevel.BLOCKER) return List.of(action(user, applicationId, "FIX_GATE", "修复发布检查", "/applications/{applicationId}?tab=release", "PUBLISH"));
        if (active == null) return List.of(action(user, applicationId, "PUBLISH", "发布应用", "/applications/{applicationId}?tab=release", "PUBLISH"));
        return List.of(action(user, applicationId, "INVOKE", "打开运行入口", "/applications/{applicationId}/launch", "RUN"), action(user, applicationId, "OPERATE", "查看运行记录", "/workflow-executions?appId={applicationId}", "READ_DRAFT"));
    }

    /** 根据统一编排授权结果生成可用或禁用的导航动作。 */
    private LifecycleNextAction action(SecurityUser user, Long applicationId, String code, String label,
                                       String route, String permission) {
        boolean allowed = authorizationService.decide(user, applicationId, permission).allowed();
        return new LifecycleNextAction(code, label, route, allowed);
    }
}
