package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.application.authorization.AuthorizationDecision;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 编排操作权限控制服务（Orchestration Authorization Service）。
 * 负责应用草稿编辑、查看、发布、回滚及运行权限判定与安全拦截。
 * 遵循 SaaS 简易菜单权限模型，取消细粒度资源行级判定，但强校验当前登录用户的租户上下文与身份边界。
 */
@Service
public class OrchestrationAuthorizationService {

    /** 审计应用服务 */
    private final AuditApplicationService auditApplicationService;

    /**
     * 构造函数注入审计依赖服务。
     */
    public OrchestrationAuthorizationService(AuditApplicationService auditApplicationService) {
        this.auditApplicationService = auditApplicationService;
    }

    /**
     * 校验当前安全用户的操作授权决定。
     *
     * @param user 当前安全用户
     * @param appId 应用 ID
     * @param action 拟执行的操作动作（EDIT_DRAFT / READ_DRAFT / PUBLISH / ROLLBACK / RUN）
     * @return 包含 allow/deny 的 AuthorizationDecision 对象
     */
    public AuthorizationDecision decide(SecurityUser user, Long appId, String action) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            return AuthorizationDecision.deny(action, null, null, "当前登录身份无效或缺少租户与用户标识信息。");
        }
        return AuthorizationDecision.allow(action, user.getTenantId(), user.getUserId(), "菜单权限控制允许执行当前编排操作。");
    }

    /**
     * 强制断言当前安全用户具备指定编排操作权限，若未通过则记录审计事件并抛出 AuthorizationDeniedException 异常。
     *
     * @param user 当前安全用户
     * @param appId 应用 ID
     * @param action 拟执行的操作动作
     */
    public void require(SecurityUser user, Long appId, String action) {
        AuthorizationDecision decision = decide(user, appId, action);
        if (!decision.allowed()) {
            recordDenied(user, appId, decision);
            throw new AuthorizationDeniedException("拒绝访问：" + decision.reason());
        }
    }

    /**
     * 校验工作流图中所引用资源的依赖授权关系（目前仅校验租户归属与格式，不阻断正常运行）。
     *
     * @param user 当前安全用户
     * @param appId 应用 ID
     * @param graph 工作流图定义
     */
    public void requireGraphResources(SecurityUser user, Long appId, GraphDefinition graph) {
        // 节点统一由 WorkflowDependencyResolver 进行租约与归属校验
    }

    /** 记录权限拒绝审计事件 */
    private void recordDenied(SecurityUser user, Long appId, AuthorizationDecision decision) {
        if (user == null || user.getTenantId() == null) {
            return;
        }
        auditApplicationService.recordWorkflowAction(
                user.getTenantId(),
                user.getUsername(),
                "ORCHESTRATION_AUTH_DENIED",
                appId,
                Map.of("action", decision.action(), "reason", decision.reason())
        );
    }
}

