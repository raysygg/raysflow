package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCalibration;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import com.acme.agentstudio.domain.knowledge.port.RetrievalScoreCalibrator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 基于配置 Profile 阈值及模型特性的分值校准器实现类（Profile Retrieval Score Calibrator）。
 * 评估最强相关度得分 topScore、计算分值差 scoreGap，并做出无命中 (No Hit) 拦截。
 */
@Component
public class ProfileRetrievalScoreCalibrator implements RetrievalScoreCalibrator {

        /**
         * calibrate 方法。
         *
         * @param profile profile 参数
         * @param candidates candidates 参数
         * @return RetrievalCalibration 返回对象
         */
    @Override
    public RetrievalCalibration calibrate(RagEmbeddingProfile profile, List<RetrievalCandidate> candidates) {
        List<RetrievalCandidate> ranked = candidates == null ? List.of() : normalize(profile, candidates).stream()
                .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                .toList();
        if (ranked.isEmpty()) {
            return new RetrievalCalibration(List.of(), true, 0D, 0D);
        }
        double topScore = ranked.get(0).relevanceScore();
        double secondScore = ranked.size() > 1 ? ranked.get(1).relevanceScore() : 0D;
        double scoreGap = topScore - secondScore;
        List<RetrievalCandidate> thresholdAccepted = ranked.stream()
                .filter(candidate -> candidate.exactScore() > 0D
                        || (candidate.rerankScore() > 0D
                        ? candidate.rerankScore() >= profile.rerankThreshold()
                        : candidate.fusionScore() >= profile.noHitThreshold()))
                .toList();

        // 临界分只有在第一名明显领先时才允许进入后续上下文组装。
        // Exact 命中不受通用 no-hit 阈值拦截，临界结果仍要求 Top1/Top2 有足够差距。
        boolean exactProtected = ranked.stream().anyMatch(candidate -> candidate.exactScore() > 0D);
        boolean belowNoHitThreshold = !exactProtected && topScore < profile.noHitThreshold();
        boolean confidentBoundaryHit = thresholdAccepted.isEmpty()
                && !belowNoHitThreshold
                && scoreGap >= profile.scoreGapThreshold();
        boolean noHit = belowNoHitThreshold || (thresholdAccepted.isEmpty() && !confidentBoundaryHit);
        List<RetrievalCandidate> accepted = confidentBoundaryHit ? List.of(ranked.get(0)) : thresholdAccepted;
        return new RetrievalCalibration(noHit ? List.of() : accepted, noHit, topScore, scoreGap);
    }

    /** 各通道先压缩到同一分数区间，避免 BM25 数值量级挤压向量分。 */
    private List<RetrievalCandidate> normalize(RagEmbeddingProfile profile, List<RetrievalCandidate> candidates) {
        double maxDense = candidates.stream().mapToDouble(RetrievalCandidate::denseScore).max().orElse(1D);
        double maxSparse = candidates.stream().mapToDouble(RetrievalCandidate::sparseScore).max().orElse(1D);
        return candidates.stream().map(candidate -> {
            double dense = normalize(candidate.denseScore(), maxDense);
            double sparse = normalize(candidate.sparseScore(), maxSparse);
            double exact = Math.min(1D, candidate.exactScore());
            double fusion = candidate.fusionScore() > 0D ? candidate.fusionScore() : candidate.relevanceScore();
            double calibrated = Math.min(1D, Math.max(fusion, exact));
            return candidate.withChannelScores(dense, sparse, exact, calibrated, candidate.rerankScore(),
                    candidate.channels(), candidate.hitReasons());
        }).toList();
    }

    private double normalize(double value, double max) {
        return max <= 0D ? 0D : Math.max(0D, Math.min(1D, value / max));
    }
}
