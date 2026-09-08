package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionResult;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.OveragePolicy;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.PlanStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasAdmissionDecisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasEntitlementEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasPlanVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasSubscriptionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasUsageEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasAdmissionDecisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasEntitlementMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasPlanVersionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasSubscriptionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasUsageEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * SaaS 租户套餐版本（Plan Version）、订阅（Subscription）与功能/配额消费准入决策（Admission Decision）服务。
 * 负责定义套餐版本权益配额，处理租户订购/升级订阅，并在调用执行前进行硬限制（HARD_STOP）、超量告警（WARN）与安全否决（DENY）决策。
 */
@Service
public class TenantEntitlementService {

    /** 状态标识：活动中 */
    private static final String ACTIVE = "ACTIVE";

    /** 套餐版本 Mapper */
    private final SaasPlanVersionMapper planMapper;

    /** 租户订阅 Mapper */
    private final SaasSubscriptionMapper subscriptionMapper;

    /** 租户权益 Mapper */
    private final SaasEntitlementMapper entitlementMapper;

    /** 用量事件 Mapper */
    private final SaasUsageEventMapper usageMapper;

    /** 准入决策审计 Mapper */
    private final SaasAdmissionDecisionMapper decisionMapper;

    /** Jackson JSON 映射组件 */
    private final ObjectMapper objectMapper;

    /** 准入分批启用和异常降级配置 */
    private final SaasGovernanceProperties properties;

    /** 权益短缓存，只缓存套餐事实，不缓存实时用量 */
    private final Map<EntitlementCacheKey, CachedEntitlement> entitlementCache = new ConcurrentHashMap<>();

