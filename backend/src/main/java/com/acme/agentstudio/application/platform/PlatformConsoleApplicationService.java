package com.acme.agentstudio.application.platform;

import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantBillingQuotaEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolConnectorMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantBillingQuotaMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台运营总控制台（Platform Super Console）大盘应用服务。
 * 针对 SUPER_ADMIN 平台超级管理员提供跨租户健康度审查、全平台额度配额与用量汇总、连接器资源不可用风险预警以及全局服务就绪度评估。
 */
@Service
public class PlatformConsoleApplicationService {

    /** 租户 Persistence Mapper */
    private final TenantMapper tenantMapper;

    /** 租户计费配额 Persistence Mapper */
    private final TenantBillingQuotaMapper quotaMapper;

    /** 平台执行上下文 Mapper */
    private final PlatformExecutionContextMapper executionMapper;

    /** 平台工具连接器 Mapper */
    private final PlatformToolConnectorMapper connectorMapper;

    /**
     * 构造函数注入平台控制台所需 Mapper 依赖。
     */
    public PlatformConsoleApplicationService(TenantMapper tenantMapper,
                                             TenantBillingQuotaMapper quotaMapper,
                                             PlatformExecutionContextMapper executionMapper,
                                             PlatformToolConnectorMapper connectorMapper) {
        this.tenantMapper = tenantMapper;
        this.quotaMapper = quotaMapper;
        this.executionMapper = executionMapper;
        this.connectorMapper = connectorMapper;
    }

    /**
     * 查询全平台跨租户运营健康度、额度配额与服务可用性大盘概览。
     *
     * @param user 当前登录用户（需具备 SUPER_ADMIN 超级管理员权限）
     * @return 包含全平台租户健康度、全局资源分布、配额消耗以及风险列表的大盘字典
     */
    public Map<String, Object> overview(SecurityUser user) {
        if (user == null || !user.hasRole("SUPER_ADMIN")) {
            throw new AuthorizationDeniedException("仅平台超级管理员（SUPER_ADMIN）有权限查看跨租户运营控制台。");
        }
        List<TenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<>());
        List<TenantBillingQuotaEntity> quotas = quotaMapper.selectList(null);
        List<PlatformExecutionContextEntity> executions = executionMapper.selectList(null);
        List<PlatformToolConnectorEntity> connectors = connectorMapper.selectList(null);

        Map<Long, TenantBillingQuotaEntity> quotaByTenant = quotas.stream()
                .collect(Collectors.toMap(TenantBillingQuotaEntity::getTenantId, item -> item, (left, right) -> left));

        Map<Long, Long> failuresByTenant = executions.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .collect(Collectors.groupingBy(PlatformExecutionContextEntity::getTenantId, Collectors.counting()));

        List<Map<String, Object>> tenantHealth = tenants.stream()
                .map(tenant -> tenantHealth(tenant, quotaByTenant.get(tenant.getId()), failuresByTenant.getOrDefault(tenant.getId(), 0L)))
                .toList();

        long unavailableConnectors = connectors.stream()
                .filter(item -> !"ACTIVE".equalsIgnoreCase(item.getStatus()))
                .count();

        long activeExecutions = executions.stream()
                .filter(item -> List.of("QUEUED", "RUNNING", "WAITING_APPROVAL").contains(item.getStatus()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consoleType", "PLATFORM");
        result.put("tenantHealth", tenantHealth);
        result.put("platformResources", Map.of(
                "tenantCount", tenants.size(),
                "connectorCount", connectors.size(),
                "activeExecutions", activeExecutions
        ));
        result.put("quota", Map.of(
                "tenantsWithQuota", quotas.size(),
                "totalTokenLimit", quotas.stream().mapToLong(item -> value(item.getMonthlyTokenLimit())).sum(),
                "totalTokenUsed", quotas.stream().mapToLong(item -> value(item.getMonthlyTokenUsed())).sum()
        ));
        result.put("serviceAvailability", Map.of(
                "executionRuntime", "AVAILABLE",
                "connectorUnavailable", unavailableConnectors,
                "status", unavailableConnectors == 0 ? "AVAILABLE" : "DEGRADED"
        ));
        result.put("crossTenantRisks", tenantHealth.stream()
                .filter(item -> !"HEALTHY".equals(item.get("healthStatus")))
                .toList());

        return result;
    }

    /**
     * 计算单租户的健康状态（如正常 HEALTHY / 冻结 FROZEN / 存在风险 AT_RISK）。
     */
    private Map<String, Object> tenantHealth(TenantEntity tenant, TenantBillingQuotaEntity quota, long failures) {
        boolean frozen = "FROZEN".equalsIgnoreCase(tenant.getStatus());
        boolean quotaNearLimit = quota != null && quota.getMonthlyTokenLimit() != null && quota.getMonthlyTokenLimit() > 0
                && value(quota.getMonthlyTokenUsed()) * 100 >= quota.getMonthlyTokenLimit() * 90;
        String status = frozen ? "FROZEN" : (quota == null || quotaNearLimit || failures > 0 ? "AT_RISK" : "HEALTHY");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenant.getId());
        result.put("tenantName", tenant.getTenantName());
        result.put("status", tenant.getStatus());
        result.put("healthStatus", status);
        result.put("failedExecutions", failures);
        result.put("quotaConfigured", quota != null);
        result.put("quotaNearLimit", quotaNearLimit);
        return result;
    }

    /**
     * Long 值安全转换 helper 方法。
     */
    private long value(Long number) {
        return number == null ? 0L : number;
    }
}

