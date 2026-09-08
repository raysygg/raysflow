package com.acme.agentstudio.domain.knowledge;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库生命周期管理、数据源同步、版本管控与质量评测契约定义类（Knowledge Lifecycle Contracts）。
 * 集中管理知识库状态、数据源同步状态、断点 Checkpoint、文档版本、质量发现与评测摘要数据模型。
 */
public final class KnowledgeLifecycleContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private KnowledgeLifecycleContracts() {
    }

    /** 知识库生命周期状态枚举 */
    public enum KnowledgeBaseStatus {
        /** 在线正常活跃 */
        ACTIVE,

        /** 归档存档 */
        ARCHIVED
    }

    /** 知识来源类型枚举 */
    public enum SourceType {
        /** 用户手动上传 */
        UPLOAD,

        /** 连接器自动拉取 */
        CONNECTOR
    }

    /** 数据源同步状态枚举 */
    public enum SourceStatus {
        /** 已创建待处理 */
        CREATED,

        /** 同步中 */
        SYNCING,

        /** 活跃生效 */
        ACTIVE,

        /** 已暂停 */
        PAUSED,

        /** 同步失败 */
        FAILED,

        /** 已逻辑删除 */
        DELETED
    }

    /** 同步批次任务状态枚举 */
    public enum SyncRunStatus {
        /** 排队中 */
        QUEUED,

        /** 运行中 */
        RUNNING,

        /** 部分完成 */
        PARTIAL,

        /** 全部成功 */
        SUCCEEDED,

        /** 整体失败 */
        FAILED,

        /** 已手动取消 */
        CANCELLED
    }

    /** 文档版本生命周期状态枚举 */
    public enum DocumentVersionStatus {
        /** 已发现 */
        DISCOVERED,

        /** 已解析 */
        PARSED,

        /** 向量索引构建中 */
        INDEXING,

        /** 已完成向量构建 */
        INDEXED,

        /** 已被规则排除 */
        EXCLUDED,

        /** 已删除 */
        DELETED,

        /** 处理失败 */
        FAILED
    }

    /** 质量问题严重级别枚举 */
    public enum QualitySeverity {
        /** 提示信息 */
        INFO,

        /** 警告 */
        WARNING,

        /** 阻塞阻断级别 */
        BLOCKER
    }

    /** 质量检查结果处理状态枚举 */
    public enum QualityFindingStatus {
        /** 待处理 */
        OPEN,

        /** 已指派处理人 */
        ASSIGNED,

        /** 已忽略 */
        IGNORED,

        /** 已解决 */
        RESOLVED,

        /** 需重新校验 */
        RECHECK_REQUIRED
    }

    /** 向量配置 Profile 版本状态枚举 */
    public enum ProfileVersionStatus {
        /** 草稿 */
        DRAFT,

        /** 诊断测试中 */
        DIAGNOSING,

        /** 在线活跃中 */
        ACTIVE,

        /** 已退役 */
        RETIRED,

        /** 失败 */
        FAILED
    }

    /** RAG 评测任务状态枚举 */
    public enum EvaluationStatus {
        /** 排队中 */
        QUEUED,

        /** 运行中 */
        RUNNING,

        /** 成功完成 */
        SUCCEEDED,

        /** 部分成功 */
        PARTIAL,

        /** 失败 */
        FAILED
    }

    /** 知识库基础摘要 Record */
    public record KnowledgeBaseSummary(
            Long id,
            Long tenantId,
            String name,
            KnowledgeBaseStatus status,
            LocalDateTime updatedAt
    ) {
    }

    /** 知识数据源摘要 Record */
    public record SourceSummary(
            Long id,
            Long tenantId,
            Long knowledgeBaseId,
            SourceType sourceType,
            String displayName,
            SourceStatus status,
            LocalDateTime lastSyncedAt
    ) {
    }

    /** 数据源增量同步检查点 Record */
    public record SyncRunCheckpoint(
            Long syncRunId,
            String cursor,
            int discoveredCount,
            int changedCount,
            int deletedCount,
            int skippedCount,
            int failedCount,
            SyncRunStatus status
    ) {
    }

    /** 文档版本控制摘要 Record */
    public record DocumentVersionSummary(
            Long documentId,
            String externalId,
            String contentFingerprint,
            int versionNo,
            DocumentVersionStatus status
    ) {
    }

    /** 知识库质量诊断发现项 Record */
    public record QualityFinding(
            String code,
            QualitySeverity severity,
            String targetType,
            Long targetId,
            String evidenceSummary,
            String remediation,
            QualityFindingStatus status,
            String assignee
    ) {
    }

    /** 向量配置 Profile 版本引用 Record */
    public record ProfileVersionReference(
            Long profileId,
            int versionNo,
            String embeddingFingerprint,
            ProfileVersionStatus status
    ) {
    }

    /** 知识评测任务运行摘要 Record */
    public record KnowledgeEvaluationSummary(
            Long evaluationId,
            Long profileVersionId,
            Long generationId,
            EvaluationStatus status,
            List<String> modes,
            LocalDateTime completedAt
    ) {
        public KnowledgeEvaluationSummary {
            modes = (modes == null) ? List.of() : List.copyOf(modes);
        }
    }
}

