package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.workflow.OrchestrationQueryService;
import com.acme.agentstudio.application.workflow.OrchestrationApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 编排应用与版本管理 REST 控制器。
 * 负责提供编排应用列表查询、应用发布版本历史检索、版本拓扑比对（Compare）以及从不可变发布版本反向衍生新建应用草稿（Draft Fork）接口。
 */
@Tag(name = "编排应用", description = "编排应用、草稿、版本和环境管理")
@RestController
@RequestMapping("/api/orchestration/apps")
public class OrchestrationAppController {

    /** 编排应用只读查询服务 */
    private final OrchestrationQueryService queryService;

    /** 编排应用发布与草稿核心服务 */
    private final OrchestrationApplicationService orchestrationService;

    /**
     * 构造函数注入编排服务依赖。
     */
    public OrchestrationAppController(OrchestrationQueryService queryService,
                                       OrchestrationApplicationService orchestrationService) {
        this.queryService = queryService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * 获取当前登录用户在当前租户下有权访问的编排应用列表。
     *
     * @param user 当前登录用户
     * @return 编排应用列表数据
     */
    @Operation(summary = "获取编排应用列表", description = "获取当前登录用户有权访问的编排应用列表。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(queryService.listApps(user));
    }

    /**
     * 查询指定编排应用的历史发布版本记录（Version Summary）。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @return 版本历史列表
     */
    @Operation(summary = "获取应用版本历史", description = "获取指定编排应用的发布历史版本记录。")
    @GetMapping("/{appId}/versions")
    public ApiResponse<?> versions(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        return ApiResponse.ok(queryService.versions(user, appId));
    }

    /**
     * 比对指定编排应用的两个不同发布版本（Left 与 Right）在图节点和全局参数上的配置差异。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param left 左侧对比基准版本号
     * @param right 右侧对比目标版本号
     * @return 版本比对结果
     */
    @Operation(summary = "对比应用版本差异", description = "对比指定编排应用的两个历史版本在节点拓扑和配置上的差异。")
    @GetMapping("/{appId}/versions/compare")
    public ApiResponse<?> compare(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                  @RequestParam String left, @RequestParam String right) {
        return ApiResponse.ok(queryService.compare(user, appId, left, right));
    }

    /**
     * 复制指定只读发布版本（Version）的内容，反向衍生生成可二次编辑的新工作流草稿（Draft）。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param versionId 源发布版本 ID
     * @return 新生成的草稿数据
     */
    @Operation(summary = "从发布版本创建草稿", description = "复制指定不可变发布版本生成新的应用草稿，不影响生产版本。")
    @PostMapping("/{appId}/versions/{versionId}/draft")
    public ApiResponse<?> createDraftFromVersion(@AuthenticationPrincipal SecurityUser user,
                                                 @PathVariable Long appId,
                                                 @PathVariable String versionId) {
        return ApiResponse.ok("已从发布版本创建新草稿。",
                orchestrationService.createDraftFromVersion(user, appId, versionId));
    }
}

