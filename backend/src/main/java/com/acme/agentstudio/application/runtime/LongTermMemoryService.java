package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.LongTermMemoryPolicy;
import com.acme.agentstudio.domain.runtime.model.LongTermMemoryRecord;
import com.acme.agentstudio.domain.runtime.model.MemoryLifecycleStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 长期记忆生命周期管理服务（Long-Term Memory Service）。
 * 负责 Agent 运行中产生的跨会话长期记忆提取（Extract）、置信度校验（Confidence Threshold）、
 * 敏感数据人工确认（Confirmation）、删除与过期丢弃（Retention & Expiration）等治理流程。
 */
@Service
public class LongTermMemoryService {

    /** 内存级长期记忆记录存储 Map */
    private final Map<String, LongTermMemoryRecord> records = new ConcurrentHashMap<>();

    /**
     * 从多轮对话或运行上下文中提取写入一条长期记忆。
     *
     * @param tenantId 租户 ID
     * @param namespace 命名空间
     * @param subject 记忆主体/实体名
     * @param content 记忆正文内容
     * @param confidence 抽取置信度 (0.0 - 1.0)
     * @param sensitive 是否包含敏感隐私数据
     * @param sourceRunId 来源 Run 执行 ID
     * @param policy 长期记忆保留与审核策略 LongTermMemoryPolicy
     * @return 提取生成的记忆实体 LongTermMemoryRecord
     */
    public LongTermMemoryRecord extract(
            long tenantId,
            String namespace,
            String subject,
            String content,
            double confidence,
            boolean sensitive,
            String sourceRunId,
            LongTermMemoryPolicy policy
    ) {
        LongTermMemoryPolicy effective = (policy == null) ? LongTermMemoryPolicy.defaults() : policy;
        if (confidence < effective.minimumConfidence()) {
            throw new IllegalArgumentException("提取的记忆置信度 [" + confidence + "] 低于策略设定的最小写入门槛 [" + effective.minimumConfidence() + "]。");
        }

        MemoryLifecycleStatus status;
        if (sensitive && effective.requireConfirmationForSensitive()) {
            status = MemoryLifecycleStatus.CANDIDATE;
        } else if (effective.autoWrite()) {
            status = MemoryLifecycleStatus.CONFIRMED;
        } else {
            status = MemoryLifecycleStatus.CANDIDATE;
        }

        Instant now = Instant.now();
        LongTermMemoryRecord record = new LongTermMemoryRecord(
                UUID.randomUUID().toString(),
                tenantId,
                namespace,
                subject,
                content,
                confidence,
                sensitive,
                status,
                sourceRunId,
                now.plus(effective.retentionDays(), ChronoUnit.DAYS),
                now
        );
        records.put(record.memoryId(), record);
        return record;
    }

    /**
     * 人工审定或用户确认/拒绝指定的候选记忆。
     *
     * @param tenantId 租户 ID
     * @param memoryId 记忆 ID
     * @param accepted 是否采纳接受
     * @return 确认后的记忆实体 LongTermMemoryRecord
     */
    public LongTermMemoryRecord confirm(long tenantId, String memoryId, boolean accepted) {
        LongTermMemoryRecord current = requireTenant(tenantId, memoryId);
        MemoryLifecycleStatus next = accepted ? MemoryLifecycleStatus.CONFIRMED : MemoryLifecycleStatus.REJECTED;
        LongTermMemoryRecord updated = replace(current, next);
        records.put(memoryId, updated);
        return updated;
    }

    /**
     * 逻辑删除指定的长期记忆条目。
     *
     * @param tenantId 租户 ID
     * @param memoryId 记忆 ID
     */
    public void delete(long tenantId, String memoryId) {
        LongTermMemoryRecord current = requireTenant(tenantId, memoryId);
        records.put(memoryId, replace(current, MemoryLifecycleStatus.DELETED));
    }

    /**
     * 校验并读取可用（未过期且处于 CONFIRMED 状态）的长期记忆。
     *
     * @param tenantId 租户 ID
     * @param memoryId 记忆 ID
     * @param now 当前校验时间
     * @return 可读的长期记忆实体 LongTermMemoryRecord
     */
    public LongTermMemoryRecord requireReadable(long tenantId, String memoryId, Instant now) {
        LongTermMemoryRecord current = requireTenant(tenantId, memoryId);
        if (!now.isBefore(current.expiresAt())) {
            records.put(memoryId, replace(current, MemoryLifecycleStatus.EXPIRED));
            throw new IllegalStateException("指定的长期记忆记录已超出保留天数过期。");
        }
        if (current.status() != MemoryLifecycleStatus.CONFIRMED) {
            throw new IllegalStateException("该长期记忆目前处于不可用状态：" + current.status());
        }
        return current;
    }

    /** 租户隔离存在性校验 */
    private LongTermMemoryRecord requireTenant(long tenantId, String memoryId) {
        LongTermMemoryRecord record = records.get(memoryId);
        if (record == null || record.tenantId() != tenantId) {
            throw new IllegalArgumentException("指定的长期记忆不存在或无权访问。");
        }
        return record;
    }

    /** 替换记忆状态并更新更新时间 */
    private LongTermMemoryRecord replace(LongTermMemoryRecord current, MemoryLifecycleStatus status) {
        return new LongTermMemoryRecord(
                current.memoryId(),
                current.tenantId(),
                current.namespace(),
                current.subject(),
                current.content(),
                current.confidence(),
                current.sensitive(),
                status,
                current.sourceRunId(),
                current.expiresAt(),
                Instant.now()
        );
    }
}

