package com.acme.agentstudio.domain.agent.model;

public record AgentProfileSummary(
        Long id,
        String code,
        String name,
        String agentType,
        String ownerTeam,
        String status,
        String modelKey,
        String toolSummary,
        String workflowCode
) {
}
