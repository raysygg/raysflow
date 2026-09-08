package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ToolConfirmation;
import com.acme.agentstudio.domain.runtime.model.ToolConfirmationStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时高风险工具人工审批与确认状态机服务（Tool Confirmation Service）。
 * 当 Agent 试图触发具有高风险或包含副作用的 Tool 工具时，发起 PENDING 状态的人工确认单 request()，
 * 并限制特定审批人 ownerActorId 在有效期 expiresAt 内做出同意（APPROVED）或拒绝（DENIED）的决策（decide()）；
 * 工具执行前由 requireApproved() 强制校验人工确认状态，过期（EXPIRED）或未同意直接阻断执行。
 */
@Service
public class ToolConfirmationService {

    /** 系统自动触发操作的操作人标识 */
    private static final String SYSTEM_ACTOR = "system";

    /** 内存中的工具确认单映射 Map（Key 为 confirmationId） */
    private final Map<String, ToolConfirmation> confirmations = new ConcurrentHashMap<>();

    /**
     * 发起一条新的工具人工确认单，状态为 PENDING。
     *
     * @param runId 运行 ID
     * @param toolId 拟调用的工具 ID
     * @param ownerActorId 指定的审批责任人账号 ID
     * @param expiresAt 确认单过期绝对时间点
     * @return 构建的工具确认单实体 ToolConfirmation
     */
    public ToolConfirmation request(String runId, String toolId, String ownerActorId, Instant expiresAt) {
        ToolConfirmation confirmation = new ToolConfirmation(
                UUID.randomUUID().toString(),
                runId,
                toolId,
                ownerActorId,
                ToolConfirmationStatus.PENDING,
                expiresAt,
                null,
                "",
                null
        );
        confirmations.put(confirmation.confirmationId(), confirmation);
        return confirmation;
    }

    /**
     * 审批责任人提交决策（同意或拒绝）。
     *
     * @param confirmationId 确认单 ID
     * @param decisionActorId 实际提交决策的账号 ID
     * @param approved true 表示同意，false 表示拒绝
     * @param reason 决策原因说明
     * @return 更新后的 ToolConfirmation 确认单实体
     */
    public ToolConfirmation decide(String confirmationId, String decisionActorId, boolean approved, String reason) {
        ToolConfirmation current = require(confirmationId);
        Instant now = Instant.now();

        if (current.status() != ToolConfirmationStatus.PENDING) {
            throw new IllegalStateException("工具确认单当前状态 [" + current.status() + "] 不处于 PENDING 审批状态，无法重复决策。");
        }

        if (current.isExpired(now)) {
            String actor = (decisionActorId == null || decisionActorId.isBlank()) ? SYSTEM_ACTOR : decisionActorId;
            ToolConfirmation expired = replace(current, ToolConfirmationStatus.EXPIRED, actor, "工具确认单已超时自动失效", now);
            confirmations.put(confirmationId, expired);
            throw new IllegalStateException("工具确认单已超时失效，请重新发起审批。");
        }

        if (decisionActorId == null || decisionActorId.isBlank()) {
            throw new IllegalArgumentException("提交审批决策时，决策人 ID 不能为空。");
        }
        if (!current.ownerActorId().equals(decisionActorId)) {
            throw new IllegalArgumentException("当前决策人 [" + decisionActorId + "] 不是指定的确认单审批责任人 [" + current.ownerActorId() + "]。");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("提交审批决策时，必须提供中文决策原因说明。");
        }

        ToolConfirmationStatus nextStatus = approved ? ToolConfirmationStatus.APPROVED : ToolConfirmationStatus.DENIED;
        ToolConfirmation decided = replace(current, nextStatus, decisionActorId, reason, now);
        confirmations.put(confirmationId, decided);
        return decided;
    }

    /**
     * 强校验工具确认单是否处于已批准状态 APPROVED。
     *
     * @param confirmationId 确认单 ID
     * @param now 当前时间戳
     * @return 已核准的 ToolConfirmation 实例
     */
    public ToolConfirmation requireApproved(String confirmationId, Instant now) {
        ToolConfirmation confirmation = require(confirmationId);

        if (confirmation.isExpired(now)) {
            confirmations.put(confirmationId, replace(confirmation, ToolConfirmationStatus.EXPIRED, SYSTEM_ACTOR, "工具确认单已超时自动失效", now));
            throw new IllegalStateException("工具确认单已超时失效，无法继续执行工具。");
        }

        if (confirmation.status() != ToolConfirmationStatus.APPROVED) {
            throw new IllegalStateException("目标工具尚未获得人工确认批准，当前状态为：" + confirmation.status());
        }

        return confirmation;
    }

    /** 安全获取确认单，不存在抛出 IllegalArgumentException */
    private ToolConfirmation require(String confirmationId) {
        ToolConfirmation confirmation = confirmations.get(confirmationId);
        if (confirmation == null) {
            throw new IllegalArgumentException("未找到 ID 为 [" + confirmationId + "] 的工具确认单。");
        }
        return confirmation;
    }

    /** 替换创建新的 ToolConfirmation 快照 */
    private ToolConfirmation replace(
            ToolConfirmation current,
            ToolConfirmationStatus status,
            String decidedBy,
            String reason,
            Instant decidedAt
    ) {
        return new ToolConfirmation(
                current.confirmationId(),
                current.runId(),
                current.toolId(),
                current.ownerActorId(),
                status,
                current.expiresAt(),
                decidedBy,
                reason,
                decidedAt
        );
    }
}

