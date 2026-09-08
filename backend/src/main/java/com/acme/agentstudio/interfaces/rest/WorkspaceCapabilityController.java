package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.workspace.WorkspaceCapabilityApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作区功能特性与能力包（Workspace Capability Bundle）REST 控制器。
 * 负责提供客户端前端路由导航显示控制、高阶组件特性开关（Feature Flags）以及用户在当前工作区中的可用能力集合接口。
 */
@Tag(name = "工作区能力", description = "工作区功能开关、导航路由能力与菜单显隐控制")
@RestController
@RequestMapping("/api/project/workspace")
public class WorkspaceCapabilityController {

    /** 工作区能力应用服务 */
    private final WorkspaceCapabilityApplicationService service;

    /**
     * 构造函数注入工作区能力服务。
     */
    public WorkspaceCapabilityController(WorkspaceCapabilityApplicationService service) {
        this.service = service;
    }

    /**
     * 获取当前登录用户在当前租户工作区中的功能开关、权限许可与导航能力包。
     *
     * @param user 当前登录用户
     * @return 能力包配置视图
     */
    @Operation(summary = "获取当前用户的工作区能力包", description = "用于前端动态生成工作空间导航菜单、控制功能按钮显隐与组件权限。")
    @GetMapping("/capabilities")
    public ApiResponse<?> capabilities(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.getCapabilities(user));
    }
}