    /**
     * 构造函数注入套餐与准入相关依赖组件。
     */
    public TenantEntitlementService(SaasPlanVersionMapper planMapper,
                                    SaasSubscriptionMapper subscriptionMapper,
                                    SaasEntitlementMapper entitlementMapper,
                                    SaasUsageEventMapper usageMapper,
                                    SaasAdmissionDecisionMapper decisionMapper,
                                    ObjectMapper objectMapper,
                                    SaasGovernanceProperties properties) {
        this.planMapper = planMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.entitlementMapper = entitlementMapper;
        this.usageMapper = usageMapper;
        this.decisionMapper = decisionMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 创建一个全新的套餐版本（草稿状态 DRAFT，版本号自动递增）。
     *
     * @param actor 当前操作用户
     * @param command 套餐版本创建命令
     * @return 新创建的套餐版本实体
     */
    @Transactional
    public SaasPlanVersionEntity createPlan(SecurityUser actor, PlanVersionCommand command) {
        requireActor(actor);
        try {
            Integer latest = planMapper.selectList(new LambdaQueryWrapper<SaasPlanVersionEntity>()
                            .eq(SaasPlanVersionEntity::getPlanCode, command.planCode())
                            .orderByDesc(SaasPlanVersionEntity::getVersionNo))
                    .stream()
                    .findFirst()
                    .map(SaasPlanVersionEntity::getVersionNo)
                    .orElse(0);

            SaasPlanVersionEntity entity = new SaasPlanVersionEntity();
            entity.setPlanCode(command.planCode());
            entity.setVersionNo(latest + 1);
            entity.setPlanName(command.planName());
            entity.setStatus(PlanStatus.DRAFT.name());
            entity.setCurrency(command.currency());
            entity.setMonthlyBasePrice(command.monthlyBasePrice());
            entity.setFeatureJson(objectMapper.writeValueAsString(command.features()));
            entity.setEntitlementJson(objectMapper.writeValueAsString(command.entitlements()));
            entity.setCreatedAt(LocalDateTime.now());

            planMapper.insert(entity);
            return entity;
        } catch (Exception exception) {
            throw new IllegalStateException("创建套餐版本失败，解析功能或权益定义时发生异常。", exception);
        }
    }

    /**
     * 租户订阅或升级至指定的套餐版本（自动作废现有活动订阅并同步复制新套餐权益）。
     *
     * @param actor 当前操作用户
     * @param tenantId 租户 ID
     * @param planVersionId 目标套餐版本 ID
     * @param policy 超量控制策略（HARD_STOP / OVERAGE_BILLING / FLEXIBLE）
     * @param startsOn 生效开始日期
     * @return 订阅实体对象
     */
    @Transactional
    public SaasSubscriptionEntity subscribe(SecurityUser actor, Long tenantId, Long planVersionId,
                                            OveragePolicy policy, LocalDate startsOn) {
        requireActor(actor);
        SaasPlanVersionEntity plan = planMapper.selectById(planVersionId);
        if (plan == null || !PlanStatus.ACTIVE.name().equals(plan.getStatus())) {
            throw new IllegalArgumentException("指定的套餐版本不存在或尚未启用（需处于 ACTIVE 状态）。");
        }

        subscriptionMapper.update(null, new LambdaUpdateWrapper<SaasSubscriptionEntity>()
                .eq(SaasSubscriptionEntity::getTenantId, tenantId)
                .in(SaasSubscriptionEntity::getStatus, ACTIVE, "TRIAL")
                .set(SaasSubscriptionEntity::getStatus, "CANCELLED")
                .set(SaasSubscriptionEntity::getCancelledAt, LocalDateTime.now()));

        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setTenantId(tenantId);
        subscription.setPlanVersionId(planVersionId);
        subscription.setStatus(ACTIVE);
        subscription.setOveragePolicy(policy.name());
        subscription.setStartsOn(startsOn == null ? LocalDate.now() : startsOn);
        subscription.setBillingAnchor(subscription.getStartsOn());
        subscription.setCreatedBy(actor.getUserId());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(subscription.getCreatedAt());

        subscriptionMapper.insert(subscription);
        copyEntitlements(tenantId, subscription.getId(), plan.getEntitlementJson(), policy);
        entitlementCache.keySet().removeIf(key -> key.tenantId().equals(tenantId));
        return subscription;
    }

    /**
     * 在功能或用量消耗执行前进行实时准入评估（Admit Decision）。
     *
     * @param request 准入请求参数
     * @param securityAllowed 安全校验是否已通过
     * @param shadowMode 影子观测模式（若为 true，即使超过硬限制也仅返回 WARN，不阻断业务）
     * @return 准入评估结果
     */
    public AdmissionResult admit(AdmissionRequest request, boolean securityAllowed, boolean shadowMode) {
        if (request == null || request.tenantId() == null || request.featureCode() == null) {
            throw new IllegalArgumentException("提交的准入校验请求参数无效。");
        }

        if (!securityAllowed) {
            return record(request, AdmissionDecision.DENY, 0, 0, null, OveragePolicy.HARD_STOP,
                    "SECURITY_DENIED", "当前请求不满足安全策略或租户权限校验要求", false);
        }

        SaasEntitlementEntity entitlement = cachedEntitlement(request.tenantId(), request.featureCode());

        if (entitlement == null) {
            return record(request, shadowMode ? AdmissionDecision.WARN : AdmissionDecision.DENY,
                    0, 0, null, OveragePolicy.HARD_STOP, "FEATURE_NOT_INCLUDED", "当前租户订阅的套餐未包含该功能", shadowMode);
        }

        long current = currentUsage(request.tenantId(), request.featureCode(), entitlement.getResetAt());
        long projected = Math.max(0, request.estimatedQuantity()) + current;
        long limit = entitlement.getHardLimit();
        OveragePolicy policy = OveragePolicy.valueOf(entitlement.getOveragePolicy());
        AdmissionDecision decision;
        String reason;

        if (limit > 0 && projected > limit && policy == OveragePolicy.HARD_STOP) {
            decision = shadowMode ? AdmissionDecision.WARN : AdmissionDecision.DENY;
            reason = "HARD_LIMIT_EXCEEDED";
        } else if (limit > 0 && projected > limit) {
            decision = AdmissionDecision.WARN;
            reason = "OVERAGE_ALLOWED";
        } else if (entitlement.getSoftLimit() != null && entitlement.getSoftLimit() > 0 && projected >= entitlement.getSoftLimit()) {
            decision = AdmissionDecision.WARN;
            reason = "BUDGET_WARNING";
        } else {
            decision = AdmissionDecision.ALLOW;
            reason = "WITHIN_LIMIT";
        }

        return record(request, decision, current, limit, entitlement.getResetAt(), policy, reason,
                decision == AdmissionDecision.DENY ? "配额已用尽，请清理资源或升级套餐后重试" : "配额充足，允许继续操作", shadowMode);
    }

    /**
     * 按功能启用策略执行消费前准入，并明确处理准入基础设施异常。
     *
     * @param request 准入请求
     * @param securityAllowed 安全与租户权限是否通过
     * @return 强类型准入结果
     */
    public AdmissionResult admitForConsumption(AdmissionRequest request, boolean securityAllowed) {
        boolean shadowMode = request != null
                && !properties.getEnforcedAdmissionFeatures().contains(request.featureCode());
        try {
            return admit(request, securityAllowed, shadowMode);
        } catch (RuntimeException exception) {
            if (request == null || request.featureCode() == null) {
                throw exception;
            }
            boolean failOpen = securityAllowed
                    && properties.getAdmissionFailOpenFeatures().contains(request.featureCode());
            return new AdmissionResult(failOpen ? AdmissionDecision.WARN : AdmissionDecision.DENY,
                    request.featureCode(), 0, 0, null, OveragePolicy.HARD_STOP,
                    failOpen ? "准入服务暂时不可用，本次按计费类降级策略放行并告警。" : "准入服务暂时不可用，当前操作已按安全策略阻止。",
                    failOpen ? "平台将保留告警，请稍后核对用量。" : "请稍后重试或联系平台管理员检查准入服务。");
        }
    }

    /**
     * 校验管理写操作准入。该功能默认影子记录，但准入基础设施异常时安全关闭。
     *
     * @param actor 当前操作用户
     * @param operation 管理操作名称
     */
    public void requireAdministrativeWrite(SecurityUser actor, String operation) {
        requireActor(actor);
        AdmissionResult result = admitForConsumption(new AdmissionRequest(actor.getTenantId(), null,
                "ADMIN_WRITE", 1, operation, "admin-write-" + UUID.randomUUID()), true);
        if (result.decision() == AdmissionDecision.DENY) {
            throw new IllegalStateException(result.reason() + " " + result.remediation());
        }
    }

    /**
     * 查询租户当前的全部功能权益配额配置。
     *
     * @param tenantId 租户 ID
     * @return 权益实体列表
     */
    public List<SaasEntitlementEntity> entitlements(Long tenantId) {
        return entitlementMapper.selectList(new LambdaQueryWrapper<SaasEntitlementEntity>()
                .eq(SaasEntitlementEntity::getTenantId, tenantId)
                .orderByAsc(SaasEntitlementEntity::getFeatureCode));
    }

    /** 记录准入决策日志并构建 AdmissionResult 返回 */
    private AdmissionResult record(AdmissionRequest request, AdmissionDecision decision, long current, long limit,
                                  LocalDateTime resetAt, OveragePolicy policy, String reasonCode, String message, boolean shadowMode) {
        SaasAdmissionDecisionEntity fact = new SaasAdmissionDecisionEntity();
        fact.setTenantId(request.tenantId());
        fact.setApplicationId(request.applicationId());
        fact.setFeatureCode(request.featureCode());
        fact.setDecision(decision.name());
        fact.setCurrentUsage(current);
        fact.setRequestedQuantity(request.estimatedQuantity());
        fact.setUsageLimit(limit);
        fact.setOveragePolicy(policy.name());
        fact.setReasonCode(reasonCode);
        fact.setRequestId(request.requestId());
        fact.setShadowMode(shadowMode);
        fact.setResetAt(resetAt);
        fact.setCreatedAt(LocalDateTime.now());
        decisionMapper.insert(fact);

        Instant reset = resetAt == null ? null : resetAt.atZone(ZoneId.systemDefault()).toInstant();
        return new AdmissionResult(decision, request.featureCode(), current, limit, reset, policy, message,
                decision == AdmissionDecision.DENY ? "建议联系管理员提升套餐额度或清理历史资源。" : null);
    }

    /** 计算当前周期内累积用量 */
    private long currentUsage(Long tenantId, String featureCode, LocalDateTime resetAt) {
        LambdaQueryWrapper<SaasUsageEventEntity> query = new LambdaQueryWrapper<SaasUsageEventEntity>()
                .eq(SaasUsageEventEntity::getTenantId, tenantId)
                .eq(SaasUsageEventEntity::getFeatureCode, featureCode);
        if (resetAt != null) {
            query.lt(SaasUsageEventEntity::getOccurredAt, resetAt);
        }
        return usageMapper.selectList(query).stream()
                .map(SaasUsageEventEntity::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .longValue();
    }

    /** 从短缓存读取权益，过期后回源数据库并按版本替换 */
    private SaasEntitlementEntity cachedEntitlement(Long tenantId, String featureCode) {
        EntitlementCacheKey key = new EntitlementCacheKey(tenantId, featureCode);
        CachedEntitlement cached = entitlementCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.entitlement();
        }
        SaasEntitlementEntity entitlement = entitlementMapper.selectOne(new LambdaQueryWrapper<SaasEntitlementEntity>()
                .eq(SaasEntitlementEntity::getTenantId, tenantId)
                .eq(SaasEntitlementEntity::getFeatureCode, featureCode)
                .orderByDesc(SaasEntitlementEntity::getVersionNo)
                .last("LIMIT 1"));
        if (entitlement == null) {
            entitlementCache.remove(key);
            return null;
        }
        entitlementCache.put(key, new CachedEntitlement(entitlement,
                Instant.now().plusSeconds(Math.max(1, properties.getAdmissionCacheTtlSeconds()))));
        return entitlement;
    }

    /** 将套餐定义中的权益 JSON 展开复制为租户专属的实体记录 */
    private void copyEntitlements(Long tenantId, Long subscriptionId, String json, OveragePolicy fallback) {
        try {
            List<EntitlementDefinition> definitions = objectMapper.readValue(json, new TypeReference<>() {});
            for (EntitlementDefinition item : definitions) {
                SaasEntitlementEntity entity = new SaasEntitlementEntity();
                entity.setTenantId(tenantId);
                entity.setSubscriptionId(subscriptionId);
                entity.setFeatureCode(item.featureCode());
                entity.setHardLimit(item.hardLimit());
                entity.setSoftLimit(item.softLimit());
                entity.setUnit(item.unit());
                entity.setOveragePolicy((item.overagePolicy() == null ? fallback : item.overagePolicy()).name());
                entity.setVersionNo(1);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(entity.getCreatedAt());
                entitlementMapper.insert(entity);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("解析套餐权益定义 JSON 字符串失败。", exception);
        }
    }

    /** 校验 Actor 非空 */
    private void requireActor(SecurityUser actor) {
        if (actor == null || actor.getTenantId() == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份无效，请重新登录。");
        }
    }

    /** 权益定义 Record */
    public record EntitlementDefinition(String featureCode, long hardLimit, long softLimit, String unit, OveragePolicy overagePolicy) { }

    /** 套餐版本 Command Record */
    public record PlanVersionCommand(
            String planCode,
            String planName,
            String currency,
            BigDecimal monthlyBasePrice,
            List<String> features,
            List<EntitlementDefinition> entitlements
    ) { }

    /** 权益缓存键 */
    private record EntitlementCacheKey(Long tenantId, String featureCode) { }

    /** 权益缓存值 */
    private record CachedEntitlement(SaasEntitlementEntity entitlement, Instant expiresAt) { }
}

