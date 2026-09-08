package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;
import java.util.Map;

/**
 * 受安全治理管控的分布式 Worker 节点上报协议消息 Record（Governed Worker Message）。
 * 携带着 Worker ID workerId、租约 ID leaseId、运行任务 ID runId、适配框架 framework、
 * 消息类型 type (WorkerMessageType)、严格自增序号 sequence、载荷 Map payload 与发送时间 sentAt，禁止 Worker 绕过平台控制台篡改数据状态。
 *
 * @param workerId 发送节点的 Worker 物理标识
 * @param leaseId 领取的分布式任务租约 ID
 * @param runId 关联的运行任务 Run ID
 * @param framework Worker 运行的底层 Agent 框架（NativeFramework）
 * @param type 消息类型（WorkerMessageType）
 * @param sequence 消息序列号（从 0 开始自增）
 * @param payload 节点事件载荷数据
 * @param sentAt 消息发送时刻
 */
public record GovernedWorkerMessage(
        String workerId,
        String leaseId,
        String runId,
        NativeFramework framework,
        WorkerMessageType type,
        long sequence,
        Map<String, Object> payload,
        Instant sentAt
) {
    /** 紧凑构造函数做输入属性严格性校验 */
    public GovernedWorkerMessage {
        if (workerId == null || workerId.isBlank() || leaseId == null || leaseId.isBlank()
                || runId == null || runId.isBlank() || framework == null || type == null || sequence < 0) {
            throw new IllegalArgumentException("Worker 消息标识、租约、Run、框架和序号无效");
        }
        payload = (payload == null) ? Map.of() : Map.copyOf(payload);
        sentAt = (sentAt == null) ? Instant.now() : sentAt;
    }
}

