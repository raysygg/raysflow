package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeContextBuilderTest {
    private final RuntimeContextBuilder builder = new RuntimeContextBuilder();

    @Test
    void nodeConfigurationOverridesLowerLayersAndSensitiveValuesAreRedacted() {
        RuntimeContext context = builder.build(2, 10, "release-1", "user-1", "conversation-1", RuntimeMode.CHAT,
                Map.of("question", "hello"),
                Map.of("budget", Map.of("maxSteps", 4), "secret", "tenant-secret"),
                Map.of("budget", Map.of("maxSteps", 8), "tools", Map.of("apiKey", "app-key")),
                Map.of("budget", Map.of("maxTokens", 9000)),
                Map.of("budget", Map.of("maxSteps", 12), "policy", Map.of("password", "node-password")));

        assertEquals(12, context.budget().maxSteps());
        assertEquals(9000, context.budget().maxTokens());
        Map<?, ?> configuration = assertInstanceOf(Map.class, context.resolvedSnapshot().get("configuration"));
        assertEquals("[REDACTED]", configuration.get("secret"));
        Map<?, ?> policy = assertInstanceOf(Map.class, configuration.get("policy"));
        assertEquals("[REDACTED]", policy.get("password"));
    }

    @Test
    void invalidTenantIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(0, 10, "release-1", "user-1", null,
                RuntimeMode.CHAT, Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));
    }
}
