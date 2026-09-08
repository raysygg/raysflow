package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.RuntimeTelemetryEvent;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliSnapshot;
import com.acme.agentstudio.domain.runtime.RuntimeTelemetryContracts.SliType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 运行时事件遥测与 SLI 核心指标聚合服务（Runtime Telemetry Service）。
 * 在内存中使用线程安全的 CopyOnWriteArrayList 维护最近的事件缓存（限制最多 MAX_EVENTS = 10,000 条），
 * 强制通过 RuntimeTelemetrySanitizer 脱敏 safeSummary。
 * 提供多维度的 SliSnapshot 聚合计算（包含队列延迟 QUEUE_LATENCY、成功率 SUCCESS_RATE、失败率 FAILURE_RATE、P95 响应耗时 P95_LATENCY 以及故障恢复率 RECOVERY_RATE）。
 */
@Service
public class RuntimeTelemetryService {

    /** 内存事件最大保留队列限制 */
    private static final int MAX_EVENTS = 10_000;

    /** 内存并发事件队列 CopyOnWriteArrayList */
    private final CopyOnWriteArrayList<RuntimeTelemetryEvent> events = new CopyOnWriteArrayList<>();

    /**
     * 记录一条脱敏后的遥测事件 RuntimeTelemetryEvent。
     *
     * @param event 待记录的遥测事件
     */
    public void record(RuntimeTelemetryEvent event) {
        if (event == null || event.correlation() == null || event.safeSummary() == null) {
            return;
        }

        events.add(new RuntimeTelemetryEvent(
                event.correlation(),
                event.stage(),
                safeEventType(event.eventType()),
                Math.max(0, event.durationMs()),
                event.success(),
                RuntimeTelemetrySanitizer.summary(event.safeSummary()),
                (event.occurredAt() == null) ? Instant.now() : event.occurredAt()
        ));

        while (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
    }

    /**
     * 重载：基于租户和运行 ID 提取指定滑动窗口的 SLI 指标快照。
     *
     * @param tenantId 租户 ID
     * @param runId 运行 ID
     * @param window 时间窗口 Duration
     * @return 5 项核心 SLI 快照列表 List&lt;SliSnapshot&gt;
     */
    public List<SliSnapshot> snapshots(Long tenantId, String runId, Duration window) {
        return snapshots(tenantId, null, null, runId, window);
    }

    /**
     * 多维条件检索并计算滑动窗口内的 SLI 核心指标快照。
     *
     * @param tenantId 租户 ID
     * @param applicationId 应用 ID（可选）
     * @param releaseId 发布版本 ID（可选）
     * @param runId 运行 ID（可选）
     * @param window 滑动时间窗口（默认 60 分钟）
     * @return SLI 核心指标快照列表 List&lt;SliSnapshot&gt;
     */
    public List<SliSnapshot> snapshots(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String runId,
            Duration window
    ) {
        Instant since = Instant.now().minus((window == null) ? Duration.ofMinutes(60) : window);

        List<RuntimeTelemetryEvent> selected = events.stream()
                .filter(item -> tenantId == null || tenantId.equals(item.correlation().tenantId()))
                .filter(item -> applicationId == null || applicationId.equals(item.correlation().applicationId()))
                .filter(item -> releaseId == null || releaseId.equals(item.correlation().releaseId()))
                .filter(item -> runId == null || runId.equals(item.correlation().runId()))
                .filter(item -> item.occurredAt().isAfter(since))
                .toList();

        int count = selected.size();
        double success = (count == 0) ? 0.0D : selected.stream().filter(RuntimeTelemetryEvent::success).count() * 1.0D / count;
        double failure = (count == 0) ? 0.0D : (1.0D - success);
        double p95 = percentile(selected.stream().map(RuntimeTelemetryEvent::durationMs).sorted().toList(), 0.95);

        List<RuntimeTelemetryEvent> queueEvents = selected.stream()
                .filter(item -> item.stage() == RuntimeTelemetryContracts.StageType.QUEUE)
                .toList();
        double queueLatency = queueEvents.stream().mapToLong(RuntimeTelemetryEvent::durationMs).average().orElse(0.0D);

        List<RuntimeTelemetryEvent> recoveryEvents = selected.stream()
                .filter(item -> item.eventType().startsWith("RECOVERY_"))
                .toList();
        double recoveryRate = recoveryEvents.isEmpty()
                ? 0.0D
                : recoveryEvents.stream().filter(RuntimeTelemetryEvent::success).count() * 1.0D / recoveryEvents.size();

        return List.of(
                new SliSnapshot(SliType.QUEUE_LATENCY, queueLatency, queueEvents.size(), "window", Instant.now()),
                new SliSnapshot(SliType.SUCCESS_RATE, success, count, "window", Instant.now()),
                new SliSnapshot(SliType.FAILURE_RATE, failure, count, "window", Instant.now()),
                new SliSnapshot(SliType.P95_LATENCY, p95, count, "window", Instant.now()),
                new SliSnapshot(SliType.RECOVERY_RATE, recoveryRate, recoveryEvents.size(), "window", Instant.now())
        );
    }

    /** 计算列表分位数 */
    private double percentile(List<Long> values, double ratio) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        int index = Math.min(values.size() - 1, (int) Math.ceil(values.size() * ratio) - 1);
        return values.get(index);
    }

    /** 安全事件类型名称转换 */
    private String safeEventType(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.substring(0, Math.min(64, value.length()));
    }
}

