package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ComplianceEvidence;
import com.acme.agentstudio.domain.runtime.model.DataVisibilityPolicy;
import com.acme.agentstudio.domain.runtime.model.EnterpriseIdentityConfig;
import com.acme.agentstudio.domain.runtime.model.SecretRotationPolicy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业身份与数据治理合规证据应用服务（Compliance Evidence Service）。
 * 记录与审计 Enterprise Identity（SSO/OIDC）、Secret 轮换策略（Secret Rotation Policy）及数据区域隔离（Data Visibility）等合规事实存证。
 */
@Service
public class ComplianceEvidenceService {

    /** 内存级合规存证记录 Map */
    private final Map<String, ComplianceEvidence> evidences = new ConcurrentHashMap<>();

    /**
     * 记录一条通用合规审计存证记录。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者账号 ID
     * @param type 存证类型
     * @param resourceId 目标资源标识
     * @param details 详细参数 Map
     * @return 生成的合规存证记录 ComplianceEvidence
     */
    public ComplianceEvidence record(
            long tenantId,
            String actorId,
            String type,
            String resourceId,
            Map<String, Object> details
    ) {
        ComplianceEvidence evidence = new ComplianceEvidence(
                UUID.randomUUID().toString(),
                tenantId,
                type,
                actorId,
                resourceId,
                details,
                null
        );
        evidences.put(evidence.evidenceId(), evidence);
        return evidence;
    }

    /**
     * 记录企业身份配置（SSO/SCIM/OIDC）变更存证。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者账号 ID
     * @param config 企业身份配置对象 EnterpriseIdentityConfig
     * @return 合规存证记录 ComplianceEvidence
     */
    public ComplianceEvidence recordIdentityConfig(long tenantId, String actorId, EnterpriseIdentityConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("企业身份配置 EnterpriseIdentityConfig 不能为空。");
        }
        return record(
                tenantId,
                actorId,
                "IDENTITY_CONFIGURED",
                config.issuer(),
                Map.of("protocol", config.protocol().name(), "scim", config.scimEnabled())
        );
    }

    /**
     * 记录密钥自动轮换策略配置变更存证。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者账号 ID
     * @param policy 密钥轮换策略对象 SecretRotationPolicy
     * @return 合规存证记录 ComplianceEvidence
     */
    public ComplianceEvidence recordRotationPolicy(long tenantId, String actorId, SecretRotationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("密钥自动轮换策略 SecretRotationPolicy 不能为空。");
        }
        return record(
                tenantId,
                actorId,
                "SECRET_ROTATION_CONFIGURED",
                null,
                Map.of("rotationDays", policy.rotationDays())
        );
    }

    /**
     * 记录数据区域与可见性安全策略变更存证。
     *
     * @param tenantId 租户 ID
     * @param actorId 操作者账号 ID
     * @param policy 数据可见性策略对象 DataVisibilityPolicy
     * @return 合规存证记录 ComplianceEvidence
     */
    public ComplianceEvidence recordVisibilityPolicy(long tenantId, String actorId, DataVisibilityPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("数据可见性与区域策略 DataVisibilityPolicy 不能为空。");
        }
        return record(
                tenantId,
                actorId,
                "DATA_VISIBILITY_CONFIGURED",
                policy.dataRegion(),
                Map.of("encryption", policy.encryptionEnabled())
        );
    }

    /**
     * 查询指定租户的合规存证记录列表。
     *
     * @param tenantId 租户 ID
     * @return 合规存证记录列表 List&lt;ComplianceEvidence&gt;
     */
    public List<ComplianceEvidence> list(long tenantId) {
        return evidences.values().stream()
                .filter(item -> item.tenantId() == tenantId)
                .toList();
    }
}

