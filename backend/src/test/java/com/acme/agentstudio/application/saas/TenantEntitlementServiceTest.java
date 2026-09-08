package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasEntitlementEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 套餐准入、超额策略、缓存并发和异常降级边界测试。 */
class TenantEntitlementServiceTest {

    /** 安全规则拒绝优先于商业影子模式。 */
    @Test
    void securityDenialAlwaysFailsClosed() {
        Fixture fixture = fixture();

        var result = fixture.service.admit(request("MODEL_TOKEN"), false, true);

        assertEquals(AdmissionDecision.DENY, result.decision());
    }

    /** 硬限制在并发检查下必须稳定拒绝，不能随机放行。 */
    @Test
    void hardStopRemainsDeniedDuringConcurrentChecks() {
        Fixture fixture = fixture();
        SaasEntitlementEntity entitlement = entitlement("WORKFLOW_RUN", 10, 8, "HARD_STOP");
        when(fixture.entitlementMapper.selectOne(any())).thenReturn(entitlement);
        when(fixture.usageMapper.selectList(any())).thenReturn(List.of());

        List<AdmissionDecision> decisions = IntStream.range(0, 20).parallel()
                .mapToObj(index -> fixture.service.admit(new AdmissionRequest(1L, 2L, "WORKFLOW_RUN", 11,
                        "并发运行", "request-" + index), true, false).decision())
                .toList();

        assertEquals(Set.of(AdmissionDecision.DENY), Set.copyOf(decisions));
    }

    /** 计费类功能在准入基础设施故障时告警放行。 */
    @Test
    void billingFeatureFailsOpenWhenAdmissionStoreUnavailable() {
        Fixture fixture = fixture();
        when(fixture.entitlementMapper.selectOne(any())).thenThrow(new IllegalStateException("数据库不可用"));

        var result = fixture.service.admitForConsumption(request("MODEL_TOKEN"), true);

        assertEquals(AdmissionDecision.WARN, result.decision());
    }

    /** 存储类功能在准入基础设施故障时关闭，避免产生无法治理的数据。 */
    @Test
    void storageFeatureFailsClosedWhenAdmissionStoreUnavailable() {
        Fixture fixture = fixture();
        when(fixture.entitlementMapper.selectOne(any())).thenThrow(new IllegalStateException("数据库不可用"));

        var result = fixture.service.admitForConsumption(request("KNOWLEDGE_STORAGE"), true);

        assertEquals(AdmissionDecision.DENY, result.decision());
    }

    /** 允许超额的权益达到硬限制后应告警放行。 */
    @Test
    void allowedOverageReturnsWarning() {
        Fixture fixture = fixture();
        when(fixture.entitlementMapper.selectOne(any()))
                .thenReturn(entitlement("MODEL_TOKEN", 10, 8, "ALLOW_OVERAGE"));
        when(fixture.usageMapper.selectList(any())).thenReturn(List.of());

        var result = fixture.service.admit(new AdmissionRequest(1L, 2L, "MODEL_TOKEN", 11,
                "模型调用", "overage-request"), true, false);

        assertEquals(AdmissionDecision.WARN, result.decision());
    }

    private Fixture fixture() {
        SaasPlanVersionMapper planMapper = mock(SaasPlanVersionMapper.class);
        SaasSubscriptionMapper subscriptionMapper = mock(SaasSubscriptionMapper.class);
        SaasEntitlementMapper entitlementMapper = mock(SaasEntitlementMapper.class);
        SaasUsageEventMapper usageMapper = mock(SaasUsageEventMapper.class);
        SaasAdmissionDecisionMapper decisionMapper = mock(SaasAdmissionDecisionMapper.class);
        SaasGovernanceProperties properties = new SaasGovernanceProperties();
        properties.setEnforcedAdmissionFeatures(Set.of("WORKFLOW_RUN", "KNOWLEDGE_STORAGE"));
        properties.setAdmissionFailOpenFeatures(Set.of("MODEL_TOKEN", "WORKFLOW_RUN", "CONNECTOR_CALL"));
        TenantEntitlementService service = new TenantEntitlementService(planMapper, subscriptionMapper,
                entitlementMapper, usageMapper, decisionMapper, new ObjectMapper(), properties);
        return new Fixture(service, entitlementMapper, usageMapper);
    }

    private AdmissionRequest request(String feature) {
        return new AdmissionRequest(1L, 2L, feature, 1, "测试操作", "request");
    }

    private SaasEntitlementEntity entitlement(String feature, long hard, long soft, String policy) {
        SaasEntitlementEntity entity = new SaasEntitlementEntity();
        entity.setTenantId(1L);
        entity.setFeatureCode(feature);
        entity.setHardLimit(hard);
        entity.setSoftLimit(soft);
        entity.setOveragePolicy(policy);
        entity.setVersionNo(1);
        return entity;
    }

    private record Fixture(TenantEntitlementService service, SaasEntitlementMapper entitlementMapper,
                           SaasUsageEventMapper usageMapper) { }
}
