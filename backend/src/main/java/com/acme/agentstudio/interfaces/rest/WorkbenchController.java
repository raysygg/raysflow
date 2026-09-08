package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.project.WorkbenchApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人与团队企业工作台 REST 控制器。
 * 负责提供工作台待办任务（Drafts / Inbox）、我的应用列表、快捷入口、使用分析及近期操作历史等运营聚合数据接口。
 */
@Tag(name = "企业工作台", description = "个人待办、快捷应用与团队工作台概览")
@RestController
@RequestMapping("/api/project/workbench")
public class WorkbenchController {

    /** 工作台聚合应用服务 */
    private final WorkbenchApplicationService service;

    /**
     * 构造函数注入工作台服务。
     */
    public WorkbenchController(WorkbenchApplicationService service) {
        this.service = service;
    }

    /**
     * 获取当前登录用户的个人工作台运营聚合数据。
     *
     * @param user 当前登录用户
     * @return 工作台聚合数据视图
     */
    @Operation(summary = "获取工作台运营聚合", description = "拉取包含待办审批、快捷应用、最近编辑草稿与耗费额度的聚合视图。")
    @GetMapping
    public ApiResponse<?> aggregate(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.aggregate(user));
    }
}

