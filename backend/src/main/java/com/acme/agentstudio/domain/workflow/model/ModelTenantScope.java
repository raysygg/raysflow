package com.acme.agentstudio.domain.workflow.model;

/**
 * 平台与租户模型共享隔离作用域常量类（Model Tenant Scope）。
 * 明确区分全平台全局租户 ID（PLATFORM_TENANT_ID = 1L）与常规租户私有模型资源配置。
 */
public final class ModelTenantScope {

    /** 全平台共享全局模型租户标识 ID */
    public static final long PLATFORM_TENANT_ID = 1L;

    /** 私有构造函数，防止工具类被实例化 */
    private ModelTenantScope() {
    }
}

