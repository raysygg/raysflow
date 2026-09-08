package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.TenantEntitlementService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.OveragePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

/**
 * SaaS 租户套餐与资源权益准入（Entitlement & Admission）REST 控制器。
 * 负责提供租户已配权益列表查询、商业套餐版本定义、租户订阅套餐开通以及高并发 API/Agent 执行前置配额与权限准入校验（Admission Check）接口。
 */
@Tag(name = "SaaS 权益准入", description = "套餐版本、租户订阅、配额权益与执行前置准入校验")
@RestController
@RequestMapping("/api/saas/entitlements")
public class SaasEntitlementController {

    /** 套餐变更角色 */
    private static final Set<String> SUBSCRIPTION_MANAGER_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    /** 租户权益与准入校验服务 */
    private final TenantEntitlementService service;

    /**
     * 构造函数注入权益服务。
     */
    public SaasEntitlementController(TenantEntitlementService service) {
        this.service = service;
    }

    /**
     * 获取当前租户拥有的所有功能模块、Token 额度和并发数等权益配额列表。
     *
     * @param user 当前登录用户
     * @return 租户权益明细
     */
    @Operation(summary = "获取租户权益配额", description = "查询当前租户已购买或绑定的功能与资源额度清单。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.entitlements(user.getTenantId()));
    }

    /**
     * 创建全新的商业套餐版本（Plan Version）定义（如 Enterprise 2026 基础版）。
     *
     * @param user 当前登录用户
     * @param command 套餐定义指令
     * @return 创建的套餐版本记录
     */
    @Operation(summary = "创建商业套餐版本", description = "定义商业化套餐的版本号、包含权益、超限策略与阶梯计费单价。")
    @PostMapping("/plans")
    public ApiResponse<?> createPlan(@AuthenticationPrincipal SecurityUser user,
                                     @RequestBody TenantEntitlementService.PlanVersionCommand command) {
        requireManager(user);
        service.requireAdministrativeWrite(user, "创建套餐版本");
        return ApiResponse.ok(service.createPlan(user, command));
    }

    /** 订阅套餐请求体 */
    public record SubscribeRequest(Long tenantId, Long planVersionId, OveragePolicy overagePolicy, LocalDate startsOn) { }

    /**
     * 为指定租户订购或升级商业套餐。
     *
     * @param user 当前登录用户
     * @param request 包含目标租户、套餐 ID、超额处理策略与生效日期的请求
     * @return 订阅绑定记录
     */
    @Operation(summary = "租户订购套餐", description = "为租户绑定指定套餐版本，并配置用量溢出（Block / Auto-Pay）处理策略。")
    @PostMapping("/subscriptions")
    public ApiResponse<?> subscribe(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody SubscribeRequest request) {
        requireManager(user);
        service.requireAdministrativeWrite(user, "变更租户订阅");
        return ApiResponse.ok(service.subscribe(user, request.tenantId(), request.planVersionId(), request.overagePolicy(), request.startsOn()));
    }

    /** 准入校验请求体 */
    public record AdmissionBody(AdmissionRequest request, boolean securityAllowed, boolean shadowMode) { }

    /**
     * 运行执行引擎发起前置准入校验（校验租户余额、欠费状态、Token 配额与影子模式验证）。
     *
     * @param body 包含准入请求、安全授权标志与影子模式标头的请求
     * @return 准入结果（包含放行 ALLOW / 拦截 REJECT 及具体拦截原因）
     */
    @Operation(summary = "执行前置准入校验", description = "实时校验请求在配额、防刷限流与欠费拦截维度是否可安全放行。")
    @PostMapping("/admission")
    public ApiResponse<?> admission(@AuthenticationPrincipal SecurityUser user, @RequestBody AdmissionBody body) {
        if (body.request() == null || user == null) {
            throw new AuthorizationDeniedException("准入检查缺少当前登录租户上下文。");
        }
        AdmissionRequest requested = body.request();
        AdmissionRequest scoped = new AdmissionRequest(user.getTenantId(), requested.applicationId(), requested.featureCode(),
                requested.estimatedQuantity(), requested.operation(), requested.requestId());
        return ApiResponse.ok(service.admit(scoped, true, body.shadowMode()));
    }

    /** 校验套餐管理角色 */
    private void requireManager(SecurityUser user) {
        if (user == null || user.getRoles().stream().noneMatch(SUBSCRIPTION_MANAGER_ROLES::contains)) {
            throw new AuthorizationDeniedException("当前账号没有变更套餐和订阅的权限。");
        }
    }
}

