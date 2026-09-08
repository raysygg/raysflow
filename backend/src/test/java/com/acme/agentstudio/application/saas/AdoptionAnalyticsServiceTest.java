package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventSource;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventType;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasAdoptionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasAdoptionEventMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 产品采用事件去重、流量排除和隐私聚合边界测试。 */
class AdoptionAnalyticsServiceTest {

    /** 重复幂等键必须返回原事实，不能再次写入。 */
    @Test
    void recordReturnsExistingEventForDuplicateKey() {
        SaasAdoptionEventMapper mapper = mock(SaasAdoptionEventMapper.class);
        SaasAdoptionEventEntity existing = event(1L, AdoptionEventType.PRODUCTION_SUCCEEDED, AdoptionEventSource.PRODUCTION, "same");
        when(mapper.selectOne(any())).thenReturn(existing);
        AdoptionAnalyticsService service = new AdoptionAnalyticsService(mapper, new SaasGovernanceProperties());

        SaasAdoptionEventEntity result = service.record(new AdoptionAnalyticsService.EventCommand(
                1L, 9L, "release", "run", AdoptionEventType.PRODUCTION_SUCCEEDED,
                AdoptionEventSource.PRODUCTION, "same", LocalDateTime.now(), "{}"));

        assertSame(existing, result);
        verify(mapper, never()).insert(any(SaasAdoptionEventEntity.class));
    }

    /** 草稿测试成功不得抬高生产价值漏斗。 */
    @Test
    void reportExcludesDraftTestTraffic() {
        SaasAdoptionEventMapper mapper = mock(SaasAdoptionEventMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                event(1L, AdoptionEventType.PRODUCTION_SUCCEEDED, AdoptionEventSource.DRAFT_TEST, "test"),
                event(1L, AdoptionEventType.TENANT_OPENED, AdoptionEventSource.PRODUCTION, "open")));
        AdoptionAnalyticsService service = new AdoptionAnalyticsService(mapper, new SaasGovernanceProperties());

        var report = service.report(new SecurityUser(7L, 1L, "admin", "ADMIN"), LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0, report.funnel().stream()
                .filter(item -> item.eventType() == AdoptionEventType.PRODUCTION_SUCCEEDED)
                .findFirst().orElseThrow().eventCount());
        assertFalse(report.firstValue().reached());
    }

    /** 跨租户样本不足时不得返回可推断其他客户的基准值。 */
    @Test
    void benchmarkHidesValuesBelowMinimumSample() {
        SaasAdoptionEventMapper mapper = mock(SaasAdoptionEventMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(event(1L, AdoptionEventType.PRODUCTION_SUCCEEDED,
                AdoptionEventSource.PRODUCTION, "one")));
        SaasGovernanceProperties properties = new SaasGovernanceProperties();
        properties.setAdoptionMinimumBenchmarkTenants(5);
        AdoptionAnalyticsService service = new AdoptionAnalyticsService(mapper, properties);

        var report = service.benchmark(new SecurityUser(1L, 1L, "root", "SUPER_ADMIN"), null, null);

        assertFalse(report.available());
        assertNull(report.productionActivationRate());
        assertNull(report.averageFirstValueMinutes());
    }

    /** 采用报告必须按事件发生月份保留 cohort 分界。 */
    @Test
    void reportGroupsCohortsByEventMonth() {
        SaasAdoptionEventMapper mapper = mock(SaasAdoptionEventMapper.class);
        SaasAdoptionEventEntity january = event(1L, AdoptionEventType.PRODUCTION_SUCCEEDED,
                AdoptionEventSource.PRODUCTION, "jan");
        january.setOccurredAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        SaasAdoptionEventEntity february = event(1L, AdoptionEventType.WEEKLY_ACTIVE,
                AdoptionEventSource.PRODUCTION, "feb");
        february.setOccurredAt(LocalDateTime.of(2026, 2, 10, 10, 0));
        when(mapper.selectList(any())).thenReturn(List.of(january, february));
        AdoptionAnalyticsService service = new AdoptionAnalyticsService(mapper, new SaasGovernanceProperties());

        var report = service.report(new SecurityUser(7L, 1L, "admin", "ADMIN"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));

        assertEquals(List.of("2026-01", "2026-02"),
                report.cohorts().stream().map(AdoptionAnalyticsService.CohortMetric::cohortMonth).toList());
    }

    private SaasAdoptionEventEntity event(Long tenantId, AdoptionEventType type, AdoptionEventSource source, String key) {
        SaasAdoptionEventEntity entity = new SaasAdoptionEventEntity();
        entity.setTenantId(tenantId);
        entity.setApplicationId(10L);
        entity.setEventType(type.name());
        entity.setEventSource(source.name());
        entity.setSchemaVersion(1);
        entity.setIdempotencyKey(key);
        entity.setOccurredAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
