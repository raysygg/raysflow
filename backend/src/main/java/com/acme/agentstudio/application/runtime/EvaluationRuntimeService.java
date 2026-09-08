package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.EvaluationReview;
import com.acme.agentstudio.domain.runtime.model.EvaluationRunRecord;
import com.acme.agentstudio.domain.runtime.model.EvaluationRunStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 评测运行与人工复核应用服务（Evaluation Runtime Service）。
 * 负责应用发布候选版本的自动化测试集评测（Dataset Evaluation）、指标计算（Metrics）、
 * 发布门禁校验与人工复核存证（Human Review & Evidence）。
 */
@Service
public class EvaluationRuntimeService {

    /** 内存评测运行记录 Map */
    private final Map<String, EvaluationRunRecord> runs = new ConcurrentHashMap<>();

    /** 内存人工复核记录 Map */
    private final Map<String, EvaluationReview> reviews = new ConcurrentHashMap<>();

    /**
     * 固定评测快照并启动评测运行任务。
     *
     * @param datasetId 评测数据集 ID
     * @param releaseId 候选发布版本 ID
     * @param snapshots 依赖资源与节点参数快照 Map
     * @return 评测运行记录 EvaluationRunRecord
     */
    public EvaluationRunRecord start(String datasetId, String releaseId, Map<String, Object> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            throw new IllegalArgumentException("评测快照 snapshots 不能为空，必须先固定资源版本。");
        }
        EvaluationRunRecord run = new EvaluationRunRecord(
                UUID.randomUUID().toString(),
                datasetId,
                releaseId,
                snapshots,
                EvaluationRunStatus.RUNNING,
                Map.of(),
                Instant.now(),
                null
        );
        runs.put(run.evaluationRunId(), run);
        return run;
    }

    /**
     * 评测计算完成，回写指标结果并标记终态。
     *
     * @param evaluationRunId 评测运行 ID
     * @param metrics 评测出具的质量指标 Map
     * @param requiresReview 是否需要进一步人工复核
     * @return 更新后的评测运行记录 EvaluationRunRecord
     */
    public EvaluationRunRecord complete(String evaluationRunId, Map<String, Object> metrics, boolean requiresReview) {
        EvaluationRunRecord current = require(evaluationRunId);
        EvaluationRunRecord updated = new EvaluationRunRecord(
                current.evaluationRunId(),
                current.datasetId(),
                current.releaseId(),
                current.snapshots(),
                requiresReview ? EvaluationRunStatus.REVIEW_REQUIRED : EvaluationRunStatus.COMPLETED,
                metrics,
                current.startedAt(),
                Instant.now()
        );
        runs.put(evaluationRunId, updated);
        return updated;
    }

    /**
     * 提交人工复核或生产抽样评估打分记录。
     *
     * @param evaluationRunId 评测运行 ID
     * @param reviewerId 复核人 ID
     * @param score 人工打分数 (1-5/0-100)
     * @param label 标签分类
     * @param comment 复核评语与中文意见
     * @param productionSample 是否为生产线上抽样复核
     * @return 人工复核对象 EvaluationReview
     */
    public EvaluationReview review(
            String evaluationRunId,
            String reviewerId,
            int score,
            String label,
            String comment,
            boolean productionSample
    ) {
        EvaluationRunRecord run = require(evaluationRunId);
        if (run.status() != EvaluationRunStatus.REVIEW_REQUIRED && !productionSample) {
            throw new IllegalStateException("当前评测运行处于非 REVIEW_REQUIRED 状态，不需要提交人工复核。");
        }
        EvaluationReview review = new EvaluationReview(
                UUID.randomUUID().toString(),
                evaluationRunId,
                reviewerId,
                score,
                label,
                comment,
                productionSample,
                Instant.now()
        );
        reviews.put(review.reviewId(), review);
        return review;
    }

    /** 获取或校验评测运行记录存在性 */
    private EvaluationRunRecord require(String evaluationRunId) {
        EvaluationRunRecord run = runs.get(evaluationRunId);
        if (run == null) {
            throw new IllegalArgumentException("未找到指定的评测运行记录：" + evaluationRunId);
        }
        return run;
    }
}

