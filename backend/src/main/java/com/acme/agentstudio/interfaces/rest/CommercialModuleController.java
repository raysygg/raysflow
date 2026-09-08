package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.commercial.CommercialModuleApplicationService;
import com.acme.agentstudio.application.lifecycle.MarketplaceApplicationInstallationService;
import com.acme.agentstudio.application.lifecycle.MarketplaceApplicationInstallationService.MarketplaceInstallRequest;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.response.PageQuery;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商业化与应用集市 REST 控制器。
 * 负责提供多租户商业化授权拉取、工单审批收件箱、应用集市（Marketplace）模板克隆安装、模板版本升级预检（Preflight）以及商业审计日志查询接口。
 */
@Tag(name = "商业化模块", description = "套餐、计费、工单审批与应用集市模板管理")
@RestController
@RequestMapping("/api/commercial")
public class CommercialModuleController {

    /** 商业化模块应用层服务 */
    private final CommercialModuleApplicationService commercialModuleApplicationService;

    /** 集市模板克隆安装服务 */
    private final MarketplaceApplicationInstallationService marketplaceInstallationService;

    /**
     * 构造函数注入商业化依赖服务。
     */
    public CommercialModuleController(CommercialModuleApplicationService commercialModuleApplicationService,
                                       MarketplaceApplicationInstallationService marketplaceInstallationService) {
        this.commercialModuleApplicationService = commercialModuleApplicationService;
        this.marketplaceInstallationService = marketplaceInstallationService;
    }

    /**
     * 全局租户列表与商业化授权查询（仅限超级管理员访问）。
     *
     * @param user 当前登录用户
     * @return 包含全量租户及其套餐规格的列表
     */
    @Operation(summary = "获取全局租户列表", description = "超级管理员获取全平台租户列表及其商业化授权。")
    @GetMapping("/tenants")
    public ApiResponse<?> tenants(@AuthenticationPrincipal SecurityUser user) {
        if (!"SUPER_ADMIN".equals(user.getRole())) {
            return ApiResponse.fail("无权查看全局租户列表。");
        }
        return ApiResponse.ok(commercialModuleApplicationService.listTenants());
    }

    /**
     * 查询当前用户待审核的操作或发布工单。
     *
     * @param user 当前登录用户
     * @return 待审批工单列表
     */
    @Operation(summary = "获取待审批工单列表", description = "按当前用户的租户与角色权限获取待审核的操作或发布工单。")
    @GetMapping("/approvals")
    public ApiResponse<?> approvals(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(commercialModuleApplicationService.listApprovals(user.getTenantId(), user.getUserId(), user.getRole()));
    }

    /**
     * 分页查询统一任务收件箱（包含审批、补充材料和转交任务）。
     *
     * @param user 当前登录用户
     * @param page 请求页码（默认 1）
     * @param size 每页记录数（默认 20）
     * @param keyword 搜索关键字
     * @param type 任务分类
     * @return 任务收件箱分页数据
     */
    @Operation(summary = "统一任务收件箱", description = "按当前用户可见范围分页返回审批、补充材料和转交任务。")
    @GetMapping("/tasks")
    public ApiResponse<?> tasks(@AuthenticationPrincipal SecurityUser user,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String type) {
        PageQuery query = new PageQuery(page, size, keyword, "createdAt", "DESC");
        return ApiResponse.ok(commercialModuleApplicationService.listTaskInbox(
                user.getTenantId(), user.getUserId(), user.getRole(), query, type));
    }

    /**
     * 处理工单审批同意或驳回操作。
     *
     * @param id 审批工单 ID
     * @param user 当前登录用户
     * @param payload 包含 action ('APPROVE' / 'REJECT') 和 comment 留言的请求体
     * @return 审核操作处理结果
     */
    @Operation(summary = "处理工单审批操作", description = "对指定待办审批任务执行同意或驳回操作。")
    @PostMapping("/approvals/{id}/action")
    public ApiResponse<?> approveTask(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user,
            @RequestBody ApprovalDecisionRequest payload
    ) {
        if (!(user.hasRole("ADMIN") || user.hasRole("APPROVER") || user.hasRole("OPERATOR"))) {
            throw new com.acme.agentstudio.common.exception.AuthorizationDeniedException("当前用户没有审批任务处理权限");
        }
        if (payload == null) {
            throw new IllegalArgumentException("审批请求不能为空");
        }
        commercialModuleApplicationService.approveTask(id, user.getTenantId(), user.getUserId(),
                user.getUsername(), user.getRole(), payload.action(), payload.comment());
        return ApiResponse.ok("审核操作处理成功。", null);
    }

