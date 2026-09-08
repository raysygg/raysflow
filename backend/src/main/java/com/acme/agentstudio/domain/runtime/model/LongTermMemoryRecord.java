package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;

/**
 * 跨会话长期记忆持久化事实条目实体 Record（Long-term Memory Record）。
 * 记录记忆 ID memoryId、租户 ID tenantId、命名空间 namespace、记忆主体 subject、记忆核心内容 content、
 * 置信度 confidence、是否敏感 sensitive、生命周期状态 status (MemoryLifecycleStatus)、来源 Run ID sourceRunId、到期失效时间 expiresAt 与更新时刻 updatedAt。
 *
 * @param memoryId 记忆实体唯一 ID
 * @param tenantId 归属租户物理 ID
 * @param namespace 记忆隔离命名空间（如 user_preference / business_fact）
 * @param subject 记忆所描述的主体实体编号或账户标识
 * @param content 提炼后的具体事实或偏好文本
 * @param confidence 抽取置信度分数（0.0 ~ 1.0）
 * @param sensitive 是否标记为敏感敏感字段
 * @param status 记忆生命周期状态（MemoryLifecycleStatus）
 * @param sourceRunId 提炼出该记忆的原真实 Run ID
 * @param expiresAt 记忆保留到期失效时间
 * @param updatedAt 最近一次提炼或修正更新时间
 */
public record LongTermMemoryRecord(
        String memoryId,
        long tenantId,
        String namespace,
        String subject,
        String content,
        double confidence,
        boolean sensitive,
        MemoryLifecycleStatus status,
        String sourceRunId,
        Instant expiresAt,
        Instant updatedAt
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public LongTermMemoryRecord {
        if (memoryId == null || memoryId.isBlank() || tenantId <= 0 || namespace == null || namespace.isBlank()
                || subject == null || subject.isBlank() || content == null || content.isBlank()
                || sourceRunId == null || sourceRunId.isBlank()) {
            throw new IllegalArgumentException("长期记忆标识、租户、命名空间、主体、内容和来源不能为空");
        }
        if (confidence < 0D || confidence > 1D || status == null || expiresAt == null) {
            throw new IllegalArgumentException("长期记忆置信度、状态和过期时间无效");
        }
        updatedAt = (updatedAt == null) ? Instant.now() : updatedAt;
    }
}

