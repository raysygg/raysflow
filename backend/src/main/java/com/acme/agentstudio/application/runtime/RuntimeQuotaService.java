package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeQuota;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时并发与每分钟最高 Run 次数配额限流服务（Runtime Quota Service）。
 * 在应用触发 API / Webhook 入口时，按 60 秒滑动时间窗口限制每个应用下的每分钟最大运行次数（maxRunsPerMinute），超限时返回 false 进行降级阻断。
 */
@Service
public class RuntimeQuotaService {

    /** 速率窗口时长（秒） */
    private static final long RATE_WINDOW_SECONDS = 60L;

    /** 内存频率计数器 Map（Key 为 tenantId:applicationId） */
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * 尝试申请一次 Run 运行配额。
     *
     * @param tenantId 租户 ID
     * @param applicationId 应用 ID
     * @param quota 配额限制实体 RuntimeQuota
     * @return true 表示成功获取配额许可，false 表示已超出每分钟允许的最高 Run 限制
     */
    public boolean tryAcquire(long tenantId, long applicationId, RuntimeQuota quota) {
        if (tenantId <= 0 || applicationId <= 0 || quota == null) {
            throw new IllegalArgumentException("申请配额许可时，租户 ID、应用 ID 必须大于零且配额规则 RuntimeQuota 不能为空。");
        }

        String key = tenantId + ":" + applicationId;
        Counter counter = counters.compute(key, (ignored, current) -> {
            long now = Instant.now().getEpochSecond();
            if (current == null || (now - current.minuteStart()) >= RATE_WINDOW_SECONDS) {
                return new Counter(now, 1);
            }
            return new Counter(current.minuteStart(), current.count() + 1);
        });

        return counter.count() <= quota.maxRunsPerMinute();
    }

    /** 内部分钟滑动窗口计数器 Record */
    private record Counter(long minuteStart, int count) {
    }
}