    /** 审批处理决策请求载荷 */
    public record ApprovalDecisionRequest(String action, String comment) { }

    /**
     * 浏览公共应用模板库与集市模板。
     *
     * @return 集市模板列表
     */
    @Operation(summary = "获取应用集市市场列表", description = "浏览公共应用模板库与预制 Agent/工作流集市。")
    @GetMapping("/marketplace")
    public ApiResponse<?> marketplace() {
        return ApiResponse.ok(commercialModuleApplicationService.listMarketplaceItems());
    }

    /**
     * 从应用集市一键克隆安装模板到当前租户控制台。
     *
     * @param user 当前登录用户
     * @param request 包含集市模板 ID 的安装请求
     * @return 克隆新建的应用草稿对象
     */
    @Operation(summary = "安装集市应用模板", description = "从应用集市一键克隆安装模板到当前租户控制台。")
    @PostMapping("/marketplace/install")
    public ApiResponse<?> installItem(@AuthenticationPrincipal SecurityUser user,
                                       @RequestBody MarketplaceInstallRequest request) {
        return ApiResponse.ok("已创建应用草稿。", marketplaceInstallationService.install(user, request));
    }

    /**
     * 执行应用集市模板安装前的预检（Preflight），检查缺失的环境变量或连接器依赖。
     *
     * @param user 当前登录用户
     * @param marketplaceItemId 集市模板 ID
     * @param request 替换依赖的配置参数
     * @return 预检报告
     */
    @Operation(summary = "应用安装预检", description = "检查模板依赖的模型、知识库与连接器是否在目标租户中准备就绪。")
    @PostMapping("/marketplace/{marketplaceItemId}/preflight")
    public ApiResponse<?> preflightMarketplaceItem(@AuthenticationPrincipal SecurityUser user,
                                                     @PathVariable Long marketplaceItemId,
                                                     @RequestBody(required = false) MarketplacePreflightRequest request) {
        return ApiResponse.ok(marketplaceInstallationService.preflight(user, marketplaceItemId,
                request == null ? java.util.List.of() : request.replacements()));
    }

    /** 集市模板预检请求载荷 */
    public record MarketplacePreflightRequest(
            java.util.List<MarketplaceApplicationInstallationService.DependencyReplacement> replacements) { }

    /**
     * 执行应用模板升级前的 Preflight 比对。
     *
     * @param user 当前登录用户
     * @param applicationId 本地应用 ID
     * @param marketplaceItemId 目标新版集市模板 ID
     * @return 升级预检比对报告
     */
    @Operation(summary = "集市应用升级预检", description = "比对本地已安装应用与线上最新模板的 Schema 节点差异。")
    @GetMapping("/marketplace/applications/{applicationId}/upgrade-preflight")
    public ApiResponse<?> preflightMarketplaceUpgrade(@AuthenticationPrincipal SecurityUser user,
                                                        @PathVariable Long applicationId,
                                                        @RequestParam Long marketplaceItemId) {
        return ApiResponse.ok(marketplaceInstallationService.preflightUpgrade(user, applicationId, marketplaceItemId));
    }

    /**
     * 执行应用模板从集市新版本的平滑升级。
     *
     * @param user 当前登录用户
     * @param applicationId 本地应用 ID
     * @param request 升级策略请求
     * @return 升级后的新版本应用草稿
     */
    @Operation(summary = "升级集市安装的应用", description = "将本地应用升级合并至集市的最新版本。")
    @PostMapping("/marketplace/applications/{applicationId}/upgrade")
    public ApiResponse<?> upgradeMarketplaceApplication(@AuthenticationPrincipal SecurityUser user,
                                                         @PathVariable Long applicationId,
                                                         @RequestBody MarketplaceApplicationInstallationService.UpgradeRequest request) {
        return ApiResponse.ok("已生成新的应用草稿。",
                marketplaceInstallationService.upgrade(user, applicationId, request));
    }

    /**
     * 获取租户关联的商业化组件与工单审计事件记录。
     *
     * @param user 当前登录用户
     * @return 审计事件日志列表
     */
    @Operation(summary = "获取商业化审计事件", description = "获取租户关联的商业化组件与工单审计事件记录。")
    @GetMapping("/audit-events")
    public ApiResponse<?> auditEvents(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(commercialModuleApplicationService.listAuditEvents(user.getTenantId()));
    }
}

