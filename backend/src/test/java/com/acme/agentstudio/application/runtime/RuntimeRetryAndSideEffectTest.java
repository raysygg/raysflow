package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.SideEffectStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeSideEffectCheckpointEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeSideEffectCheckpointMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuntimeRetryAndSideEffectTest {
    @Test
    void shouldRetryOnlyTransientFailures() {
        assertTrue(RuntimeRetryPolicy.retryable(ErrorCategory.TIMEOUT));
        assertTrue(RuntimeRetryPolicy.retryable(ErrorCategory.RATE_LIMIT));
        assertFalse(RuntimeRetryPolicy.retryable(ErrorCategory.AUTHENTICATION));
        assertFalse(RuntimeRetryPolicy.retryable(ErrorCategory.CONFIGURATION));
    }

    @Test
    void shouldRequireReviewForUnknownExternalSideEffect() {
        RuntimeSideEffectCheckpointMapper mapper = mock(RuntimeSideEffectCheckpointMapper.class);
        RuntimeSideEffectCheckpointEntity checkpoint = new RuntimeSideEffectCheckpointEntity();
        checkpoint.setSideEffectStatus(SideEffectStatus.UNKNOWN.name()); checkpoint.setResultSummary("供应商结果未知");
        when(mapper.selectOne(any())).thenReturn(checkpoint);
        var decision = new RuntimeSideEffectService(mapper).beforeRecovery(1L, "operation-1");
        assertFalse(decision.allowed());
        assertEquals(SideEffectStatus.REQUIRES_REVIEW, decision.status());
    }
}
