package com.acme.agentstudio.infrastructure.persistence.mapper;

import com.acme.agentstudio.infrastructure.persistence.entity.TenantBillingQuotaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * TenantBillingQuota 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/**
 * 租户计费与资源配额 Mapper 接口
 */
@Mapper
/**
 * TenantBillingQuota 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的 TenantBillingQuota 数据库读写方法。
 */
public interface TenantBillingQuotaMapper extends BaseMapper<TenantBillingQuotaEntity> {

    /**
     * 原子扣减/消耗工作流执行配额（当用量未超限时自增 1）
     *
     * @param tenantId 租户 ID
     * @return 影响行数（1 表示扣减成功，0 表示超出每月最大额度限制）
     */
    @Update("UPDATE tenant_billing_quota SET monthly_workflow_used = monthly_workflow_used + 1, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = #{tenantId} AND monthly_workflow_used < monthly_workflow_limit")
    int consumeWorkflowQuota(@Param("tenantId") Long tenantId);

    /**
     * 原子扣减/消耗大模型 Token 计费配额
     *
     * @param tenantId 租户 ID
     * @param tokens 本次调用的 Token 消耗数量
     * @return 影响行数（1 表示扣减成功，0 表示超出每月 Token 额度上限）
     */
    @Update("UPDATE tenant_billing_quota SET monthly_token_used = monthly_token_used + #{tokens}, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = #{tenantId} AND monthly_token_used + #{tokens} <= monthly_token_limit")
    int consumeTokenQuota(@Param("tenantId") Long tenantId, @Param("tokens") Long tokens);
}
