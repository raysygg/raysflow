package com.acme.agentstudio.application.lifecycle;

import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** 生命周期基础契约测试，确保候选快照和门禁状态不会被调用方意外修改。 */
class ApplicationLifecycleContractsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void candidateSnapshotCopiesReferenceLists() throws Exception {
        var entrypoints = new ArrayList<>(java.util.List.of("chat"));
        var policies = new ArrayList<>(java.util.List.of("default"));
        var snapshot = new ApplicationLifecycleContracts.CandidateSnapshot(
                "application-release-candidate-v1", 100L, 3,
                objectMapper.readTree("{\"graphType\":\"APPLICATION_WORKFLOW\"}"),
                "dependency-fingerprint", entrypoints, policies);

        entrypoints.add("admin");
        policies.clear();

        assertThat(snapshot.entrypointReferences()).containsExactly("chat");
        assertThat(snapshot.policyReferences()).containsExactly("default");
    }

    @Test
    void lifecycleStatesExposeOnlySupportedTransitions() {
        assertThat(ApplicationLifecycleContracts.CandidateStatus.values())
                .containsExactly(ApplicationLifecycleContracts.CandidateStatus.CREATED,
                        ApplicationLifecycleContracts.CandidateStatus.EVALUATING,
                        ApplicationLifecycleContracts.CandidateStatus.READY,
                        ApplicationLifecycleContracts.CandidateStatus.BLOCKED,
                        ApplicationLifecycleContracts.CandidateStatus.PUBLISHED,
                        ApplicationLifecycleContracts.CandidateStatus.RETIRED);
        assertThat(ApplicationLifecycleContracts.GateLevel.BLOCKER.name()).isEqualTo("BLOCKER");
        assertThat(ApplicationLifecycleContracts.RuntimeReadiness.PRODUCTION_READY.name())
                .isEqualTo("PRODUCTION_READY");
    }

}
