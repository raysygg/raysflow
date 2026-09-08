package com.acme.agentstudio.infrastructure.model.strategy;

import com.acme.agentstudio.domain.model.ModelInvocationErrorCategory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ModelInvocationErrorClassifierTest {
    private final ModelInvocationErrorClassifier classifier = new ModelInvocationErrorClassifier();

    @Test
    void shouldClassifyNestedTimeout() {
        ModelInvocationErrorCategory category = classifier.classify(
                new CompletionException(new TimeoutException("provider timeout")));

        assertThat(category).isEqualTo(ModelInvocationErrorCategory.TIMEOUT);
    }

    @Test
    void shouldClassifyRateLimitResponse() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "rate limited", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);

        assertThat(classifier.classify(exception)).isEqualTo(ModelInvocationErrorCategory.RATE_LIMITED);
    }

    @Test
    void shouldClassifyDimensionMismatchWithoutExposingProviderTypes() {
        assertThat(classifier.classify(new IllegalStateException("查询向量维度不一致")))
                .isEqualTo(ModelInvocationErrorCategory.DIMENSION_MISMATCH);
    }
}
