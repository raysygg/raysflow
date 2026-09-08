package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.workflow.OrchestrationQueryService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runtime 可运行应用目录 REST 控制器。
 * 负责提供工作空间控制台与客户端页面查询当前登录用户具有运行权限的正式 Runtime 应用与 Agent 目录接口。
 */
@Tag(name = "Runtime 应用", description = "Runtime 应用目录和应用上下文")
@RestController
@RequestMapping("/api/runtime/applications")
public class RuntimeApplicationController {

    /** 编排应用只读查询服务 */
    private final OrchestrationQueryService queryService;

    /**
     * 构造函数注入查询服务。
     */
    public RuntimeApplicationController(OrchestrationQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 获取当前登录用户可交互调用的所有 Runtime 应用与 Agent 汇总目录。
     *
     * @param user 当前登录用户
     * @return 应用列表
     */
    @Operation(summary = "获取 Runtime 应用目录", description = "获取当前登录用户有权在线调试或发起的 Runtime 应用列表。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
        return ApiResponse.ok(queryService.listApps(user));
    }
}

