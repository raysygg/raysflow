package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.GovernedWorkerMessage;
import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 受治理的分布式 Worker 协议组件（Governed Worker Protocol）。
 * 负责开源框架 Worker 执行节点与平台之间的分布式租约（Worker Lease）、心跳续约（Heartbeat）、
 * 消息递增序号连续性校验（Sequence Continuity Check）及过期节点失效回收。
 */
@Service
public class GovernedWorkerProtocol {

    /** 默认 Worker 租约有效时长（秒） */
    private static final long DEFAULT_LEASE_SECONDS = 30L;

    /** 内存中活跃 Worker 租约 Map */
    private final Map<String, WorkerLease> leases = new ConcurrentHashMap<>();

    /**
     * 为指定 Worker 节点针对特定 Run 申请分配租约。
     *
     * @param workerId Worker 节点 ID
     * @param runId 运行执行 ID
     * @param framework 原生框架 NativeFramework
     * @return 分配的 Worker 租约实体 WorkerLease
     */
    public WorkerLease acquire(String workerId, String runId, NativeFramework framework) {
        if (workerId == null || workerId.isBlank() || runId == null || runId.isBlank() || framework == null) {
            throw new IllegalArgumentException("Worker 节点 ID、Run 执行 ID 与原生框架 NativeFramework 均不能为空。");
        }
        WorkerLease lease = new WorkerLease(
                UUID.randomUUID().toString(),
                workerId,
                runId,
                framework,
                Instant.now().plusSeconds(DEFAULT_LEASE_SECONDS),
                -1L
        );
        leases.put(lease.leaseId(), lease);
        return lease;
    }

    /**
     * 接收并校验来自 Worker 的治理消息，确保租约有效且序号递增连续。
     *
     * @param message 治理型 Worker 消息 GovernedWorkerMessage
     * @return 更新后的 Worker 租约实体 WorkerLease
     */
    public WorkerLease accept(GovernedWorkerMessage message) {
        WorkerLease lease = leases.get(message.leaseId());
        if (lease == null
                || !lease.workerId().equals(message.workerId())
                || !lease.runId().equals(message.runId())
                || lease.framework() != message.framework()) {
            throw new IllegalArgumentException("Worker 消息与当前持有者的租约属性不匹配。");
        }
        if (!Instant.now().isBefore(lease.expiresAt())) {
            throw new IllegalStateException("当前 Worker 租约已超时失效，请先重新申请租约。");
        }

        long expectedSequence = lease.lastSequence() + 1;
        if (message.sequence() != expectedSequence) {
            throw new IllegalStateException("Worker 发送的消息序号乱序，期望序号为：" + expectedSequence + "，实际为：" + message.sequence());
        }

        WorkerLease updated = new WorkerLease(
                lease.leaseId(),
                lease.workerId(),
                lease.runId(),
                lease.framework(),
                lease.expiresAt(),
                message.sequence()
        );
        leases.put(lease.leaseId(), updated);
        return updated;
    }

    /**
     * 接收 Worker 节点的定期心跳并续期租约。
     *
     * @param leaseId 租约 ID
     * @return 续期后的 Worker 租约实体 WorkerLease
     */
    public WorkerLease heartbeat(String leaseId) {
        WorkerLease lease = leases.get(leaseId);
        if (lease == null) {
            throw new IllegalArgumentException("未找到指定的 Worker 租约：" + leaseId);
        }
        if (!Instant.now().isBefore(lease.expiresAt())) {
            throw new IllegalStateException("当前 Worker 租约已经超时过期，无法续期。");
        }
        WorkerLease updated = new WorkerLease(
                lease.leaseId(),
                lease.workerId(),
                lease.runId(),
                lease.framework(),
                Instant.now().plusSeconds(DEFAULT_LEASE_SECONDS),
                lease.lastSequence()
        );
        leases.put(leaseId, updated);
        return updated;
    }

    /**
     * 主动释放租约。
     *
     * @param leaseId 租约 ID
     */
    public void release(String leaseId) {
        leases.remove(leaseId);
    }

    /**
     * 扫频回收所有过期的 Worker 租约，以便其他健康的 Worker 节点接管失联的 Run。
     *
     * @return 回收清理的租约数量
     */
    public int reclaimExpired() {
        Instant now = Instant.now();
        int reclaimed = 0;
        for (Map.Entry<String, WorkerLease> entry : leases.entrySet()) {
            if (!now.isBefore(entry.getValue().expiresAt()) && leases.remove(entry.getKey(), entry.getValue())) {
                reclaimed++;
            }
        }
        return reclaimed;
    }

    /**
     * Worker 分布式租约凭证 Record。
     *
     * @param leaseId 租约全局唯一 ID
     * @param workerId Worker 实例 ID
     * @param runId 关联运行 ID
     * @param framework 原生框架
     * @param expiresAt 租约到期绝对时间
     * @param lastSequence 上一次成功挂接的消息递增序号
     */
    public record WorkerLease(
            String leaseId,
            String workerId,
            String runId,
            NativeFramework framework,
            Instant expiresAt,
            long lastSequence
    ) {
        public WorkerLease {
            if (leaseId == null || leaseId.isBlank()
                    || workerId == null || workerId.isBlank()
                    || runId == null || runId.isBlank()
                    || framework == null
                    || expiresAt == null) {
                throw new IllegalArgumentException("WorkerLease 构造参数中的任意必选属性均不能为空。");
            }
        }
    }
}

