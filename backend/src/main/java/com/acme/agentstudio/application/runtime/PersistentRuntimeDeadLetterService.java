package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DeadLetterStatus;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.DispositionType;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterDispositionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterDispositionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运行时死信队列持久化与处置服务（Persistent Runtime Dead-Letter Service）。
 * 负责收集与持久化各种无法恢复的运行异常/失败任务为死信实体（RuntimeDeadLetterEntity），
 * 维护死信状态机（OPEN -> UNDER_REVIEW -> RESOLVED / IGNORED），并保留每次处置动作的操作日志审计（Disposition Trail）。
 */
@Service
public class PersistentRuntimeDeadLetterService {

    /** 死信队列 Mapper */
    private final RuntimeDeadLetterMapper deadLetterMapper;

    /** 死信处置审计 Mapper */
    private final RuntimeDeadLetterDispositionMapper dispositionMapper;

    /**
     * 构造函数注入依赖 Mapper。
     */
    public PersistentRuntimeDeadLetterService(
            RuntimeDeadLetterMapper deadLetterMapper,
            RuntimeDeadLetterDispositionMapper dispositionMapper
    ) {
        this.deadLetterMapper = deadLetterMapper;
        this.dispositionMapper = dispositionMapper;
    }

    /**
     * 记录一条失败的任务死信实体。
     *
     * @param tenantId 租户 ID
     * @param taskId 任务尝试 ID
     * @param runId 运行 ID
     * @param category 错误类型枚举 ErrorCategory
     * @param errorSummary 错误摘要文本
     * @param snapshotJson 运行时快照 JSON
     * @return 创建的死信实体 RuntimeDeadLetterEntity
     */
    @Transactional
    public RuntimeDeadLetterEntity record(
            Long tenantId,
            Long taskId,
            String runId,
            ErrorCategory category,
            String errorSummary,
            String snapshotJson
    ) {
        RuntimeDeadLetterEntity entity = new RuntimeDeadLetterEntity();
        entity.setTenantId(tenantId);
        entity.setTaskId(taskId);
        entity.setRunId(runId);
        entity.setErrorCategory(category.name());
        entity.setErrorSummary(errorSummary);
        entity.setSnapshotJson(snapshotJson);
        entity.setDeadLetterStatus(DeadLetterStatus.OPEN.name());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        deadLetterMapper.insert(entity);
        return entity;
    }

    /**
     * 按死信状态条件列表查询当前租户下的死信实体。
     *
     * @param user 当前登录 SecurityUser
     * @param status 死信状态（可选）
     * @return 死信实体列表 List&lt;RuntimeDeadLetterEntity&gt;
     */
    public List<RuntimeDeadLetterEntity> list(SecurityUser user, DeadLetterStatus status) {
        requireTenant(user);
        LambdaQueryWrapper<RuntimeDeadLetterEntity> query = new LambdaQueryWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                .orderByDesc(RuntimeDeadLetterEntity::getCreatedAt);

        if (status != null) {
            query.eq(RuntimeDeadLetterEntity::getDeadLetterStatus, status.name());
        }
        return deadLetterMapper.selectList(query);
    }

    /**
     * 对特定死信任务执行处置（重试/驳回/忽略/指派人），并持久化操作审计记录。
     *
     * @param user 当前登录 SecurityUser
     * @param deadLetterId 死信 ID
     * @param type 处置动作类型 DispositionType
     * @param nextStatus 下一个死信状态 DeadLetterStatus
     * @param reason 处置原因说明
     * @param assignee 被指派的接管人账号 ID
     */
    @Transactional
    public void dispose(
            SecurityUser user,
            Long deadLetterId,
            DispositionType type,
            DeadLetterStatus nextStatus,
            String reason,
            Long assignee
    ) {
        requireTenant(user);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("处置死信任务时必须填写真实的处置原因说明。");
        }

        RuntimeDeadLetterEntity current = deadLetterMapper.selectOne(new LambdaQueryWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                .eq(RuntimeDeadLetterEntity::getId, deadLetterId));

        if (current == null) {
            throw new IllegalArgumentException("指定死信记录不存在或不属于当前租户。");
        }

        DeadLetterStatus previous = DeadLetterStatus.valueOf(current.getDeadLetterStatus());
        deadLetterMapper.update(null, new LambdaUpdateWrapper<RuntimeDeadLetterEntity>()
                .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                .eq(RuntimeDeadLetterEntity::getId, deadLetterId)
                .eq(RuntimeDeadLetterEntity::getDeadLetterStatus, previous.name())
                .set(RuntimeDeadLetterEntity::getDeadLetterStatus, nextStatus.name())
                .set(RuntimeDeadLetterEntity::getAssignedTo, assignee));

        RuntimeDeadLetterDispositionEntity audit = new RuntimeDeadLetterDispositionEntity();
        audit.setTenantId(user.getTenantId());
        audit.setDeadLetterId(deadLetterId);
        audit.setDispositionType(type.name());
        audit.setPreviousStatus(previous.name());
        audit.setNextStatus(nextStatus.name());
        audit.setReason(reason.trim());
        audit.setOperatorId(user.getUserId());
        audit.setCreatedAt(LocalDateTime.now());
        dispositionMapper.insert(audit);
    }

    /** 租户安全用户校验 */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }
}

