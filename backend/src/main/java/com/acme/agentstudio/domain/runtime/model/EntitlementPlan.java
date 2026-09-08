package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;
import java.util.Set;

/**
 * 租户商业套餐权益与资源使用配额定义 Record（Entitlement Plan）。
 * 包含套餐编码 planCode、部署版本 edition、包含特性集合 features、用量限额映射 quotas、是否已冻结挂起 suspended 以及续费状态 renewalState。
 *
 * @param planCode 套餐编码 slug
 * @param edition 部署版本（DeploymentEdition）
 * @param features 包含的开通功能特性集合
 * @param quotas 限制的用量配额 Map（如最大并发数、最大月度 Token 数）
 * @param suspended 是否由于欠费或违规暂停挂起
 * @param renewalState 订阅续费状态说明
 */
public record EntitlementPlan(
        String planCode,
        DeploymentEdition edition,
        Set<String> features,
        Map<String, Long> quotas,
        boolean suspended,
        String renewalState
) {
    /** 紧凑构造函数做输入属性断言校验 */
    public EntitlementPlan {
        if (planCode == null || planCode.isBlank() || edition == null || renewalState == null || renewalState.isBlank()) {
            throw new IllegalArgumentException("套餐标识、部署版本和续费状态不能为空");
        }
        features = (features == null) ? Set.of() : Set.copyOf(features);
        quotas = (quotas == null) ? Map.of() : Map.copyOf(quotas);
    }
}

