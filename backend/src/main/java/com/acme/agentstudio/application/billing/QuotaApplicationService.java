package com.acme.agentstudio.application.billing;

import com.acme.agentstudio.infrastructure.persistence.entity.TenantBillingQuotaEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantBillingQuotaMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 租户计量配额应用服务。
 * 在工作流或大模型 Agent 运行时原子扣减租户配额（Workflow 次数与 Token 数量），防止高并发请求击穿租户月度用量上限。
 */
@Service
public class QuotaApplicationService {

    /** 租户计费配额 Mapper */
    private final TenantBillingQuotaMapper quotaMapper;

    /**
     * 构造函数注入配额 Persistence 层组件。
     */
    public QuotaApplicationService(TenantBillingQuotaMapper quotaMapper) {
        this.quotaMapper = quotaMapper;
    }

    /**
     * 原子扣减租户工作流执行配额（若额度用尽则抛出异常以阻断执行）。
     *
     * @param tenantId 租户 ID
     */
    public void consumeWorkflow(Long tenantId) {
        TenantBillingQuotaEntity quota = quotaMapper.selectOne(new QueryWrapper<TenantBillingQuotaEntity>().eq("tenant_id", tenantId));
        if (quota == null) {
            throw new IllegalArgumentException("当前租户未配置工作流配额。");
        }
        if (quotaMapper.consumeWorkflowQuota(tenantId) == 0) {
            throw new IllegalArgumentException("当前租户本月工作流调用次数已用尽。");
        }
    }

    /**
     * 原子扣减租户的大模型 Token 计费配额。
     *
     * @param tenantId 租户 ID
     * @param tokens 本次请求消耗的 Token 数量
     */
    public void consumeTokens(Long tenantId, long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("Token 消耗数量不能为负数。");
        }
        if (tokens == 0) {
            return;
        }
        if (quotaMapper.consumeTokenQuota(tenantId, tokens) == 0) {
            throw new IllegalArgumentException("当前租户本月 Token 计费配额已用尽。");
        }
    }
}

