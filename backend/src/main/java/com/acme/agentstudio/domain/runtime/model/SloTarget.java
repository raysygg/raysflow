package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台 Runtime 服务等级目标 SLO (Service Level Objective) 质量指标定义 Record（SLO Target）。
 * 包含服务层级 tier (ServiceTier)、可用性基线 availability (0.0~1.0)、运行成功率 runSuccessRate (0.0~1.0)、
 * P95 全链路响应延迟毫秒数 p95LatencyMillis、故障自动恢复率 recoveryRate、恢复点目标 rpoMinutes 与恢复时间目标 rtoMinutes。
 *
 * @param tier 服务质量分级（ServiceTier：STANDARD, BUSINESS, ENTERPRISE）
 * @param availability 整体系统可用性 SLA 目标（如 0.999）
 * @param runSuccessRate Run 任务整体执行成功率目标（如 0.99）
 * @param p95LatencyMillis P95 时延上限（毫秒）
 * @param recoveryRate 故障自动恢复成功率（如 0.95）
 * @param rpoMinutes 允许的最大数据丢失恢复点目标 RPO（分钟）
 * @param rtoMinutes 允许的最大服务中断恢复时间目标 RTO（分钟）
 */
public record SloTarget(
        ServiceTier tier,
        double availability,
        double runSuccessRate,
        long p95LatencyMillis,
        double recoveryRate,
        long rpoMinutes,
        long rtoMinutes
) {
    /** 紧凑构造函数做输入属性校验 */
    public SloTarget {
        if (tier == null || availability < 0D || availability > 1D || runSuccessRate < 0D || runSuccessRate > 1D
                || recoveryRate < 0D || recoveryRate > 1D || p95LatencyMillis < 1 || rpoMinutes < 0 || rtoMinutes < 1) {
            throw new IllegalArgumentException("SLO 目标参数无效");
        }
    }
}

