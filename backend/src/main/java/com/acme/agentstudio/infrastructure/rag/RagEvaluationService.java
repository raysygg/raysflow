package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.RagEvaluationMetrics;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationMode;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationObservation;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationReport;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RagEvaluation 业务服务接口。
 * 定义 RagEvaluation 相关的核心业务契约与流程接口。
 */
/** 评测计算器只处理脱敏观察值，不在评测结果中保存查询和正文。 */
@Service
public class RagEvaluationService {
    private static final int DEFAULT_K = 5;
    private static final int SCALE = 8;

        /**
         * evaluate 方法。
         *
         * @param request request 参数
         * @return RagEvaluationReport 返回对象
         */
    public RagEvaluationReport evaluate(RagEvaluationRequest request) {
        if (request == null) return evaluate(List.of(), DEFAULT_K);
        return evaluate(request.observations(), request.k());
    }

        /**
         * evaluate 方法。
         *
         * @param observations observations 参数
         * @param requestedK requestedK 参数
         * @return RagEvaluationReport 返回对象
         */
    public RagEvaluationReport evaluate(List<RagEvaluationObservation> observations, int requestedK) {
        int k = Math.max(1, requestedK <= 0 ? DEFAULT_K : requestedK);
        Map<RagEvaluationMode, List<RagEvaluationObservation>> grouped = new EnumMap<>(RagEvaluationMode.class);
        if (observations != null) {
            observations.stream().filter(observation -> observation != null)
                    .forEach(observation -> grouped.computeIfAbsent(observation.mode(), ignored -> new ArrayList<>()).add(observation));
        }
        List<RagEvaluationMetrics> result = new ArrayList<>();
        for (RagEvaluationMode mode : RagEvaluationMode.values()) {
            result.add(metrics(mode, grouped.getOrDefault(mode, List.of()), k));
        }
        return new RagEvaluationReport(result);
    }

    private RagEvaluationMetrics metrics(RagEvaluationMode mode, List<RagEvaluationObservation> items, int k) {
        double recall = items.stream().mapToDouble(item -> recall(item, k)).average().orElse(0D);
        double ndcg = items.stream().mapToDouble(item -> ndcg(item, k)).average().orElse(0D);
        double firstHit = items.stream().filter(item -> firstHit(item, k)).count() / (double) Math.max(1, items.size());
        double latency = items.stream().mapToLong(RagEvaluationObservation::latencyMs).average().orElse(0D);
        int rerankerCalls = (int) items.stream().filter(RagEvaluationObservation::rerankerCalled).count();
        BigDecimal cost = items.stream().map(RagEvaluationObservation::rerankerCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        return new RagEvaluationMetrics(mode, items.size(), recall, ndcg, firstHit, latency, rerankerCalls, cost);
    }

    private double recall(RagEvaluationObservation item, int k) {
        Set<Long> expected = new HashSet<>(item.expectedDocumentIds());
        if (expected.isEmpty()) return 0D;
        Set<Long> retrieved = new HashSet<>(item.retrievedDocumentIds().stream().limit(k).toList());
        expected.retainAll(retrieved);
        return expected.size() / (double) new HashSet<>(item.expectedDocumentIds()).size();
    }

    private double ndcg(RagEvaluationObservation item, int k) {
        Set<Long> expected = new HashSet<>(item.expectedDocumentIds());
        if (expected.isEmpty()) return 0D;
        double dcg = 0D;
        List<Long> retrieved = item.retrievedDocumentIds().stream().limit(k).toList();
        for (int index = 0; index < retrieved.size(); index++) {
            if (expected.contains(retrieved.get(index))) dcg += 1D / log2(index + 2D);
        }
        double ideal = 0D;
        for (int index = 0; index < Math.min(k, expected.size()); index++) ideal += 1D / log2(index + 2D);
        return ideal == 0D ? 0D : dcg / ideal;
    }

    private boolean firstHit(RagEvaluationObservation item, int k) {
        return !item.retrievedDocumentIds().isEmpty() && item.expectedDocumentIds().contains(item.retrievedDocumentIds().get(0))
                && k > 0;
    }

    private double log2(double value) { return Math.log(value) / Math.log(2D); }
}
