package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 绑定固定模型、Prompt、知识库与工具调用的测试集运行物理记录实体 Record（Evaluation Run Record）。
 * 记录评测运行 ID evaluationRunId、数据集 ID datasetId、关联候选发布版本 ID releaseId、资源快照 Map snapshots、
 * 运行状态 status (EvaluationRunStatus)、多维度评测得分指标 metrics、开始与完成时间。
 *
 * @param evaluationRunId 评测运行唯一 ID
 * @param datasetId 关联的评测数据集 ID
 * @param releaseId 绑定的发布候选 Release ID
 * @param snapshots 依赖的模型/Prompt/知识库/策略不可变快照 Map
 * @param status 运行状态（EvaluationRunStatus）
 * @param metrics 综合维度评分结果 Map
 * @param startedAt 评测开始时间
 * @param finishedAt 评测完成时间
 */
public record EvaluationRunRecord(
        String evaluationRunId,
        String datasetId,
        String releaseId,
        Map<String, Object> snapshots,
        EvaluationRunStatus status,
        Map<String, Object> metrics,
        Instant startedAt,
        Instant finishedAt
) {
    /** 紧凑构造函数做输入属性校验 */
    public EvaluationRunRecord {
        if (evaluationRunId == null || evaluationRunId.isBlank() || datasetId == null || datasetId.isBlank()
                || releaseId == null || releaseId.isBlank() || status == null) {
            throw new IllegalArgumentException("评测运行标识、数据集、发布版本和状态不能为空");
        }
        snapshots = (snapshots == null) ? Map.of() : Map.copyOf(snapshots);
        metrics = (metrics == null) ? Map.of() : Map.copyOf(metrics);
        startedAt = (startedAt == null) ? Instant.now() : startedAt;
    }
}

