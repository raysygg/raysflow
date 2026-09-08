package com.acme.agentstudio.application.project;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.ApprovalTaskEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApprovalTaskMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营人员协同工作台（Workbench Console）数据聚合应用服务。
 * 集中聚合渲染用户与运营人员关注的已发布应用列表、活动中的工作流执行、待办审批任务（Pending Approvals）、近期失败运行日志及应用健康度卡片。
 */
@Service
public class WorkbenchApplicationService {

    /** 工作流应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 平台执行上下文 Mapper */
    private final PlatformExecutionContextMapper executionMapper;

    /** 审批任务 Mapper */
    private final ApprovalTaskMapper approvalMapper;

    /**
     * 构造函数注入工作台聚合所需 Mapper 组件。
     */
    public WorkbenchApplicationService(OrchestrationAppMapper appMapper,
                                       PlatformExecutionContextMapper executionMapper,
                                       ApprovalTaskMapper approvalMapper) {
        this.appMapper = appMapper;
        this.executionMapper = executionMapper;
        this.approvalMapper = approvalMapper;
    }

    /**
     * 聚合查询工作台首页所需的大盘面板数据（已发布应用、活动执行、待审批任务、失败异常）。
     *
     * @param user 当前登录用户
     * @return 工作台各核心模块统计与数据列表映射 Map
     */
    public Map<String, Object> aggregate(SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("当前登录身份无效，无法加载工作台。");
        }
        Long tenantId = user.getTenantId();
        boolean platformScope = user.hasRole("SUPER_ADMIN");
        boolean canApprove = platformScope || user.hasRole("ADMIN") || user.hasRole("APPROVER") || user.hasRole("OPERATOR");

        List<OrchestrationAppEntity> apps = appMapper.selectList(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(!platformScope, OrchestrationAppEntity::getTenantId, tenantId)
                .orderByDesc(OrchestrationAppEntity::getUpdatedAt));

        List<PlatformExecutionContextEntity> executions = executionMapper.selectList(new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                .eq(!platformScope, PlatformExecutionContextEntity::getTenantId, tenantId)
                .orderByDesc(PlatformExecutionContextEntity::getStartedAt)
                .last("LIMIT 100"));

        List<ApprovalTaskEntity> approvals = canApprove
                ? approvalMapper.selectList(new LambdaQueryWrapper<ApprovalTaskEntity>()
                        .eq(!platformScope, ApprovalTaskEntity::getTenantId, tenantId)
                        .eq(ApprovalTaskEntity::getApprovalStatus, "PENDING")
                        .orderByDesc(ApprovalTaskEntity::getCreatedAt)
                        .last("LIMIT 20"))
                : List.of();

        List<Map<String, Object>> recentFailures = executions.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .limit(20)
                .map(this::executionSummary)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("publishedApps", apps.stream()
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()) || item.getCurrentRevisionId() != null)
                .limit(10)
                .toList());

        result.put("activeExecutions", executions.stream()
                .filter(item -> List.of("QUEUED", "RUNNING", "PAUSING", "WAITING_APPROVAL").contains(item.getStatus()))
                .map(this::executionSummary)
                .limit(20)
                .toList());

        result.put("pendingApprovals", approvals);
        result.put("recentFailures", recentFailures);

        result.put("availableApplications", apps.stream()
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()) || item.getCurrentRevisionId() != null)
                .limit(20)
                .map(this::applicationSummary)
                .toList());

        result.put("myRequests", executions.stream()
                .map(this::executionSummary)
                .limit(20)
                .toList());

        result.put("assignedTasks", approvals);

        result.put("applicationHealth", apps.stream()
                .limit(20)
                .map(this::applicationHealth)
                .toList());

        result.put("metrics", Map.of(
                "publishedApps", apps.stream().filter(item -> item.getCurrentRevisionId() != null).count(),
                "activeExecutions", executions.stream().filter(item -> List.of("QUEUED", "RUNNING", "PAUSING", "WAITING_APPROVAL").contains(item.getStatus())).count(),
                "pendingApprovals", (long) approvals.size(),
                "failedExecutions", executions.stream().filter(item -> "FAILED".equals(item.getStatus())).count(),
                "sourceAvailable", 1L
        ));

        result.put("scope", platformScope ? "PLATFORM" : "TENANT");
        result.put("projectName", platformScope ? "平台运营控制台" : "企业运营工作台");
        result.put("canManageTenants", platformScope);

        return result;
    }

    /**
     * 格式化执行上下文结构摘要。
     */
    private Map<String, Object> executionSummary(PlatformExecutionContextEntity item) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("executionId", item.getExecutionId());
        summary.put("appId", item.getAppId());
        summary.put("versionId", item.getVersionId());
        summary.put("status", item.getStatus());
        summary.put("currentNodeId", item.getCurrentNodeId());
        summary.put("errorMessage", item.getErrorMessage());
        summary.put("startedAt", item.getStartedAt());
        return summary;
    }

    /**
     * 格式化工作流应用简要信息。
     */
    private Map<String, Object> applicationSummary(OrchestrationAppEntity item) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("appId", item.getId());
        summary.put("name", item.getAppName());
        summary.put("status", item.getStatus());
        summary.put("updatedAt", item.getUpdatedAt());
        return summary;
    }

    /**
     * 格式化应用运行健康度状态。
     */
    private Map<String, Object> applicationHealth(OrchestrationAppEntity item) {
        Map<String, Object> health = applicationSummary(item);
        health.put("healthStatus", item.getCurrentRevisionId() == null ? "DRAFT" : "AVAILABLE");
        return health;
    }
}

