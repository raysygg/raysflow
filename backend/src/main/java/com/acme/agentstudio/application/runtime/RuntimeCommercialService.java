package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.EntitlementPlan;
import com.acme.agentstudio.domain.runtime.model.RuntimeAdmissionDecision;
import com.acme.agentstudio.domain.runtime.model.UsageMeterRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时 SaaS 商业套餐准入校验与用量计量服务（Runtime Commercial Service）。
 * 负责维护租户的权益套餐（EntitlementPlan），在 Runtime Run 启动前校验能力特权（Feature Access）与预估 Token 配额（Quota Check）；
 * 在 Run 执行完成后，对消耗的大模型 Token 数、工具调用数及链路成本进行幂等记账与计量汇总（Usage Metering）。
 */
@Service
public class RuntimeCommercialService {

    /** Token 计费配额键名 */
    private static final String QUOTA_TOKENS = "tokens";

    /** 内存中租户权益套餐映射 Map */
    private final Map<Long, EntitlementPlan> plans = new ConcurrentHashMap<>();

    /** 内存中用量计量记录映射 Map（按 idempotencyKey 幂等键去重） */
    private final Map<String, UsageMeterRecord> meters = new ConcurrentHashMap<>();

    /**
     * 为指定租户绑定或更新 SaaS 权益套餐（EntitlementPlan）。
     *
     * @param tenantId 租户 ID
     * @param plan 套餐定义实体 EntitlementPlan
     */
    public void bindPlan(long tenantId, EntitlementPlan plan) {
        if (tenantId <= 0 || plan == null) {
            throw new IllegalArgumentException("绑定套餐时，租户 ID 必须大于零且套餐定义 EntitlementPlan 不能为空。");
        }
        plans.put(tenantId, plan);
    }

    /**
     * 针对特定的功能特性（feature）和预估消耗 Token 数，执行 Runtime 准入鉴权与配额检查。
     *
     * @param tenantId 租户 ID
     * @param feature 请求的功能特性标识（如 "MULTI_AGENT", "RAG_HYBRID"）
     * @param estimatedTokens 本次请求预估消耗的 Token 总量
     * @return 准入决策实体 RuntimeAdmissionDecision
     */
    public RuntimeAdmissionDecision admit(long tenantId, String feature, long estimatedTokens) {
        EntitlementPlan plan = plans.get(tenantId);
        List<String> reasons = new ArrayList<>();

        if (plan == null) {
            reasons.add("当前租户尚未绑定任何有效的 SaaS 商业套餐。");
        } else {
            if (plan.suspended()) {
                reasons.add("当前租户的商业套餐已被暂停或已过有效期。");
            }
            if (feature == null || !plan.features().contains(feature)) {
                reasons.add("当前租户绑定的商业套餐不包含该项高阶功能特性：" + feature);
            }
            Long tokenQuota = plan.quotas().get(QUOTA_TOKENS);
            if (tokenQuota != null && estimatedTokens > tokenQuota) {
                reasons.add("本次请求预计消耗的 Token 数 (" + estimatedTokens + ") 已超过套餐配额上限 (" + tokenQuota + ")。");
            }
        }
        return new RuntimeAdmissionDecision(reasons.isEmpty(), reasons);
    }

    /**
     * 记录一条 Runtime 用量明细（支持 idempotencyKey 幂等写入）。
     *
     * @param record 用量记录对象 UsageMeterRecord
     * @return 实际存储的用量记录对象 UsageMeterRecord
     */
    public UsageMeterRecord meter(UsageMeterRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("用量计量记录 UsageMeterRecord 不能为空。");
        }
        return meters.computeIfAbsent(record.idempotencyKey(), ignored -> record);
    }

    /**
     * 查询指定租户的所有用量计量记录。
     *
     * @param tenantId 租户 ID
     * @return 用量计量记录列表 List&lt;UsageMeterRecord&gt;
     */
    public List<UsageMeterRecord> usage(long tenantId) {
        return meters.values().stream()
                .filter(record -> record.tenantId() == tenantId)
                .toList();
    }
}

