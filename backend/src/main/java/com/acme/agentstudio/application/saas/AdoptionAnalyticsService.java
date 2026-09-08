package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventSource;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdoptionEventType;
import com.acme.agentstudio.domain.saas.SaasGovernancePermissions;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasAdoptionEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasAdoptionEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采用度事实服务：写入幂等业务事件，并从事件事实计算产品价值指标。
 */
@Service
public class AdoptionAnalyticsService {
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final Set<AdoptionEventSource> CUSTOMER_VALUE_SOURCES = EnumSet.of(AdoptionEventSource.PRODUCTION);
    private final SaasAdoptionEventMapper eventMapper;
    private final SaasGovernanceProperties properties;

    public AdoptionAnalyticsService(SaasAdoptionEventMapper eventMapper, SaasGovernanceProperties properties) {
        this.eventMapper = eventMapper;
        this.properties = properties;
    }

    @Transactional
    public SaasAdoptionEventEntity record(EventCommand command) {
        requireCommand(command);
        SaasAdoptionEventEntity existing = eventMapper.selectOne(new LambdaQueryWrapper<SaasAdoptionEventEntity>()
                .eq(SaasAdoptionEventEntity::getTenantId, command.tenantId())
                .eq(SaasAdoptionEventEntity::getIdempotencyKey, command.idempotencyKey()));
        if (existing != null) return existing;
        SaasAdoptionEventEntity entity = new SaasAdoptionEventEntity();
        entity.setTenantId(command.tenantId());
        entity.setApplicationId(command.applicationId());
        entity.setReleaseId(command.releaseId());
        entity.setRunId(command.runId());
        entity.setEventType(command.eventType().name());
        entity.setSchemaVersion(properties.getAdoptionEventSchemaVersion());
        entity.setEventSource(command.source().name());
        entity.setIdempotencyKey(command.idempotencyKey());
        entity.setPropertiesJson(command.safePropertiesJson());
        entity.setOccurredAt(command.occurredAt() == null ? LocalDateTime.now() : command.occurredAt());
        entity.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(entity);
        return entity;
    }

    public AdoptionReport report(SecurityUser actor, LocalDate from, LocalDate to) {
        requireActor(actor);
        DateRange range = range(from, to);
        List<SaasAdoptionEventEntity> events = tenantEvents(actor.getTenantId(), range);
        List<SaasAdoptionEventEntity> valueEvents = events.stream().filter(this::isCustomerValueEvent).toList();
        List<FunnelStage> funnel = List.of(
                stage(valueEvents, AdoptionEventType.TENANT_OPENED, "租户已开通"),
                stage(valueEvents, AdoptionEventType.DRAFT_CREATED, "已创建应用草稿"),
                stage(valueEvents, AdoptionEventType.TEST_SUCCEEDED, "草稿测试成功"),
                stage(valueEvents, AdoptionEventType.RELEASE_PUBLISHED, "应用已发布"),
                stage(valueEvents, AdoptionEventType.PRODUCTION_SUCCEEDED, "生产运行成功"));
        LocalDateTime opened = first(valueEvents, AdoptionEventType.TENANT_OPENED);
        LocalDateTime published = first(valueEvents, AdoptionEventType.RELEASE_PUBLISHED);
        LocalDateTime production = first(valueEvents, AdoptionEventType.PRODUCTION_SUCCEEDED);
        Long firstValueMinutes = opened != null && published != null && production != null && !production.isBefore(published)
                ? Duration.between(opened, production).toMinutes() : null;
        long weeklyActive = activeApplications(valueEvents, range.to().minusDays(6).atStartOfDay());
        long monthlyActive = activeApplications(valueEvents, range.to().withDayOfMonth(1).atStartOfDay());
        long qualityReached = count(valueEvents, AdoptionEventType.QUALITY_REACHED);
        return new AdoptionReport(properties.getAdoptionEventSchemaVersion(), range.from(), range.to(), funnel,
                new FirstValueMetric(firstValueMinutes, production != null), new ActiveMetric(weeklyActive, monthlyActive),
                new QualityMetric(qualityReached > 0, qualityReached), cohorts(valueEvents));
    }

