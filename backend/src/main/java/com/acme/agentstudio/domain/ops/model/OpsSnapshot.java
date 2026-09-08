package com.acme.agentstudio.domain.ops.model;

import java.util.List;

/**
 * 系统运维与健康度诊断实时快照 Record（Ops Snapshot）。
 *
 * @param todayRequests 今日总请求量
 * @param averageLatency 平均响应时延
 * @param ragHitRate RAG 检索命中率
 * @param failureRate 异常失败率
 * @param alerts 当前触发的运维告警提示列表
 */
public record OpsSnapshot(
        String todayRequests,
        String averageLatency,
        String ragHitRate,
        String failureRate,
        List<String> alerts
) {
}

