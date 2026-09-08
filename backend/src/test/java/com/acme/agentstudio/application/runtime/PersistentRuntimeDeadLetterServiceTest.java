package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DeadLetterStatus;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DispositionType;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterDispositionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterMapper;
import com.acme.agentstudio.config.SecurityUser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PersistentRuntimeDeadLetterServiceTest {
    @Test
    void shouldRecordTenantScopedDeadLetter() {
        RuntimeDeadLetterMapper mapper = mock(RuntimeDeadLetterMapper.class);
        RuntimeDeadLetterDispositionMapper audit = mock(RuntimeDeadLetterDispositionMapper.class);
        var service = new PersistentRuntimeDeadLetterService(mapper, audit);
        var item = service.record(9L, 2L, "run-1", ErrorCategory.BUSINESS, "业务失败", "安全快照");
        assertEquals(9L, item.getTenantId());
        assertEquals(DeadLetterStatus.OPEN.name(), item.getDeadLetterStatus());
        verify(mapper).insert(any(RuntimeDeadLetterEntity.class));
    }
}
