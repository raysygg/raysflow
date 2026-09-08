package com.acme.agentstudio.application.runtime;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时工具（Tool）调用频率限制器（Tool Rate Limiter）。
 * 在 Worker 或 Agent 准备发起工具调用前，基于 ConcurrentHashMap 与 60 秒滑动时间窗口（WINDOW_SECONDS = 60L）
 * 按 "tenantId:toolId" 维度限制每分钟最高允许调用的上限次数（limit），超限时 tryAcquire() 返回 false 进行拦截阻断。
 */
@Component
public class ToolRateLimiter {

    /** 工具限流时间窗口时长（秒） */
    private static final long WINDOW_SECONDS = 60L;

    /** 内存中的工具频率计数器 Map（Key 为 tenantId:toolId） */
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * 尝试申请一次工具调用许可。
     *
     * @param tenantId 租户 ID
     * @param toolId 工具标识 ID
     * @param limit 每分钟允许的最高调用次数上限
     * @return true 表示成功获得许可，false 表示超出频率上限
     */
    public boolean tryAcquire(long tenantId, String toolId, int limit) {
        if (tenantId <= 0 || toolId == null || toolId.isBlank() || limit < 1) {
            throw new IllegalArgumentException("申请工具调用限流许可时，租户 ID 必须大于零，工具 ID 不能为空且限制次数 limit 必须至少为 1。");
        }

        String key = tenantId + ":" + toolId;
        long now = Instant.now().getEpochSecond();

        Counter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || (now - current.windowStart()) >= WINDOW_SECONDS) {
                return new Counter(now, 1);
            }
            return new Counter(current.windowStart(), current.count() + 1);
        });

        return counter.count() <= limit;
    }

    /** 工具频率窗口计数器 Record */
    private record Counter(long windowStart, int count) {
    }
}

