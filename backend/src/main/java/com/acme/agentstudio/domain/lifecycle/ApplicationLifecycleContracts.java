package com.acme.agentstudio.domain.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一 Agent/Workflow 应用生命周期跨层契约类（Application Lifecycle Contracts）。
 * 约定发布候选状态 CandidateStatus、评测状态 EvaluationStatus、发布门禁等级 GateLevel、
 * 运行就绪状态 RuntimeReadiness、门禁分类 GateCategory 及完整生命周期摘要模型等。
 */
public final class ApplicationLifecycleContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private ApplicationLifecycleContracts() {
    }

    /** 发布候选（Release Candidate）生命周期状态枚举 */
    public enum CandidateStatus {
        /** 已创建草稿候选 */
        CREATED,

        /** 自动化评测运行中 */
        EVALUATING,

        /** 门禁与评测校验通过，准备就绪 */
        READY,

        /** 被阻断拦截 */
        BLOCKED,

        /** 已正式发布上线 */
        PUBLISHED,

        /** 已废弃退役 */
        RETIRED
    }

    /** 评测任务状态枚举 */
    public enum EvaluationStatus {
        /** 排队中 */
        QUEUED,

        /** 运行中 */
        RUNNING,

        /** 部分成功 */
        PARTIAL,

        /** 成功完成 */
        SUCCEEDED,

        /** 失败 */
        FAILED
    }

    /** 门禁检测结果等级枚举 */
    public enum GateLevel {
        /** 通过 */
        PASSED,

        /** 警告 */
        WARNING,

        /** 阻断挂起 */
        BLOCKER
    }

    /** 运行时准备就绪状态枚举 */
    public enum RuntimeReadiness {
        /** 生产环境就绪 */
        PRODUCTION_READY,

        /** 实验性环境 */
        EXPERIMENTAL,

        /** 未就绪 */
        NOT_READY
    }

    /** 门禁规则检测分类枚举 */
    public enum GateCategory {
        /** 拓扑结构 */
        STRUCTURE,

        /** 鉴权与权限 */
        AUTHORIZATION,

        /** 安全注入防范 */
        SECURITY,

        /** 依赖组件强校验 */
        DEPENDENCY,

        /** 运行时配置 */
        RUNTIME,

        /** 离线评测质量 */
        EVALUATION,

        /** P95 延时预算 */
        LATENCY,

        /** 成本用量上限 */
        COST
    }

    /** 评测指标得分状态枚举 */
    public enum EvaluationMetricState {
        /** 已打分 */
        SCORED,

        /** 不适用 */
        NOT_APPLICABLE,

        /** 不可用 */
        UNAVAILABLE
    }

    /** 发布候选版本快照实体 Record */
    public record CandidateSnapshot(
            String schemaVersion,
            Long applicationId,
            Integer draftRevisionNo,
            JsonNode graph,
            String dependencyFingerprint,
            List<String> entrypointReferences,
            List<String> policyReferences,
            KnowledgeBinding knowledgeBinding
    ) {
        public CandidateSnapshot(
                String schemaVersion,
                Long applicationId,
                Integer draftRevisionNo,
                JsonNode graph,
                String dependencyFingerprint,
                List<String> entrypointReferences,
                List<String> policyReferences
        ) {
            this(schemaVersion, applicationId, draftRevisionNo, graph, dependencyFingerprint,
                    entrypointReferences, policyReferences, KnowledgeBinding.empty());
        }

        public CandidateSnapshot {
            entrypointReferences = (entrypointReferences == null) ? List.of() : List.copyOf(entrypointReferences);
            policyReferences = (policyReferences == null) ? List.of() : List.copyOf(policyReferences);
            knowledgeBinding = (knowledgeBinding == null) ? KnowledgeBinding.empty() : knowledgeBinding;
        }
    }

    /** 知识库绑定引用 Record */
    public record KnowledgeBinding(
            Long profileVersionId,
            Long generationId
    ) {
        /**
         * 构建空知识库绑定实例。
         *
         * @return 空 KnowledgeBinding 对象
         */
        public static KnowledgeBinding empty() {
            return new KnowledgeBinding(null, null);
        }
    }

    /** 发布候选版本摘要 Record */
    public record CandidateSummary(
            Long candidateId,
            Long applicationId,
            Long draftRevisionId,
            Integer draftRevisionNo,
            String snapshotFingerprint,
            CandidateStatus status,
            LocalDateTime createdAt
    ) {
    }

    /** 发布候选版本详情 Record */
    public record CandidateDetail(
            CandidateSummary summary,
            String changeSummary,
            String baseReleaseId,
            Integer lockVersion
    ) {
    }

    /** 候选版本变更 Diff Record */
    public record CandidateDiff(
            Long candidateId,
            Long draftRevisionId,
            List<String> changedSections
    ) {
        public CandidateDiff {
            changedSections = (changedSections == null) ? List.of() : List.copyOf(changedSections);
        }
    }

    /** 门禁检测发现项 Record */
    public record GateFinding(
            String code,
            GateCategory category,
            GateLevel level,
            String title,
            String reason,
            String evidenceSummary,
            String remediationTarget,
            boolean overridable
    ) {
    }

    /** 门禁检测聚合报告 Record */
    public record GateReport(
            Long reportId,
            Long candidateId,
            String candidateFingerprint,
            GateLevel overallLevel,
            List<GateFinding> findings,
            LocalDateTime evaluatedAt
    ) {
        public GateReport {
            findings = (findings == null) ? List.of() : List.copyOf(findings);
        }
    }

    /** 不可变 Release 审计事实 Record */
    public record ReleaseFact(
            String releaseId,
            Integer versionNo,
            String environmentCode,
            String snapshotFingerprint,
            LocalDateTime releasedAt
    ) {
    }

    /** 生命周期的风险发现项描述 Record */
    public record LifecycleFinding(
            String code,
            String title,
            String reason,
            String level,
            String fieldPath,
            String route,
            String actionLabel,
            boolean retryable
    ) {
    }

    /** 生命周期允许的下一步推荐操作 Record */
    public record LifecycleNextAction(
            String code,
            String label,
            String route,
            boolean enabled
    ) {
    }

    /** 租户应用生命周期大盘全景摘要 Record */
    public record LifecycleSummary(
            Integer draftRevisionNo,
            CandidateSummary candidate,
            EvaluationRunFact evaluation,
            GateReport gate,
            ReleaseFact activeRelease,
            List<EntrypointFact> entrypoints,
            List<RunFact> recentRuns,
            List<LifecycleFinding> findings,
            List<LifecycleNextAction> nextActions
    ) {
        public LifecycleSummary {
            entrypoints = (entrypoints == null) ? List.of() : List.copyOf(entrypoints);
            recentRuns = (recentRuns == null) ? List.of() : List.copyOf(recentRuns);
            findings = (findings == null) ? List.of() : List.copyOf(findings);
            nextActions = (nextActions == null) ? List.of() : List.copyOf(nextActions);
        }
    }

    /** 评测运行的工作区摘要 Record */
    public record EvaluationRunFact(
            Long runId,
            EvaluationStatus status,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            String failureMessage
    ) {
    }

    /** 对外入口的工作区摘要 Record */
    public record EntrypointFact(
            Long id,
            String name,
            String type,
            boolean enabled
    ) {
    }

    /** 最近运行的脱敏事实 Record */
    public record RunFact(
            String executionId,
            Long versionId,
            String status,
            String eventType,
            LocalDateTime occurredAt
    ) {
    }
}

