package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.RuntimeRecoveryProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DeadLetterStatus;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DispositionType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuntimeBatchDispositionServiceTest {
    @Test
    void shouldKeepSuccessfulItemsWhenOneDispositionFails() {
        PersistentRuntimeDeadLetterService deadLetters = mock(PersistentRuntimeDeadLetterService.class);
        doThrow(new IllegalStateException("记录已被处理")).when(deadLetters)
                .dispose(any(), eq(2L), any(), any(), anyString(), any());
        RuntimeBatchDispositionService service = new RuntimeBatchDispositionService(deadLetters, new RuntimeRecoveryProperties());
        SecurityUser user = new SecurityUser(2L, 1L, "operator", "OPERATOR");
        var result = service.execute(user, new RuntimeBatchDispositionService.BatchCommand(List.of(1L, 2L),
                DispositionType.RESOLVE, DeadLetterStatus.RESOLVED, "已核实", null, true));
        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        assertEquals(2, result.items().size());
    }
}
