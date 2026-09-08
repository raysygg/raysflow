package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelPriceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RagRetrievalMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelPriceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagRetrievalMetricMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RerankerBudgetGuardTest {
    @Test
    void shouldDenyRequestWhenMonthlyBudgetWouldBeExceeded() {
        ModelPriceMapper priceMapper = mock(ModelPriceMapper.class);
        RagRetrievalMetricMapper metricMapper = mock(RagRetrievalMetricMapper.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.rerankerBudget()).thenReturn(1D);
        when(profile.rerankerModelKey()).thenReturn("rerank-model");
        ModelPriceEntity price = new ModelPriceEntity();
        price.setInputPricePer1k(new BigDecimal("1"));
        when(priceMapper.selectOne(any())).thenReturn(price);
        RagRetrievalMetricEntity used = new RagRetrievalMetricEntity();
        used.setRerankerEstimatedCost(new BigDecimal("0.99"));
        when(metricMapper.selectList(any())).thenReturn(List.of(used));

        var decision = new RerankerBudgetGuard(priceMapper, metricMapper)
                .check(1L, profile, "a sufficiently long query", 100);

        assertThat(decision.allowed()).isFalse();
    }
}
