package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.RecoveryAction;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.RecoveryStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeDeadLetterEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeRecoveryRequestEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeDeadLetterMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeRecoveryRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 运行时故障恢复与历史重放命令服务（Runtime Recovery Command Service）。
 * 区分并显式处理各种恢复动作（Retry / Resume / Recover / Replay），持久化 RecoveryRequest 审计实体。
 * 在执行 Replay 历史重放命令时，使用历史快照版本创建新 Run，并同步更新关联的死信实体状态为 REPLAYED 并记录新 runId。
 */
@Service
public class RuntimeRecoveryCommandService {

    /** 恢复请求持久化 Mapper */
    private final RuntimeRecoveryRequestMapper requestMapper;

    /** 运行主入口服务 */
    private final RuntimeRunApplicationService runService;

    /** 死信 Mapper */
    private final RuntimeDeadLetterMapper deadLetterMapper;

    /**
     * 构造函数注入依赖服务与 Mappers。
     */
    public RuntimeRecoveryCommandService(
            RuntimeRecoveryRequestMapper requestMapper,
            RuntimeRunApplicationService runService,
            RuntimeDeadLetterMapper deadLetterMapper
    ) {
        this.requestMapper = requestMapper;
        this.runService = runService;
        this.deadLetterMapper = deadLetterMapper;
    }

    /**
     * 提交或创建一条恢复请求实体（RuntimeRecoveryRequestEntity）。
     *
     * @param user 当前登录 SecurityUser
     * @param runId 原运行 ID
     * @param action 恢复动作 RecoveryAction
     * @param reason 恢复原因说明
     * @return 持久化的恢复请求对象 RuntimeRecoveryRequestEntity
     */
    @Transactional
    public RuntimeRecoveryRequestEntity request(
            SecurityUser user,
            String runId,
            RecoveryAction action,
            String reason
    ) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("提交恢复请求时，当前登录用户身份信息无效。");
        }
        if (runId == null || runId.isBlank() || action == null) {
            throw new IllegalArgumentException("提交恢复请求时，目标 runId 和恢复动作 RecoveryAction 均不能为空。");
        }

        RuntimeRecoveryRequestEntity existing = requestMapper.selectOne(new LambdaQueryWrapper<RuntimeRecoveryRequestEntity>()
                .eq(RuntimeRecoveryRequestEntity::getTenantId, user.getTenantId())
                .eq(RuntimeRecoveryRequestEntity::getRunId, runId)
                .eq(RuntimeRecoveryRequestEntity::getActionType, action.name())
                .eq(RuntimeRecoveryRequestEntity::getRequestStatus, RecoveryStatus.REQUESTED.name()));

        if (existing != null) {
            return existing;
        }

        RuntimeRecoveryRequestEntity request = new RuntimeRecoveryRequestEntity();
        request.setTenantId(user.getTenantId());
        request.setRunId(runId);
        request.setActionType(action.name());
        request.setRequestStatus(RecoveryStatus.REQUESTED.name());
        request.setReason(reason);
        request.setRequestedBy(user.getUserId());
        request.setCreatedAt(LocalDateTime.now());

        requestMapper.insert(request);
        return request;
    }

    /**
     * 执行 Replay 历史恢复重放命令：使用原版本快照启动新 Run，并将死信队列关联实体更新为 REPLAYED。
     *
     * @param user 当前登录 SecurityUser
     * @param runId 源运行 ID
     * @param deadLetterId 关联的死信实体 ID（可选）
     * @param idempotencyKey 幂等键
     * @return 历史重放结果对象 ReplayResult
     */
    @Transactional
    public ReplayResult replay(SecurityUser user, String runId, Long deadLetterId, String idempotencyKey) {
        RuntimeRecoveryRequestEntity request = request(user, runId, RecoveryAction.REPLAY, "人工确认触发历史版本重放");
        Map<String, Object> result = runService.replayHistorical(user, runId, idempotencyKey);

        request.setRequestStatus(RecoveryStatus.SUCCEEDED.name());
        request.setCompletedAt(LocalDateTime.now());
        requestMapper.updateById(request);

        String newRunId = String.valueOf(result.get("executionId"));
        if (deadLetterId != null) {
            deadLetterMapper.update(null, new LambdaUpdateWrapper<RuntimeDeadLetterEntity>()
                    .eq(RuntimeDeadLetterEntity::getTenantId, user.getTenantId())
                    .eq(RuntimeDeadLetterEntity::getId, deadLetterId)
                    .set(RuntimeDeadLetterEntity::getDeadLetterStatus, "REPLAYED")
                    .set(RuntimeDeadLetterEntity::getReplayRunId, newRunId));
        }

        return new ReplayResult(request.getId(), runId, newRunId, deadLetterId);
    }

    /** 重放结果 Record */
    public record ReplayResult(Long requestId, String sourceRunId, String newRunId, Long deadLetterId) {
    }
}