    public DataQualityReport dataQuality(SecurityUser actor, LocalDate from, LocalDate to) {
        requireActor(actor);
        DateRange range = range(from, to);
        List<SaasAdoptionEventEntity> events = tenantEvents(actor.getTenantId(), range);
        long invalid = events.stream().filter(this::invalid).count();
        LocalDateTime delayedBefore = LocalDateTime.now().minusHours(properties.getAdoptionMaximumEventDelayHours());
        long delayed = events.stream().filter(item -> item.getOccurredAt() != null && item.getCreatedAt() != null
                && item.getOccurredAt().isBefore(delayedBefore) && item.getCreatedAt().isAfter(item.getOccurredAt().plusHours(properties.getAdoptionMaximumEventDelayHours()))).count();
        boolean abnormal = events.size() > properties.getAdoptionAbnormalDailyEventLimit() * Math.max(1, range.days());
        return new DataQualityReport(events.size(), invalid, delayed, 0, abnormal,
                invalid == 0 && !abnormal ? "数据质量正常" : "存在需处理的事件质量问题");
    }

    public BenchmarkReport benchmark(SecurityUser actor, LocalDate from, LocalDate to) {
        requireActor(actor);
        if (!actor.hasRole(ROLE_SUPER_ADMIN) && !actor.hasRole(SaasGovernancePermissions.CROSS_TENANT_SUMMARY)) {
            throw new IllegalArgumentException("当前角色无权查看跨租户汇总");
        }
        DateRange range = range(from, to);
        List<SaasAdoptionEventEntity> events = eventMapper.selectList(new LambdaQueryWrapper<SaasAdoptionEventEntity>()
                .ge(SaasAdoptionEventEntity::getOccurredAt, range.from().atStartOfDay())
                .le(SaasAdoptionEventEntity::getOccurredAt, range.to().atTime(LocalTime.MAX)));
        var byTenant = events.stream().filter(this::isCustomerValueEvent).collect(Collectors.groupingBy(SaasAdoptionEventEntity::getTenantId));
        int sampleSize = byTenant.size();
        if (sampleSize < properties.getAdoptionMinimumBenchmarkTenants()) {
            return new BenchmarkReport(false, sampleSize, properties.getAdoptionMinimumBenchmarkTenants(), null, null);
        }
        long activated = byTenant.values().stream().filter(items -> first(items, AdoptionEventType.PRODUCTION_SUCCEEDED) != null).count();
        List<Long> minutes = byTenant.values().stream().map(this::firstValueMinutes).filter(Objects::nonNull).toList();
        Double average = minutes.isEmpty() ? null : minutes.stream().mapToLong(Long::longValue).average().orElse(0D);
        return new BenchmarkReport(true, sampleSize, properties.getAdoptionMinimumBenchmarkTenants(), activated / (double) sampleSize, average);
    }

    public List<EventDefinition> definitions() {
        return List.of(new EventDefinition(AdoptionEventType.TENANT_OPENED, "租户开通", true), new EventDefinition(AdoptionEventType.DRAFT_CREATED, "创建草稿", true),
                new EventDefinition(AdoptionEventType.TEST_SUCCEEDED, "测试成功", false), new EventDefinition(AdoptionEventType.RELEASE_PUBLISHED, "应用发布", true),
                new EventDefinition(AdoptionEventType.PRODUCTION_SUCCEEDED, "生产成功", true), new EventDefinition(AdoptionEventType.WEEKLY_ACTIVE, "周活跃", true),
                new EventDefinition(AdoptionEventType.QUALITY_REACHED, "质量达标", true), new EventDefinition(AdoptionEventType.RENEWAL_RISK, "续费风险", true));
    }

    private List<SaasAdoptionEventEntity> tenantEvents(Long tenantId, DateRange range) {
        return eventMapper.selectList(new LambdaQueryWrapper<SaasAdoptionEventEntity>()
                .eq(SaasAdoptionEventEntity::getTenantId, tenantId).ge(SaasAdoptionEventEntity::getOccurredAt, range.from().atStartOfDay())
                .le(SaasAdoptionEventEntity::getOccurredAt, range.to().atTime(LocalTime.MAX)).orderByAsc(SaasAdoptionEventEntity::getOccurredAt));
    }

    private FunnelStage stage(List<SaasAdoptionEventEntity> events, AdoptionEventType type, String label) {
        return new FunnelStage(type, label, count(events, type), first(events, type));
    }

    private long count(List<SaasAdoptionEventEntity> events, AdoptionEventType type) {
        return events.stream().filter(item -> type.name().equals(item.getEventType())).count();
    }

