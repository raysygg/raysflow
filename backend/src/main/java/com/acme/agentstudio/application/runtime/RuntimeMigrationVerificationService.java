package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.workflow.OrchestrationAuthorizationService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEnvironmentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEnvironmentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时应用发布门禁与平滑迁移预检服务（Runtime Migration Verification Service）。
 * 只读预检当前租户边界内的应用绑定关系：包含草稿就绪（DRAFT）、流程图有效性（FLOW）、权限完整性（PERMISSION）
 * 以及生产环境活动版本绑定（RELEASE），生成可直接向前端展示的发布门禁阻断列表（blockers）。
 */
@Service
public class RuntimeMigrationVerificationService {

    /** 生产环境标识 */
    private static final String PRODUCTION_ENVIRONMENT = "PRODUCTION";

    /** 校验通过状态文本 */
    private static final String CHECK_PASSED = "PASSED";

    /** 校验阻断状态文本 */
    private static final String CHECK_BLOCKED = "BLOCKED";

    /** 应用草稿版本 Mapper */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /** 环境绑定 Mapper */
    private final OrchestrationEnvironmentMapper environmentMapper;

    /** 发布版本 Mapper */
    private final OrchestrationVersionMapper versionMapper;

    /** 编排鉴权服务 */
    private final OrchestrationAuthorizationService authorizationService;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入所有依赖服务。
     */
    public RuntimeMigrationVerificationService(
            OrchestrationDraftRevisionMapper draftMapper,
            OrchestrationEnvironmentMapper environmentMapper,
            OrchestrationVersionMapper versionMapper,
            OrchestrationAuthorizationService authorizationService,
            ObjectMapper objectMapper
    ) {
        this.draftMapper = draftMapper;
        this.environmentMapper = environmentMapper;
        this.versionMapper = versionMapper;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
    }

    /**
     * 对指定应用执行平滑迁移与发布前的全套门禁预检。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID
     * @return 门禁预检结果 Map
     */
    public Map<String, Object> verify(SecurityUser user, Long applicationId) {
        requireUser(user);
        if (applicationId == null || applicationId <= 0) {
            throw new IllegalArgumentException("预检应用时，应用标识 applicationId 必须大于零。");
        }

        List<Map<String, Object>> checks = new ArrayList<>();

        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationDraftRevisionEntity::getAppId, applicationId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo)
                .last("LIMIT 1"));
        add(checks, "DRAFT", draft != null, (draft == null) ? "未找到该应用的有效草稿记录" : "草稿记录已成功绑定");

        boolean draftGraphReady = (draft != null) && hasGraph(draft.getGraphJson());
        add(checks, "FLOW", draftGraphReady, draftGraphReady ? "流程图结构完整且格式有效" : "流程图为空或 JSON 节点格式无效");

        boolean permissionReady = hasPermissions(user, applicationId);
        add(checks, "PERMISSION", permissionReady, permissionReady ? "已具备草稿编辑与运行完整权限" : "缺少草稿编辑或运行权限，请检查角色赋权");

        OrchestrationEnvironmentEntity environment = environmentMapper.selectOne(new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                .eq(OrchestrationEnvironmentEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationEnvironmentEntity::getAppId, applicationId)
                .eq(OrchestrationEnvironmentEntity::getEnvironmentCode, PRODUCTION_ENVIRONMENT));

        String currentVersionId = (environment == null) ? null : environment.getCurrentVersionId();
        OrchestrationVersionEntity release = (currentVersionId == null) ? null : versionMapper.selectOne(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationVersionEntity::getAppId, applicationId)
                .eq(OrchestrationVersionEntity::getVersionId, currentVersionId)
                .eq(OrchestrationVersionEntity::getStatus, "PUBLISHED"));

        add(checks, "RELEASE", release != null, (release == null) ? "生产环境尚未绑定已发布的固定版本" : "生产环境已成功绑定处于 PUBLISHED 状态的发布版本");

        boolean publishable = checks.stream().allMatch(item -> CHECK_PASSED.equals(item.get("status")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId", applicationId);
        result.put("tenantId", user.getTenantId());
        result.put("publishable", publishable);
        result.put("checks", checks);
        result.put("blockers", checks.stream().filter(item -> CHECK_BLOCKED.equals(item.get("status"))).toList());

        return result;
    }

    /** 检查用户是否具备应用草稿编辑与运行权限 */
    private boolean hasPermissions(SecurityUser user, Long applicationId) {
        try {
            authorizationService.require(user, applicationId, "READ_DRAFT");
            authorizationService.require(user, applicationId, "RUN");
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** 校验流程图 JSON 是否为非空节点数组 */
    private boolean hasGraph(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            return false;
        }
        try {
            JsonNode graph = objectMapper.readTree(graphJson);
            return graph != null && graph.path("nodes").isArray() && graph.path("nodes").size() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 向检查结果列表追加单项校验结论 */
    private void add(List<Map<String, Object>> checks, String key, boolean passed, String message) {
        checks.add(Map.of("key", key, "status", passed ? CHECK_PASSED : CHECK_BLOCKED, "message", message));
    }

    /** 安全用户凭证校验 */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }
}

