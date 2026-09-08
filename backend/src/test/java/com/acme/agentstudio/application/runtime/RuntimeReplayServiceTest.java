package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeContextLayers;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeReplayServiceTest {
    @Test
    void shouldBindSnapshotToOriginalRelease() {
        RuntimeContext context = new RuntimeContext("run-1", 1L, 2L, "release-old", "user-1", null,
                RuntimeMode.CHAT, new RuntimeContextLayers(Map.of("query", "hello"), Map.of(), Map.of(), Map.of(), Map.of()),
                Map.of(), Map.of(), null, null, Map.of("prompt", Map.of("version", "1")), null);
        var snapshot = new RuntimeReplayService().capture(context);
        assertEquals("release-old", snapshot.releaseId());
        assertTrue(new RuntimeReplayService().belongsToRelease(snapshot, "release-old"));
        assertFalse(new RuntimeReplayService().belongsToRelease(snapshot, "release-new"));
    }
}
