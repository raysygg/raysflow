package com.acme.agentstudio.domain.saas;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * SaaS 治理、身份鉴权、计费账单、用量准入与数据保留策略强类型契约类（Saas Governance Contracts）。
 * 聚合身份协议枚举、订阅/套餐/用量状态枚举、准入决策 Record、用量事件 Record 以及数据治理进度 Record 等共享契约。
 */
public final class SaasGovernanceContracts {

    /** 私有构造函数，防止实例化 */
    private SaasGovernanceContracts() {
    }

    /** 身份认证协议枚举 */
    public enum IdentityProtocol {
        /** OIDC 认证协议 */
        OIDC,

        /** SAML 2.0 认证协议 */
        SAML
    }

    /** 身份配置状态枚举 */
    public enum IdentityConfigStatus {
        /** 草稿中 */
        DRAFT,

        /** 已验证 */
        VALIDATED,

        /** 生效激活 */
        ACTIVE,

        /** 禁用 */
        DISABLED
    }

    /** 订阅状态枚举 */
    public enum SubscriptionStatus {
        /** 试用中 */
        TRIAL,

        /** 活动正常 */
        ACTIVE,

        /** 已暂停 */
        PAUSED,

        /** 已挂起 */
        SUSPENDED,

        /** 已取消 */
        CANCELLED,

        /** 已过期 */
        EXPIRED
    }

    /** 套餐状态枚举 */
    public enum PlanStatus {
        /** 草稿 */
        DRAFT,

        /** 上架激活 */
        ACTIVE,

        /** 已退役 */
        RETIRED
    }

    /** 用量超限处理策略枚举 */
    public enum OveragePolicy {
        /** 硬限制直接阻断 */
        HARD_STOP,

        /** 仅告警提示，不阻断 */
        WARN_ONLY,

        /** 允许按量超额扣费 */
        ALLOW_OVERAGE
    }

    /** 用量准入判定决策枚举 */
    public enum AdmissionDecision {
        /** 放行允许 */
        ALLOW,

        /** 告警放行 */
        WARN,

        /** 拒绝阻断 */
        DENY
    }

    /** 准入失败处理策略枚举 */
    public enum AdmissionFailurePolicy {
        /** 降级放行 */
        FAIL_OPEN,

        /** 失败阻断 */
        FAIL_CLOSED
    }

    /** 用量数据来源枚举 */
    public enum UsageSource {
        /** 厂商官方上报 */
        PROVIDER,

        /** 算法预估估算 */
        ESTIMATED,

        /** 来源未知 */
        UNKNOWN
    }

    /** 成本计算状态枚举 */
    public enum CostStatus {
        /** 已精确计算 */
        CALCULATED,

        /** 预估计算 */
        ESTIMATED,

        /** 未知成本（缺少单价） */
        UNKNOWN,

        /** 人工/平摊调整 */
        ADJUSTED
    }

    /** 账本用量调整类型枚举 */
    public enum UsageAdjustmentType {
        /** 赠予/冲销 CREDIT */
        CREDIT,

        /** 扣减 DEBIT */
        DEBIT
    }

    /** 账单开票状态枚举 */
    public enum InvoiceStatus {
        /** 账单草稿 */
        DRAFT,

        /** 终态已确认 */
        FINALIZED,

        /** 已作废 */
        VOID
    }

    /** 数据治理请求类型枚举 */
    public enum GovernanceRequestType {
        /** 数据导出 */
        EXPORT,

        /** 数据删除 */
        DELETE,

        /** 诉讼保留 Legal Hold */
        LEGAL_HOLD,

        /** 保留策略变更 */
        RETENTION_UPDATE
    }

    /** 数据治理请求处理状态枚举 */
    public enum GovernanceRequestStatus {
        /** 已提交申请 */
        REQUESTED,

        /** 审批通过 */
        APPROVED,

        /** 执行中 */
        RUNNING,

        /** 部分失败 */
        PARTIAL_FAILED,

        /** 已完成 */
        COMPLETED,

        /** 已驳回 */
        REJECTED
    }

    /** 产品采用度事件来源枚举 */
    public enum AdoptionEventSource {
        /** 生产环境 */
        PRODUCTION,

        /** 草稿测试 */
        DRAFT_TEST,

        /** 诊断探测 */
        DIAGNOSTIC,

        /** 自动化任务 */
        AUTOMATION,

        /** 数据迁移 */
        MIGRATION
    }

    /** 产品采用度事件类型枚举 */
    public enum AdoptionEventType {
        /** 租户开通 */
        TENANT_OPENED,

        /** 创建草稿 */
        DRAFT_CREATED,

        /** 测试成功 */
        TEST_SUCCEEDED,

        /** 发布上线 */
        RELEASE_PUBLISHED,

        /** 生产运行成功 */
        PRODUCTION_SUCCEEDED,

        /** 周活跃用户 */
        WEEKLY_ACTIVE,

        /** 达到质量基线 */
        QUALITY_REACHED,

        /** 续约风险预警 */
        RENEWAL_RISK
    }

    /** 身份 SSO 配置草稿 Record */
    public record IdentityConfigDraft(
            Long tenantId,
            int version,
            IdentityProtocol protocol,
            String issuer,
            String organizationClaim,
            String callbackUrl,
            String secretRef,
            boolean scimEnabled,
            String mfaPolicyJson
    ) {
    }

    /** 权益配额项 Record */
    public record Entitlement(
            String featureCode,
            long hardLimit,
            long softLimit,
            String unit,
            OveragePolicy overagePolicy,
            Instant resetAt
    ) {
    }

    /** 用量准入申请 Record */
    public record AdmissionRequest(
            Long tenantId,
            Long applicationId,
            String featureCode,
            long estimatedQuantity,
            String operation,
            String requestId
    ) {
    }

    /** 用量准入结果 Record */
    public record AdmissionResult(
            AdmissionDecision decision,
            String featureCode,
            long currentUsage,
            long limit,
            Instant resetAt,
            OveragePolicy overagePolicy,
            String reason,
            String remediation
    ) {
    }

    /** 实时用量事件 Record */
    public record UsageEvent(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String runId,
            String modelKey,
            String featureCode,
            String costCenter,
            String idempotencyKey,
            BigDecimal quantity,
            String unit,
            UsageSource usageSource,
            Integer inputTokens,
            Integer outputTokens,
            Long priceVersionId,
            String currency,
            BigDecimal unitPrice,
            BigDecimal costAmount,
            CostStatus costStatus,
            String costReason,
            Instant occurredAt
    ) {
    }

    /** 账单明细行 Record */
    public record InvoiceLine(
            String featureCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount,
            String costCenter
    ) {
    }

    /** 数据治理清理进度 Record */
    public record GovernanceProgress(
            int total,
            int completed,
            int failed,
            List<String> evidenceCodes
    ) {
        /** 构造函数防空防护 */
        public GovernanceProgress {
            evidenceCodes = (evidenceCodes == null) ? List.of() : List.copyOf(evidenceCodes);
        }
    }

    /** 产品采用度归因事件 Record */
    public record AdoptionEvent(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String runId,
            AdoptionEventType eventType,
            int schemaVersion,
            AdoptionEventSource source,
            String idempotencyKey,
            Instant occurredAt,
            String propertiesJson
    ) {
    }

    /** 财务计费账期 Record */
    public record BillingPeriod(
            LocalDate startDate,
            LocalDate endDate,
            String currency
    ) {
    }
}

