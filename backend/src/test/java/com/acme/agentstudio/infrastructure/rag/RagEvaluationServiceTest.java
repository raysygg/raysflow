package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.RagEvaluationMode;
import com.acme.agentstudio.domain.knowledge.model.RagEvaluationObservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagEvaluationServiceTest {
    @Test
    void shouldCalculateRecallNdcgLatencyAndCost() {
        RagEvaluationService service = new RagEvaluationService();
        var report = service.evaluate(List.of(new RagEvaluationObservation("zh-001",
                RagEvaluationMode.HYBRID_RERANKER, List.of(10L), List.of(10L, 20L),
                120L, true, new BigDecimal("0.002"))), 5);

        assertThat(report.metrics()).hasSize(RagEvaluationMode.values().length);
        var metrics = report.metrics().stream()
                .filter(item -> item.mode() == RagEvaluationMode.HYBRID_RERANKER)
                .findFirst().orElseThrow();
        assertThat(metrics.recallAtK()).isEqualTo(1D);
        assertThat(metrics.ndcgAtK()).isEqualTo(1D);
        assertThat(metrics.rerankerCalls()).isEqualTo(1);
        assertThat(metrics.rerankerCost()).isEqualByComparingTo("0.00200000");
    }
}
