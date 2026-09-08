package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.RuntimeRecoveryProperties;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.AttemptStatus;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.ErrorCategory;
import com.acme.agentstudio.infrastructure.persistence.entity.AsyncTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeTaskAttemptEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeTaskAttemptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运行时任务 Worker 尝试（Attempt）与心跳租约生命周期服务（Runtime Task Attempt Service）。
 * 记录 Worker 每次认领任务生成的尝试实体 RuntimeTaskAttemptEntity（状态 CLAIMED），
 * 负责定期心跳续约（heartbeat 更新 leaseUntil）、完成结单（finish 标记 SUCCEEDED / FAILED），
 * 以及清理扫描超时断连并失效标记的租约记录（expireLeases 标记 EXPIRED）。
 */
@Service
public class RuntimeTaskAttemptService {

    /** 任务尝试 Mapper */
    private final RuntimeTaskAttemptMapper attemptMapper;

    /** 运行时故障恢复配置 */
    private final RuntimeRecoveryProperties properties;

    /**
     * 构造函数注入依赖 Mapper 与配置。
     */
    public RuntimeTaskAttemptService(
            RuntimeTaskAttemptMapper attemptMapper,
            RuntimeRecoveryProperties properties
    ) {
        this.attemptMapper = attemptMapper;
        this.properties = properties;
    }

    /**
     * Worker 认领 Task 任务，创建并持久化一条 CLAIMED 状态的尝试实体。
     *
     * @param task 待认领的异步任务实体 AsyncTaskEntity
     * @param workerId Worker 节点标识 ID
     * @return 新生成的任务尝试实体 RuntimeTaskAttemptEntity
     */
    @Transactional
    public RuntimeTaskAttemptEntity claim(AsyncTaskEntity task, String workerId) {
        RuntimeTaskAttemptEntity attempt = new RuntimeTaskAttemptEntity();
        attempt.setTenantId(task.getTenantId());
        attempt.setTaskId(task.getId());
        attempt.setRunId(task.getRunId());
        attempt.setAttemptNo(((task.getRetryCount() == null) ? 0 : task.getRetryCount()) + 1);
        attempt.setLeaseOwner(workerId);
        attempt.setAttemptStatus(AttemptStatus.CLAIMED.name());
        attempt.setLeaseUntil(LocalDateTime.now().plusSeconds(properties.getLeaseSeconds()));
        attempt.setHeartbeatAt(LocalDateTime.now());
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setCreatedAt(LocalDateTime.now());

        attemptMapper.insert(attempt);
        return attempt;
    }

    /**
     * 维持 Worker 对任务的租约心跳，刷新 heartbeatAt 与 leaseUntil。
     *
     * @param attemptId 尝试记录 ID
     * @param taskId 任务 ID
     * @param workerId Worker 节点标识 ID
     * @return true 表示成功更新心跳，false 表示尝试不存在或已非本 Worker 占据
     */
    public boolean heartbeat(Long attemptId, Long taskId, String workerId) {
        int updated = attemptMapper.update(null, new LambdaUpdateWrapper<RuntimeTaskAttemptEntity>()
                .eq(RuntimeTaskAttemptEntity::getId, attemptId)
                .eq(RuntimeTaskAttemptEntity::getTaskId, taskId)
                .eq(RuntimeTaskAttemptEntity::getLeaseOwner, workerId)
                .in(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.CLAIMED.name(), AttemptStatus.HEARTBEATING.name())
                .set(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.HEARTBEATING.name())
                .set(RuntimeTaskAttemptEntity::getHeartbeatAt, LocalDateTime.now())
                .set(RuntimeTaskAttemptEntity::getLeaseUntil, LocalDateTime.now().plusSeconds(properties.getLeaseSeconds())));

        return updated == 1;
    }

    /**
     * 结束当前任务尝试，记录终态状态（SUCCEEDED 或 FAILED）、错误分类及说明。
     *
     * @param attemptId 尝试记录 ID
     * @param taskId 任务 ID
     * @param workerId Worker 节点标识 ID
     * @param success 是否成功
     * @param category 错误分类（若失败）
     * @param errorSummary 错误说明摘要（若失败）
     * @return true 表示成功更新尝试终态，false 表示尝试标识不匹配
     */
    public boolean finish(
            Long attemptId,
            Long taskId,
            String workerId,
            boolean success,
            ErrorCategory category,
            String errorSummary
    ) {
        int updated = attemptMapper.update(null, new LambdaUpdateWrapper<RuntimeTaskAttemptEntity>()
                .eq(RuntimeTaskAttemptEntity::getId, attemptId)
                .eq(RuntimeTaskAttemptEntity::getTaskId, taskId)
                .eq(RuntimeTaskAttemptEntity::getLeaseOwner, workerId)
                .in(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.CLAIMED.name(), AttemptStatus.HEARTBEATING.name())
                .set(RuntimeTaskAttemptEntity::getAttemptStatus, (success ? AttemptStatus.SUCCEEDED : AttemptStatus.FAILED).name())
                .set(RuntimeTaskAttemptEntity::getErrorCategory, (category == null) ? null : category.name())
                .set(RuntimeTaskAttemptEntity::getErrorSummary, errorSummary)
                .set(RuntimeTaskAttemptEntity::getFinishedAt, LocalDateTime.now()));

        return updated == 1;
    }

    /**
     * 扫描超时失联的租约，将记录标记为 EXPIRED，并返回过期的尝试实体列表。
     *
     * @return 已被标记为 EXPIRED 的尝试实体列表 List&lt;RuntimeTaskAttemptEntity&gt;
     */
    public List<RuntimeTaskAttemptEntity> expireLeases() {
        List<RuntimeTaskAttemptEntity> expired = attemptMapper.selectList(new LambdaQueryWrapper<RuntimeTaskAttemptEntity>()
                .in(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.CLAIMED.name(), AttemptStatus.HEARTBEATING.name())
                .lt(RuntimeTaskAttemptEntity::getLeaseUntil, LocalDateTime.now()));

        for (RuntimeTaskAttemptEntity item : expired) {
            attemptMapper.update(null, new LambdaUpdateWrapper<RuntimeTaskAttemptEntity>()
                    .eq(RuntimeTaskAttemptEntity::getId, item.getId())
                    .in(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.CLAIMED.name(), AttemptStatus.HEARTBEATING.name())
                    .set(RuntimeTaskAttemptEntity::getAttemptStatus, AttemptStatus.EXPIRED.name())
                    .set(RuntimeTaskAttemptEntity::getFinishedAt, LocalDateTime.now()));
        }

        return expired;
    }
}

