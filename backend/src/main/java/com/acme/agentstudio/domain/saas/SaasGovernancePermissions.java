package com.acme.agentstudio.domain.saas;

/**
 * SaaS 治理、身份鉴权、财务订阅与数据保留策略权限码常量定义类（Saas Governance Permissions）。
 * 集中管理各模块角色细粒度操作权限字符串编码。
 */
public final class SaasGovernancePermissions {

    /** 身份体系与 MFA/SSO 管理权限 */
    public static final String IDENTITY_MANAGE = "SAAS_IDENTITY_MANAGE";

    /** 套餐订阅与权益扩容管理权限 */
    public static final String SUBSCRIPTION_MANAGE = "SAAS_SUBSCRIPTION_MANAGE";

    /** 账单与用量账本查看权限 */
    public static final String BILLING_VIEW = "SAAS_BILLING_VIEW";

    /** 账单与财务凭证导出权限 */
    public static final String BILLING_EXPORT = "SAAS_BILLING_EXPORT";

    /** 数据删除与保留策略变更申请权限 */
    public static final String GOVERNANCE_REQUEST = "SAAS_GOVERNANCE_REQUEST";

    /** 数据删除与高风险治理审批权限 */
    public static final String GOVERNANCE_APPROVE = "SAAS_GOVERNANCE_APPROVE";

    /** 产品采用度与漏斗分析查看权限 */
    public static final String ADOPTION_VIEW = "SAAS_ADOPTION_VIEW";

    /** 跨租户跨域运维总台摘要查看权限（超级管理员专用） */
    public static final String CROSS_TENANT_SUMMARY = "SAAS_CROSS_TENANT_SUMMARY";

    /** 私有构造函数，防止工具类被实例化 */
    private SaasGovernancePermissions() {
    }
}

