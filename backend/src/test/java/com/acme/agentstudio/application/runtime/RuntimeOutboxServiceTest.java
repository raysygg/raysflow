package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeOutboxEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeOutboxEventMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuntimeOutboxServiceTest {
    @Test
    void shouldPersistCompensationEvent() {
        RuntimeOutboxEventMapper mapper = mock(RuntimeOutboxEventMapper.class);
        RuntimeOutboxEventEntity event = new RuntimeOutboxService(mapper).append(1L, "RUN", "run-1", "RUN_SYNC_REQUIRED", "{}");
        assertEquals("PENDING", event.getOutboxStatus());
        verify(mapper).insert(any(RuntimeOutboxEventEntity.class));
    }
}
