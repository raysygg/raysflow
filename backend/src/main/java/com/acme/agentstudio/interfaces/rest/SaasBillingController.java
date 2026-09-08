package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.SaasBillingLifecycleService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

/**
 * SaaS 多租户账期与计费结算 REST 控制器。
 * 负责提供账期（Billing Period）开启、月度草稿账单生成、账单终态关账（Finalize）、贷记/借记冲销调整单（Credit / Debit Note）开立以及同步第三方财务 ERP 系统接口。
 */
@Tag(name = "SaaS 计费结算", description = "账期关账、账单结算、财务调整单与 ERP 同步")
@RestController
@RequestMapping("/api/saas/billing")
public class SaasBillingController {

    /** 可执行财务写操作的角色 */
    private static final Set<String> FINANCE_MANAGER_ROLES = Set.of("SUPER_ADMIN", "FINANCE");

    /** SaaS 账费生命周期服务 */
    private final SaasBillingLifecycleService service;
    private final com.acme.agentstudio.application.saas.TenantEntitlementService admissionService;

    /**
     * 构造函数注入账费生命周期服务。
     */
    public SaasBillingController(SaasBillingLifecycleService service,
                                 com.acme.agentstudio.application.saas.TenantEntitlementService admissionService) {
        this.service = service;
        this.admissionService = admissionService;
    }

    /**
     * 查询当前租户财务工作台状态。
     *
     * @param user 当前登录用户
     * @return 账期、账单、调整项和同步状态
     */
    @GetMapping("/overview")
    public ApiResponse<?> overview(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.overview(user));
    }

    /** 开启新账期请求体 */
    public record PeriodRequest(LocalDate start, LocalDate end, String currency) { }

    /**
     * 为租户开启新的月度或季度计费结算账期（Period）。
     *
     * @param user 当前登录用户
     * @param request 包含账期起止日期与结算币种的请求
     * @return 开启的账期对象
     */
    @Operation(summary = "开启新计费账期", description = "按自然月或约定周期初始化多租户资源计费周期。")
    @PostMapping("/periods")
    public ApiResponse<?> period(@AuthenticationPrincipal SecurityUser user, @RequestBody PeriodRequest request) {
        requireFinanceManager(user);
        admissionService.requireAdministrativeWrite(user, "开启计费账期");
        return ApiResponse.ok(service.openPeriod(user.getTenantId(), request.start(), request.end(), request.currency()));
    }

    /**
     * 针对指定账期汇总用量事件并生成草稿状态出账账单（Draft Invoice）。
     *
     * @param user 当前登录用户
     * @param periodId 账期 ID
     * @return 草稿账单对象
     */
    @Operation(summary = "生成草稿账单", description = "根据账期内的 Token 与存储用量快照计算费用并生成草稿账单。")
    @PostMapping("/periods/{periodId}/invoice")
    public ApiResponse<?> draft(@AuthenticationPrincipal SecurityUser user, @PathVariable Long periodId) {
        requireFinanceManager(user);
        admissionService.requireAdministrativeWrite(user, "生成草稿账单");
        return ApiResponse.ok(service.generateDraft(user, periodId));
    }

    /**
     * 确认并关账指定草稿账单，生成具有法律与财务效应的终态发票（Finalized Invoice）。
     *
     * @param user 当前登录用户
     * @param invoiceId 账单 ID
     * @return 关账发票对象
     */
    @Operation(summary = "关账并终态发票", description = "把草稿账单锁定为正式关账发票，禁止后续用量冲销覆盖。")
    @PostMapping("/invoices/{invoiceId}/finalize")
    public ApiResponse<?> finalizeInvoice(@AuthenticationPrincipal SecurityUser user, @PathVariable Long invoiceId) {
        requireFinanceManager(user);
        admissionService.requireAdministrativeWrite(user, "确认最终账单");
        return ApiResponse.ok(service.finalizeInvoice(user, invoiceId));
    }

    /**
     * 开立针对账单的财务冲销调整单（Credit Note / Debit Note）。
     *
     * @param user 当前登录用户
     * @param command 包含调整金额、类型与原因的指令
     * @return 调整单记录
     */
    @Operation(summary = "开立财务调整单", description = "针对历史扣费差错开立贷记凭单（退款/抵扣）或借记补收单。")
    @PostMapping("/notes")
    public ApiResponse<?> note(@AuthenticationPrincipal SecurityUser user, @RequestBody SaasBillingLifecycleService.NoteCommand command) {
        requireFinanceManager(user);
        admissionService.requireAdministrativeWrite(user, "创建账单调整项");
        return ApiResponse.ok(service.createNote(user, command));
    }

    /** 财务同步请求体 */
    public record SyncRequest(String adapterType, String idempotencyKey) { }

    /**
     * 将关账发票推送到外部 ERP 财务系统（如 SAP / Kingdee / Stripe）。
     *
     * @param user 当前登录用户
     * @param invoiceId 账单 ID
     * @param request 包含适配器类型与幂等 Key 的请求
     * @return 推送同步结果
     */
    @Operation(summary = "同步账单到 ERP 系统", description = "按适配器协议将应收账单推送给企业内部 ERP 财务软件。")
    @PostMapping("/invoices/{invoiceId}/sync")
    public ApiResponse<?> sync(@AuthenticationPrincipal SecurityUser user, @PathVariable Long invoiceId, @RequestBody SyncRequest request) {
        requireFinanceManager(user);
        admissionService.requireAdministrativeWrite(user, "同步外部财务系统");
        return ApiResponse.ok(service.sync(user, invoiceId, request.adapterType(), request.idempotencyKey()));
    }

    /**
     * 导出已确认账单并记录财务审计。
     *
     * @param user 当前登录用户
     * @param invoiceId 账单 ID
     * @return 安全账单文件内容
     */
    @GetMapping("/invoices/{invoiceId}/export")
    public ApiResponse<?> export(@AuthenticationPrincipal SecurityUser user, @PathVariable Long invoiceId) {
        return ApiResponse.ok(service.exportInvoice(user, invoiceId));
    }

    /** 校验财务管理角色 */
    private void requireFinanceManager(SecurityUser user) {
        if (user == null || user.getRoles().stream().noneMatch(FINANCE_MANAGER_ROLES::contains)) {
            throw new AuthorizationDeniedException("当前账号没有执行账期和账单变更的财务权限。");
        }
    }
}

