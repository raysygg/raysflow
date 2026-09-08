package com.acme.agentstudio.domain.runtime.model;

import java.time.Instant;

/**
 * 高风险工具或写操作 API 执行前触发的人工确认审批单实体 Record（Tool Confirmation）。
 * 记录确认单 ID confirmationId、关联 Run ID runId、工具 ID toolId、发起人 ownerActorId、
 * 状态 status (ToolConfirmationStatus)、失效截止时间 expiresAt、审批决策人 decidedBy、决策原因说明 decisionReason 与决策时刻 decidedAt。
 *
 * @param confirmationId 确认审批单唯一 ID
 * @param runId 关联的运行任务 Run ID
 * @param toolId 触发二次确认的工具标识 ID
 * @param ownerActorId 发起或者运行任务的所属用户 ID
 * @param status 确认单处理状态（ToolConfirmationStatus）
 * @param expiresAt 确认单过期时间
 * @param decidedBy 审核审批人账号 ID
 * @param decisionReason 审批通过或拒绝的具体中文原因
 * @param decidedAt 审批决策完成时间
 */
public record ToolConfirmation(
        String confirmationId,
        String runId,
        String toolId,
        String ownerActorId,
        ToolConfirmationStatus status,
        Instant expiresAt,
        String decidedBy,
        String decisionReason,
        Instant decidedAt
) {
    /** 紧凑构造函数做断言校验与审批状态防护 */
    public ToolConfirmation {
        if (confirmationId == null || confirmationId.isBlank() || runId == null || runId.isBlank()
                || toolId == null || toolId.isBlank() || ownerActorId == null || ownerActorId.isBlank()) {
            throw new IllegalArgumentException("确认单标识、Run、工具和所有者不能为空");
        }
        if (status == null || expiresAt == null) {
            throw new IllegalArgumentException("确认状态和有效期不能为空");
        }
        decisionReason = (decisionReason == null) ? "" : decisionReason;
        if (status != ToolConfirmationStatus.PENDING && (decidedBy == null || decidedBy.isBlank())) {
            throw new IllegalArgumentException("已处理的确认单必须记录决策人");
        }
    }

    /**
     * 判断当前确认单在指定时间点是否已过期。
     *
     * @param now 当前时间戳 Instant
     * @return true 表示确认单已失效过期
     */
    public boolean isExpired(Instant now) {
        return now != null && !now.isBefore(expiresAt);
    }
}

