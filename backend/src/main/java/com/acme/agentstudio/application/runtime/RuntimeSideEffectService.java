package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.SideEffectStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeSideEffectCheckpointEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeSideEffectCheckpointMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运行时外部副作用检查点与恢复决策服务（Runtime Side Effect Service）。
 * 在 Agent 流程中涉及外部不可逆副作用（如调用第三方 API 付款、写外部 DB、发送邮件等）时，
 * 记录 checkpoint 检查点状态。恢复执行前必须检查该 key 的状态：
 * 已确认 CONFIRMED 阻止重复执行，未知 UNKNOWN 或 REQUIRES_REVIEW 转人工审核，安全项允许继续推进。
 */
@Service
public class RuntimeSideEffectService {

    /** 副作用检查点 Mapper */
    private final RuntimeSideEffectCheckpointMapper checkpointMapper;

    /**
     * 构造函数注入依赖 Checkpoint Mapper。
     */
    public RuntimeSideEffectService(RuntimeSideEffectCheckpointMapper checkpointMapper) {
        this.checkpointMapper = checkpointMapper;
    }

    /**
     * 在故障恢复前，评估特定 operationKey 的外部副作用状态，做出是否允许继续执行的决策。
     *
     * @param tenantId 租户 ID
     * @param operationKey 操作唯一幂等 Key
     * @return 恢复决策结果对象 RuntimeSideEffectDecision
     */
    public RuntimeSideEffectDecision beforeRecovery(Long tenantId, String operationKey) {
        RuntimeSideEffectCheckpointEntity checkpoint = checkpointMapper.selectOne(new LambdaQueryWrapper<RuntimeSideEffectCheckpointEntity>()
                .eq(RuntimeSideEffectCheckpointEntity::getTenantId, tenantId)
                .eq(RuntimeSideEffectCheckpointEntity::getOperationKey, operationKey));

        if (checkpoint == null) {
            return new RuntimeSideEffectDecision(true, SideEffectStatus.NOT_STARTED, null);
        }

        SideEffectStatus status = SideEffectStatus.valueOf(checkpoint.getSideEffectStatus());
        return switch (status) {
            case CONFIRMED -> new RuntimeSideEffectDecision(false, status, checkpoint.getResultSummary());
            case UNKNOWN, REQUIRES_REVIEW -> new RuntimeSideEffectDecision(false, SideEffectStatus.REQUIRES_REVIEW, checkpoint.getResultSummary());
            default -> new RuntimeSideEffectDecision(true, status, checkpoint.getResultSummary());
        };
    }

    /**
     * 将特定 operationKey 的外部副作用记录标记为 UNKNOWN 未知状态（例如网络超时未得到 ACK 回执）。
     *
     * @param tenantId 租户 ID
     * @param operationKey 操作唯一幂等 Key
     * @param summary 情况说明摘要
     */
    @Transactional
    public void markUnknown(Long tenantId, String operationKey, String summary) {
        checkpointMapper.update(null, new LambdaUpdateWrapper<RuntimeSideEffectCheckpointEntity>()
                .eq(RuntimeSideEffectCheckpointEntity::getTenantId, tenantId)
                .eq(RuntimeSideEffectCheckpointEntity::getOperationKey, operationKey)
                .set(RuntimeSideEffectCheckpointEntity::getSideEffectStatus, SideEffectStatus.UNKNOWN.name())
                .set(RuntimeSideEffectCheckpointEntity::getResultSummary, summary));
    }

    /** 副作用恢复决策 Record */
    public record RuntimeSideEffectDecision(boolean allowed, SideEffectStatus status, String resultSummary) {
    }
}

