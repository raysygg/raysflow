package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.config.RuntimeRecoveryProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.RecoveryAction;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.SideEffectStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运行时故障恢复影响范围与副作用预检服务（Runtime Recovery Preview Service）。
 * 在真正执行挂起恢复或重放操作前，为管理员前端界面预览评估操作影响范围（runIds）、批量上限限制（maxBatchSize）
 * 以及外部异步副作用（如已触发外部 HTTP/数据库写操作）的风险审核状态（SideEffectStatus.REQUIRES_REVIEW），提示是否需要人工人工确认。
 */
@Service
public class RuntimeRecoveryPreviewService {

    /** 运行时恢复治理配置参数 */
    private final RuntimeRecoveryProperties properties;

    /** 运行时副作用追踪服务 */
    private final RuntimeSideEffectService sideEffectService;

    /**
     * 构造函数注入依赖配置与服务。
     */
    public RuntimeRecoveryPreviewService(
            RuntimeRecoveryProperties properties,
            RuntimeSideEffectService sideEffectService
    ) {
        this.properties = properties;
        this.sideEffectService = sideEffectService;
    }

    /**
     * 执行恢复前的风险预检，返回恢复预览报告 RecoveryPreview。
     *
     * @param user 当前登录 SecurityUser
     * @param runId 目标运行 ID
     * @param action 拟执行的恢复动作
     * @param operationKey 操作防重与副作用关联 Key（可选）
     * @param runIds 批量运行 ID 列表（可选）
     * @return 恢复预检评估结果对象 RecoveryPreview
     */
    public RecoveryPreview preview(
            SecurityUser user,
            String runId,
            RecoveryAction action,
            String operationKey,
            List<String> runIds
    ) {
        requireTenant(user);
        List<String> targets = (runIds == null || runIds.isEmpty()) ? List.of(runId) : List.copyOf(runIds);
        if (targets.size() > properties.getMaxBatchSize()) {
            throw new IllegalArgumentException("批量恢复的目标数量 (" + targets.size() + ") 超过了系统配置的最大限制 (" + properties.getMaxBatchSize() + ")。");
        }

        RuntimeSideEffectService.RuntimeSideEffectDecision sideEffect = (operationKey == null)
                ? new RuntimeSideEffectService.RuntimeSideEffectDecision(true, SideEffectStatus.NOT_STARTED, null)
                : sideEffectService.beforeRecovery(user.getTenantId(), operationKey);

        boolean requiresReview = (sideEffect.status() == SideEffectStatus.REQUIRES_REVIEW);
        return new RecoveryPreview(
                targets,
                action,
                requiresReview,
                sideEffect.status(),
                requiresReview ? "检测到外部副作用状态不确定，需要人工二次审批确认后再提交恢复" : "恢复评估通过，可以正常提交恢复请求"
        );
    }

    /** 租户身份安全校验 */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }

    /** 恢复预检结果 Record */
    public record RecoveryPreview(
            List<String> runIds,
            RecoveryAction action,
            boolean requiresReview,
            SideEffectStatus sideEffectStatus,
            String message
    ) {
        public RecoveryPreview {
            runIds = (runIds == null) ? List.of() : List.copyOf(runIds);
        }
    }
}

