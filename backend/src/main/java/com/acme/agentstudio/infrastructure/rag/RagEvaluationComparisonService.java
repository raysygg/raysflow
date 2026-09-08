package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.RagEvaluationMetrics;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationMode;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationReport;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;

/**
 * RagEvaluationComparison 业务服务接口。
 * 定义 RagEvaluationComparison 相关的核心业务契约与流程接口。
 */
/** 对比两个 Profile/Generation 评测报告，输出可供发布门禁消费的质量回归证据。 */
@Service
public class RagEvaluationComparisonService {
        /**
         * compare 方法。
         *
         * @param baseline baseline 参数
         * @param candidate candidate 参数
         * @return RagEvaluationComparisonReport 返回对象
         */
    public RagEvaluationComparisonReport compare(RagEvaluationReport baseline, RagEvaluationReport candidate) {
        EnumMap<RagEvaluationMode, MetricDelta> deltas = new EnumMap<>(RagEvaluationMode.class);
        for (RagEvaluationMode mode : RagEvaluationMode.values()) {
            RagEvaluationMetrics left = find(baseline, mode);
            RagEvaluationMetrics right = find(candidate, mode);
            deltas.put(mode, new MetricDelta(right.recallAtK() - left.recallAtK(),
                    right.ndcgAtK() - left.ndcgAtK(), right.firstHitRate() - left.firstHitRate(),
                    right.averageLatencyMs() - left.averageLatencyMs(),
                    right.rerankerCost().subtract(left.rerankerCost())));
        }
        return new RagEvaluationComparisonReport(deltas);
    }

    private RagEvaluationMetrics find(RagEvaluationReport report, RagEvaluationMode mode) {
        return report.metrics().stream().filter(item -> item.mode() == mode).findFirst()
                .orElse(new RagEvaluationMetrics(mode, 0, 0D, 0D, 0D, 0D, 0,
                        java.math.BigDecimal.ZERO));
    }

    public record RagEvaluationComparisonReport(EnumMap<RagEvaluationMode, MetricDelta> deltas) {
        public RagEvaluationComparisonReport {
            deltas = deltas == null ? new EnumMap<>(RagEvaluationMode.class) : new EnumMap<>(deltas);
        }

            /**
             * regressions 方法。
             * @return List<MetricDelta> 返回对象
             */
        public List<MetricDelta> regressions() {
            return deltas.values().stream().filter(item -> item.recallDelta() < 0D || item.ndcgDelta() < 0D).toList();
        }
    }

    public record MetricDelta(double recallDelta, double ndcgDelta, double firstHitDelta,
                              double latencyDeltaMs, java.math.BigDecimal costDelta) {
    }
}
