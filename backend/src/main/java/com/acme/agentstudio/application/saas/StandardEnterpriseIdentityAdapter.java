package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.IdentityProtocol;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;

/**
 * 标准企业身份协议（OIDC / SAML）基础元数据校验适配器。
 * 检查签发方端点与回调地址的 HTTPS 传输加密协议、SAML 服务实体标识及组织属性映射字段的完整性，不直接解密或持久化密钥明文。
 */
@Component
public class StandardEnterpriseIdentityAdapter implements EnterpriseIdentityAdapter {

    /**
     * 判断是否支持指定的身份协议。
     *
     * @param protocol 协议枚举
     * @return 协议非空时返回 true
     */
    @Override
    public boolean supports(IdentityProtocol protocol) {
        return protocol != null;
    }

    /**
     * 校验 SSO 请求配置参数中的传输协议与必需项。
     *
     * @param request 校验请求参数
     * @return 包含错误清单与发现端点的校验结果
     */
    @Override
    public IdentityValidationResult validate(IdentityValidationRequest request) {
        ArrayList<String> issues = new ArrayList<>();
        requireHttps(request.issuer(), "签发方地址", issues);
        requireHttps(request.callbackUrl(), "回调地址", issues);

        if (request.organizationClaim() == null || request.organizationClaim().isBlank()) {
            issues.add("缺少组织与部门属性映射字段。");
        }
        if (request.secretRef() == null || request.secretRef().isBlank()) {
            issues.add("缺少身份密钥引用配置。");
        }
        if (request.protocol() == IdentityProtocol.SAML && (request.entityId() == null || request.entityId().isBlank())) {
            issues.add("SAML 协议配置中缺少服务实体标识（Entity ID）。");
        }

        return new IdentityValidationResult(issues.isEmpty(), issues, request.issuer());
    }

    /**
     * 校验配置 URL 必须使用安全的 HTTPS 协议。
     */
    private void requireHttps(String value, String label, ArrayList<String> issues) {
        try {
            if (!"https".equalsIgnoreCase(URI.create(value).getScheme())) {
                issues.add(label + "必须使用安全的 HTTPS 协议。");
            }
        } catch (RuntimeException exception) {
            issues.add(label + "格式无效，无法解析 URL 契约。");
        }
    }
}

