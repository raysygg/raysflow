package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;

/**
 * 评测结果人工标注审查与生产抽样复核记录 Record（Evaluation Review）。
 * 记录复核 ID reviewId、关联评测运行 ID evaluationRunId、审核人 reviewerId、打分 score (0-5分)、标签 label、评语 comment、是否为生产流量抽样复核 productionSample 及复核时间 reviewedAt。
 *
 * @param reviewId 复核记录 ID
 * @param evaluationRunId 关联的离线评测运行 ID
 * @param reviewerId 执行复核的专家账号 ID
 * @param score 人工复核评分（0 ~ 5 分）
 * @param label 标注分类标签
 * @param comment 人工审核评语说明
 * @param productionSample 是否属于生产环境在线采样数据
 * @param reviewedAt 专家复核完成时间
 */
public record EvaluationReview(
        String reviewId,
        String evaluationRunId,
        String reviewerId,
        int score,
        String label,
        String comment,
        boolean productionSample,
        Instant reviewedAt
) {
    /** 紧凑构造函数做输入评分范围断言校验 */
    public EvaluationReview {
        if (reviewId == null || reviewId.isBlank() || evaluationRunId == null || evaluationRunId.isBlank()
                || reviewerId == null || reviewerId.isBlank() || score < 0 || score > 5
                || label == null || label.isBlank()) {
            throw new IllegalArgumentException("评测复核标识、评测运行、复核人、评分和标签无效");
        }
        comment = (comment == null) ? "" : comment;
        reviewedAt = (reviewedAt == null) ? Instant.now() : reviewedAt;
    }
}

