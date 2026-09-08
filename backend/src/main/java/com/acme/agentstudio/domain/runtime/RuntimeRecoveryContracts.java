package com.acme.agentstudio.domain.runtime;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent Runtime 故障恢复、任务重试、死信队列（Dead Letter）与副作用 Checkpoint 强类型契约类（Runtime Recovery Contracts）。
 * 明确任务尝试状态 AttemptStatus、死信处理状态 DeadLetterStatus、故障恢复动作 RecoveryAction 以及副作用提交状态 SideEffectStatus 等枚举与 Record。
 */
public final class RuntimeRecoveryContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private RuntimeRecoveryContracts() {
    }

    /** 任务尝试/租约状态枚举 */
    public enum AttemptStatus {
        /** 租约已领用 */
        CLAIMED,

        /** 心跳维持中 */
        HEARTBEATING,

        /** 尝试成功 */
        SUCCEEDED,

        /** 尝试失败 */
        FAILED,

        /** 租约超时过期 */
        EXPIRED,

        /** 已主动释放 */
        RELEASED
    }

    /** 死信队列处理状态枚举 */
    public enum DeadLetterStatus {
        /** 待处理 */
        OPEN,

        /** 已指派负责人 */
        ASSIGNED,

        /** 已解决 */
        RESOLVED,

        /** 已重放执行 */
        REPLAYED,

        /** 已忽略关单 */
        DISMISSED
    }

    /** 死信处置动作枚举 */
    public enum DispositionType {
        /** 指派 */
        ASSIGN,

        /** 标记解决 */
        RESOLVE,

        /** 重放 */
        REPLAY,

        /** 忽略 */
        DISMISS,

        /** 重试 */
        RETRY,

        /** 恢复 */
        RESUME,

        /** 自动故障修复 */
        RECOVER
    }

    /** 恢复控制动作枚举 */
    public enum RecoveryAction {
        /** 重试失败节点 */
        RETRY,

        /** 恢复暂停会话 */
        RESUME,

        /** 自动恢复 */
        RECOVER,

        /** 重放完整任务 */
        REPLAY
    }

    /** 恢复处理状态枚举 */
    public enum RecoveryStatus {
        /** 已申请 */
        REQUESTED,

        /** 恢复运行中 */
        RUNNING,

        /** 恢复成功 */
        SUCCEEDED,

        /** 部分恢复成功 */
        PARTIAL,

        /** 恢复失败 */
        FAILED,

        /** 拒绝恢复 */
        REJECTED
    }

    /** 外部副作用提交状态枚举 */
    public enum SideEffectStatus {
        /** 未开始 */
        NOT_STARTED,

        /** 已提交请求 */
        SUBMITTED,

        /** 外部确认识别 */
        CONFIRMED,

        /** 状态未知 */
        UNKNOWN,

        /** 需人工复核 */
        REQUIRES_REVIEW
    }

    /** Outbox 本地事务消息状态枚举 */
    public enum OutboxStatus {
        /** 待投递 */
        PENDING,

        /** 投递处理中 */
        PROCESSING,

        /** 已成功发送 */
        SENT,

        /** 投递失败 */
        FAILED
    }

    /** SLO 告警状态枚举 */
    public enum SloAlertStatus {
        /** 触发告警 */
        OPEN,

        /** 已确认 */
        ACKNOWLEDGED,

        /** 已恢复 */
        RECOVERED
    }

    /** 错误原因分类归一枚举 */
    public enum ErrorCategory {
        /** 身份鉴权错误 */
        AUTHENTICATION,

        /** 配置性错误 */
        CONFIGURATION,

        /** 瞬态网络故障 */
        TRANSIENT,

        /** 超时故障 */
        TIMEOUT,

        /** 配额超限 */
        RATE_LIMIT,

        /** 业务逻辑校验失败 */
        BUSINESS,

        /** 未知异常 */
        UNKNOWN
    }

    /** 单次任务尝试摘要 Record */
    public record TaskAttemptSummary(
            Long attemptId,
            Long taskId,
            String leaseOwner,
            AttemptStatus status,
            int attemptNo,
            LocalDateTime leaseUntil,
            ErrorCategory errorCategory,
            String errorSummary
    ) {
    }

    /** 死信队列事件摘要 Record */
    public record DeadLetterSummary(
            Long deadLetterId,
            Long tenantId,
            Long taskId,
            String runId,
            DeadLetterStatus status,
            ErrorCategory errorCategory,
            String errorSummary,
            List<TaskAttemptSummary> attempts
    ) {
        /** 构造函数防空防护 */
        public DeadLetterSummary {
            attempts = (attempts == null) ? List.of() : List.copyOf(attempts);
        }
    }

    /** 故障恢复申请 Record */
    public record RecoveryRequest(
            Long requestId,
            Long tenantId,
            String runId,
            RecoveryAction action,
            RecoveryStatus status,
            String reason
    ) {
    }

    /** 外部副作用检查点 Checkpoint Record */
    public record SideEffectCheckpoint(
            String operationKey,
            String operationType,
            SideEffectStatus status,
            String requestSummary,
            String providerReceipt,
            String resultSummary
    ) {
    }
}

