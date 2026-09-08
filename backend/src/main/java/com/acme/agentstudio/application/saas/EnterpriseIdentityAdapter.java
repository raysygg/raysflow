package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.IdentityProtocol;

import java.util.List;

/**
 * 企业身份认证协议适配器（Enterprise Identity Adapter） SPI 接口。
 * 负责抽象统一 SAML 2.0、OIDC (OpenID Connect)、LDAP 与 CAS 单点登录（SSO）协议的元数据校验与签名规则。
 */
public interface EnterpriseIdentityAdapter {

    /**
     * 判断当前适配器是否支持处理指定的身份认证协议。
     *
     * @param protocol 身份协议枚举
     * @return true 表示支持处理
     */
    boolean supports(IdentityProtocol protocol);

    /**
     * 对企业 SSO 身份提供方（IdP）的配置元数据进行合规校验。
     *
     * @param request 身份协议校验请求参数
     * @return 校验结果描述
     */
    IdentityValidationResult validate(IdentityValidationRequest request);

    /**
     * 身份验证请求 Record。
     *
     * @param protocol 身份协议（SAML/OIDC/CAS/LDAP）
     * @param issuer IdP 签发方 URL 地址
     * @param entityId 服务实体标识 ID（SAML 必需）
     * @param callbackUrl SSO 回调地址
     * @param organizationClaim 组织/部门 Claim 映射字段
     * @param secretRef 密钥凭证引用 Key
     */
    record IdentityValidationRequest(
            IdentityProtocol protocol,
            String issuer,
            String entityId,
            String callbackUrl,
            String organizationClaim,
            String secretRef
    ) { }

    /**
     * 身份验证结果 Record。
     *
     * @param valid 元数据与签名规则校验是否有效通过
     * @param issues 不合规的校验异常问题列表
     * @param discoveredIssuer 自动发现并解析出的 IdP 签发方端点
     */
    record IdentityValidationResult(
            boolean valid,
            List<String> issues,
            String discoveredIssuer
    ) {
        public IdentityValidationResult {
            issues = issues == null ? List.of() : List.copyOf(issues);
        }
    }
}

