package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.project.ProjectOverviewApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目首页与概览看板 REST 控制器。
 * 负责提供工作空间项目首页的数据统计、各类 Agent/工作流资源分布以及用户角色权限范围内的数据概览接口。
 */
@Tag(name = "项目总览", description = "项目首页和平台概览数据")
@RestController
@RequestMapping("/api/project")
public class ProjectOverviewController {

    /** 项目概览应用服务 */
    private final ProjectOverviewApplicationService projectOverviewApplicationService;

    /**
     * 构造函数注入项目概览服务。
     */
    public ProjectOverviewController(ProjectOverviewApplicationService projectOverviewApplicationService) {
        this.projectOverviewApplicationService = projectOverviewApplicationService;
    }

    /**
     * 按当前登录用户的租户与角色权限获取平台项目首页概览指标。
     *
     * @param user 当前登录用户
     * @return 首页概览数据视图
     */
    @Operation(summary = "获取项目概览数据", description = "按当前登录用户的租户与角色权限获取平台项目首页概览指标。")
    @GetMapping("/overview")
    public ApiResponse<?> overview(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(projectOverviewApplicationService.getOverview(user.getTenantId(), user.getRole()));
    }
}

