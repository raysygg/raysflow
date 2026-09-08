package com.acme.agentstudio.application.authorization;

/**
 * RBAC / ABAC 权限判定决策（Authorization Decision）传输对象。
 * 记录针对特定操作动作（Action）在当前租户与用户维度的准入判定结果（放行或拒绝）以及人性化原因说明。
 */
public record AuthorizationDecision(boolean allowed, String action, Long tenantId, Long userId, String reason) {

    /**
     * 构建允许放行的权限判定决策结果对象。
     *
     * @param action 申请的操作动作
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param reason 放行因由说明
     * @return 许可授权决策
     */
    public static AuthorizationDecision allow(String action, Long tenantId, Long userId, String reason) {
        return new AuthorizationDecision(true, action, tenantId, userId, reason);
    }

    /**
     * 构建拒绝拦截的权限判定决策结果对象。
     *
     * @param action 申请的操作动作
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param reason 拦截原因说明
     * @return 拒绝授权决策
     */
    public static AuthorizationDecision deny(String action, Long tenantId, Long userId, String reason) {
        return new AuthorizationDecision(false, action, tenantId, userId, reason);
    }
}

