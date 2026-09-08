package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.knowledge.KnowledgeApplicationService;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationExecutor;
import com.acme.agentstudio.application.task.PersistentTaskQueueService;
import com.acme.agentstudio.application.task.PersistentTaskWorker;
import com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService;
import com.acme.agentstudio.config.RuntimeRecoveryProperties;
import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AsyncTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeTaskAttemptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuntimeTaskRecoveryConcurrencyTest {
    @Test
    void onlyOneWorkerShouldWinConditionalClaim() {
        AsyncTaskMapper mapper = mock(AsyncTaskMapper.class);
        when(mapper.claim(1L, "worker-a")).thenReturn(1);
        when(mapper.claim(1L, "worker-b")).thenReturn(0);
        PersistentTaskQueueService queue = new PersistentTaskQueueService(mapper, new ObjectMapper());
        AsyncTaskEntity task = new AsyncTaskEntity(); task.setId(1L);
        assertTrue(queue.claim(task, "worker-a"));
        assertFalse(queue.claim(task, "worker-b"));
    }

    @Test
    void staleWorkerShouldNotFinishAttempt() {
        RuntimeTaskAttemptMapper mapper = mock(RuntimeTaskAttemptMapper.class);
        when(mapper.update(isNull(), any())).thenReturn(0);
        RuntimeTaskAttemptService service = new RuntimeTaskAttemptService(mapper, new RuntimeRecoveryProperties());
        assertFalse(service.finish(1L, 2L, "stale-worker", true, null, null));
    }

    @Test
    void gracefulShutdownShouldStopNewClaims() {
        PersistentTaskQueueService queue = mock(PersistentTaskQueueService.class);
        PersistentTaskWorker worker = new PersistentTaskWorker(queue, mock(KnowledgeApplicationService.class),
                mock(RuntimeRunApplicationService.class), mock(PersistentOrchestrationExecutionService.class),
                mock(ApplicationEvaluationExecutor.class), mock(RuntimeTaskAttemptService.class),
                new RuntimeTelemetryService(), new ObjectMapper(), 120, 30);
        worker.markReady();
        assertTrue(worker.isReady());
        worker.shutdown();
        assertFalse(worker.isReady());
        worker.consume();
        verify(queue, never()).nextQueued();
    }
}
