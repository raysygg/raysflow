package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.platform.PlatformConsoleApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台运营总控台 REST 控制器。
 * 负责提供超级管理员跨租户监控、平台全局健康度、基础资源水位、多租户配额分配以及安全风险诊断摘要接口。
 */
@Tag(name = "平台控制台", description = "跨租户运维、资源监控与风险总控台")
@RestController
@RequestMapping("/api/platform/console")
public class PlatformConsoleController {

    /** 平台控制台服务 */
    private final PlatformConsoleApplicationService service;

    /**
     * 构造函数注入控制台应用服务。
     */
    public PlatformConsoleController(PlatformConsoleApplicationService service) {
        this.service = service;
    }

    /**
     * 查询跨租户健康度、资源消耗、配额与风险看板摘要（仅限超级管理员）。
     *
     * @param user 当前登录用户
     * @return 控制台概览数据
     */
    @Operation(summary = "查询跨租户概览", description = "超级管理员查询全平台跨租户健康、资源水位、配额分布与风险指标摘要。")
    @GetMapping("/overview")
    public ApiResponse<?> overview(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.overview(user));
    }
}

