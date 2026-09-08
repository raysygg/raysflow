package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.Correlation;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.RuntimeTelemetryEvent;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliType;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.StageType;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeTelemetryServiceTest {
    @Test
    void shouldAggregateTenantAndReleaseWindow() {
        RuntimeTelemetryService service = new RuntimeTelemetryService();
        Correlation correlation = new Correlation(1L, 10L, "release-a", "run-a", "trace-a", "span-a");
        service.record(new RuntimeTelemetryEvent(correlation, StageType.QUEUE, "QUEUED", 20, true, "等待完成", Instant.now()));
        service.record(new RuntimeTelemetryEvent(correlation, StageType.RUN, "RECOVERY_COMPLETED", 80, true, "恢复完成", Instant.now()));

        var snapshots = service.snapshots(1L, 10L, "release-a", null, Duration.ofMinutes(5));
        assertEquals(1D, snapshots.stream().filter(item -> item.type() == SliType.SUCCESS_RATE).findFirst().orElseThrow().value());
        assertEquals(20D, snapshots.stream().filter(item -> item.type() == SliType.QUEUE_LATENCY).findFirst().orElseThrow().value());
        assertEquals(1D, snapshots.stream().filter(item -> item.type() == SliType.RECOVERY_RATE).findFirst().orElseThrow().value());
    }

    @Test
    void shouldRedactSensitiveSummary() {
        assertEquals("已隐藏敏感信息", RuntimeTelemetrySanitizer.summary("Authorization: Bearer secret"));
        assertTrue(RuntimeTelemetrySanitizer.summary("a".repeat(300)).length() <= 256);
    }
}
