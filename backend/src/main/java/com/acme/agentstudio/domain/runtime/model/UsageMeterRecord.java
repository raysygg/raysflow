package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 实时计费与商业治理 Runtime 资源用量计量明细 Record（Usage Meter Record）。
 * 记录计量唯一 ID meterId、租户 ID tenantId、关联 Run ID runId、防重复计费幂等 Key idempotencyKey、
 * 细粒度用量维度 Map (Token, CPU 时长, API 次数等) dimensions 与记录采集时间 recordedAt。
 *
 * @param meterId 计量日志唯一 ID
 * @param tenantId 归属租户物理 ID
 * @param runId 关联的运行任务 Run ID
 * @param idempotencyKey 计费防重复处理的幂等 Key
 * @param dimensions 多维度用量数据 Map（如 prompt_tokens, completion_tokens, tool_calls）
 * @param recordedAt 用量日志采集写入时间
 */
public record UsageMeterRecord(
        String meterId,
        long tenantId,
        String runId,
        String idempotencyKey,
        Map<String, Long> dimensions,
        Instant recordedAt
) {
    /** 紧凑构造函数做输入验证断言 */
    public UsageMeterRecord {
        if (meterId == null || meterId.isBlank() || tenantId <= 0 || runId == null || runId.isBlank()
                || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("用量记录标识、租户、Run 和幂等键不能为空");
        }
        dimensions = (dimensions == null) ? Map.of() : Map.copyOf(dimensions);
        recordedAt = (recordedAt == null) ? Instant.now() : recordedAt;
    }
}