    private LocalDateTime first(List<SaasAdoptionEventEntity> events, AdoptionEventType type) {
        return events.stream().filter(item -> type.name().equals(item.getEventType())).map(SaasAdoptionEventEntity::getOccurredAt).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
    }

    private long activeApplications(List<SaasAdoptionEventEntity> events, LocalDateTime since) {
        return events.stream().filter(item -> AdoptionEventType.PRODUCTION_SUCCEEDED.name().equals(item.getEventType()) && item.getOccurredAt() != null && !item.getOccurredAt().isBefore(since)).map(SaasAdoptionEventEntity::getApplicationId).filter(Objects::nonNull).distinct().count();
    }

    private List<CohortMetric> cohorts(List<SaasAdoptionEventEntity> events) {
        LinkedHashMap<YearMonth, List<SaasAdoptionEventEntity>> grouped = events.stream().filter(item -> item.getOccurredAt() != null).collect(Collectors.groupingBy(item -> YearMonth.from(item.getOccurredAt()), LinkedHashMap::new, Collectors.toList()));
        ArrayList<CohortMetric> result = new ArrayList<>();
        grouped.forEach((month, items) -> result.add(new CohortMetric(month.toString(), count(items, AdoptionEventType.PRODUCTION_SUCCEEDED), count(items, AdoptionEventType.WEEKLY_ACTIVE))));
        return List.copyOf(result);
    }

    private Long firstValueMinutes(List<SaasAdoptionEventEntity> events) {
        LocalDateTime opened = first(events, AdoptionEventType.TENANT_OPENED), published = first(events, AdoptionEventType.RELEASE_PUBLISHED), production = first(events, AdoptionEventType.PRODUCTION_SUCCEEDED);
        return opened != null && published != null && production != null && !production.isBefore(published) ? Duration.between(opened, production).toMinutes() : null;
    }

    private boolean isCustomerValueEvent(SaasAdoptionEventEntity item) {
        try {
            return CUSTOMER_VALUE_SOURCES.contains(AdoptionEventSource.valueOf(item.getEventSource()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean invalid(SaasAdoptionEventEntity item) {
        return item.getTenantId() == null || item.getEventType() == null || item.getSchemaVersion() == null || item.getEventSource() == null || item.getIdempotencyKey() == null || item.getOccurredAt() == null;
    }

    private DateRange range(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        if (resolvedFrom.isAfter(resolvedTo)) throw new IllegalArgumentException("开始日期不能晚于结束日期");
        return new DateRange(resolvedFrom, resolvedTo);
    }

    private void requireActor(SecurityUser actor) {
        if (actor == null || actor.getTenantId() == null) throw new IllegalArgumentException("当前身份无效");
    }

    private void requireCommand(EventCommand command) {
        if (command == null || command.tenantId() == null || command.eventType() == null || command.source() == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank())
            throw new IllegalArgumentException("采用事件缺少必要字段");
    }

    private record DateRange(LocalDate from, LocalDate to) {
        long days() {
            return Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).toDays();
        }
    }

    public record EventCommand(Long tenantId, Long applicationId, String releaseId, String runId,
                               AdoptionEventType eventType, AdoptionEventSource source, String idempotencyKey,
                               LocalDateTime occurredAt, String safePropertiesJson) {
    }

    public record EventDefinition(AdoptionEventType eventType, String label, boolean productionValueEvent) {
    }

    public record FunnelStage(AdoptionEventType eventType, String label, long eventCount,
                              LocalDateTime firstOccurredAt) {
    }

    public record FirstValueMetric(Long minutes, boolean reached) {
    }

    public record ActiveMetric(long weeklyActiveApplications, long monthlyActiveApplications) {
    }

    public record QualityMetric(boolean reached, long reachedEventCount) {
    }

    public record CohortMetric(String cohortMonth, long productionSuccessEvents, long weeklyActiveEvents) {
    }

    public record AdoptionReport(int definitionVersion, LocalDate from, LocalDate to, List<FunnelStage> funnel,
                                 FirstValueMetric firstValue, ActiveMetric active, QualityMetric quality,
                                 List<CohortMetric> cohorts) {
    }

    public record DataQualityReport(long totalEvents, long incompleteEvents, long delayedEvents, long duplicateEvents,
                                    boolean abnormalTraffic, String conclusion) {
    }

    public record BenchmarkReport(boolean available, int sampleTenants, int minimumSampleTenants,
                                  Double productionActivationRate, Double averageFirstValueMinutes) {
    }
}
