package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;

/**
 * 企业身份 SSO 单点登录、组织 Claim 映射、SCIM 账号同步与 MFA 策略配置 Record（Enterprise Identity Config）。
 *
 * @param protocol 单点登录协议（IdentityProtocol：OIDC / SAML）
 * @param issuer IdP 身份提供商 Issuer URL
 * @param organizationClaim 组织机构声明属性 Key
 * @param roleMappings IdP 角色映射至平台角色的配置关系 Map
 * @param scimEnabled 是否启用 SCIM 用户与组自动同步
 * @param mfaRequired 是否强制要求 MFA 多因素认证
 * @param deprovisionOnSyncFailure SCIM 同步失败时是否停用该账号 deprovision
 */
public record EnterpriseIdentityConfig(
        IdentityProtocol protocol,
        String issuer,
        String organizationClaim,
        Map<String, String> roleMappings,
        boolean scimEnabled,
        boolean mfaRequired,
        boolean deprovisionOnSyncFailure
) {
    /** 紧凑构造函数做防空防护与输入断言 */
    public EnterpriseIdentityConfig {
        if (protocol == null || issuer == null || issuer.isBlank() || organizationClaim == null || organizationClaim.isBlank()) {
            throw new IllegalArgumentException("身份协议、签发方和组织声明不能为空");
        }
        roleMappings = (roleMappings == null) ? Map.of() : Map.copyOf(roleMappings);
    }
}

