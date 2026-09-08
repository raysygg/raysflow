package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.TenantDataGovernanceService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.GovernanceRequestType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

/**
 * SaaS 多租户数据治理与合规管控 REST 控制器。
 * 负责提供数据保留策略（Retention Policy）定义、合规数据导出/彻底擦除申请、删除受影响数据预检、审批与异步执行、不可篡改审计证据链导出，以及诉讼法律冻结（Legal Hold）开立与解冻接口。
 */
@Tag(name = "SaaS 数据治理", description = "数据保留策略、合规导出与擦除、法律冻结 Legal Hold 与审计证据")
@RestController
@RequestMapping("/api/saas/governance")
public class SaasGovernanceController {

    /** 租户数据治理应用服务 */
    private final TenantDataGovernanceService service;
    private final com.acme.agentstudio.application.saas.TenantEntitlementService admissionService;
    /** 治理范围序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入治理服务。
     */
    public SaasGovernanceController(TenantDataGovernanceService service,
                                    com.acme.agentstudio.application.saas.TenantEntitlementService admissionService,
                                    ObjectMapper objectMapper) {
        this.service = service;
        this.admissionService = admissionService;
        this.objectMapper = objectMapper;
    }

    /** 数据保留策略创建请求体 */
    public record RetentionRequest(String dataCategory, int retentionDays, String legalBasis) { }

    /** 治理导出/擦除范围请求体 */
    public record GovernanceRequest(GovernanceScope scope) { }

    /** 法律冻结 Legal Hold 请求体 */
    public record HoldRequest(String holdCode, GovernanceScope scope, String reason) { }

    /** 用户可配置的结构化治理范围。 */
    public record GovernanceScope(String dataCategory, Long applicationId, LocalDate from, LocalDate to) { }

    /**
     * 获取租户当前生效中的所有数据保留（Retention）与过期自动清理策略。
     *
     * @param user 当前登录用户
     * @return 保留策略列表
     */
    @Operation(summary = "获取数据保留策略列表", description = "查询针对日志、对话、文档与知识库数据的生命周期保留天数。")
    @GetMapping("/retention")
    public ApiResponse<?> retention(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.activeRetentionPolicies(user));
    }

    /**
     * 新增或更新特定数据分类的数据保留天数及法律依据。
     *
     * @param user 当前登录用户
     * @param request 包含数据分类、保留天数与合规依据的请求
     * @return 新建的保留策略记录
     */
    @Operation(summary = "创建数据保留策略", description = "按 GDPR / 密码法等要求设置特定数据类别的存储上限与清理机制。")
    @PostMapping("/retention")
    public ApiResponse<?> createRetention(@AuthenticationPrincipal SecurityUser user, @RequestBody RetentionRequest request) {
        admissionService.requireAdministrativeWrite(user, "修改数据保留策略");
        return ApiResponse.ok(service.createRetentionPolicy(user,
                new TenantDataGovernanceService.RetentionPolicyCommand(request.dataCategory(), request.retentionDays(), request.legalBasis())));
    }

    /**
     * 预检评估指定数据范围执行彻底物理擦除（Hard Delete）所影响的记录数与表结构。
     *
     * @param user 当前登录用户
     * @param request 包含数据范围 JSON 的请求
     * @return 预检删除统计结果
     */
    @Operation(summary = "预检删除受影响范围", description = "在真正提交合规物理删除前预览受影响的记录数与关联实体。")
    @PostMapping("/delete/preview")
    public ApiResponse<?> preview(@AuthenticationPrincipal SecurityUser user, @RequestBody GovernanceRequest request) {
        return ApiResponse.ok(service.previewDelete(user, scopeJson(request.scope())));
    }

    /**
     * 提交合规数据导出或彻底物理删除治理申请（需经安全主管审批）。
     *
     * @param user 当前登录用户
     * @param type 治理请求类型（EXPORT / DELETE）
     * @param request 范围配置请求
     * @return 生成的治理工单记录
     */
    @Operation(summary = "提交数据治理申请工单", description = "发起数据导出打包或数据彻底销毁的治理申请工单。")
    @PostMapping("/requests/{type}")
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user,
                                 @PathVariable GovernanceRequestType type,
                                 @RequestBody GovernanceRequest request) {
        admissionService.requireAdministrativeWrite(user, "创建数据治理请求");
        return ApiResponse.ok(service.request(user, type, scopeJson(request.scope())));
    }

    /**
     * 查询租户下提交的所有历史数据治理工单列表。
     *
     * @param user 当前登录用户
     * @return 工单列表
     */
    @Operation(summary = "获取治理工单列表", description = "查询所有数据导出与物理销毁工单的审批与执行进展。")
    @GetMapping("/requests")
    public ApiResponse<?> requests(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(service.requests(user));
    }

    /**
     * 查询单条治理工单的详细流程与状态。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 工单详情
     */
    @Operation(summary = "获取单条治理工单详情", description = "查看数据治理工单的申请人、范围与审批意见。")
    @GetMapping("/requests/{requestId}")
    public ApiResponse<?> request(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        return ApiResponse.ok(service.request(user, requestId));
    }

    /**
     * 安全主管审批通过指定的合规数据治理工单。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 审批操作响应
     */
    @Operation(summary = "审批通过治理工单", description = "安全管理员审核通过数据导出或销毁工单。")
    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<?> approve(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        admissionService.requireAdministrativeWrite(user, "审批数据治理请求");
        service.approve(user, requestId);
        return ApiResponse.ok(true);
    }

    /**
     * 触发异步执行已审批通过的数据导出或擦除任务。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 任务启动响应
     */
    @Operation(summary = "执行治理工单任务", description = "启动后台管道进行加密数据打包导出或多表关联擦除。")
    @PostMapping("/requests/{requestId}/execute")
    public ApiResponse<?> execute(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        admissionService.requireAdministrativeWrite(user, "执行数据治理请求");
        var request = service.request(user, requestId);
        if (request == null) {
            throw new IllegalArgumentException("治理请求不存在");
        }
        return ApiResponse.ok(GovernanceRequestType.DELETE.name().equals(request.getRequestType())
                ? service.executeDelete(user, requestId)
                : service.executeExport(user, requestId));
    }

    /**
     * 对中途失败的治理工单发起后台重试（Retry）。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 重试发起响应
     */
    @Operation(summary = "重试失败的治理工单", description = "对中途抛错的导出或擦除管道重新发起任务重试。")
    @PostMapping("/requests/{requestId}/retry")
    public ApiResponse<?> retry(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        admissionService.requireAdministrativeWrite(user, "重试数据治理请求");
        return ApiResponse.ok(service.retry(user, requestId));
    }

    /**
     * 获取数据擦除或导出完成后的数字签名不可篡改合规证据链（Evidence）。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 证明合规操作的数据摘要与证书
     */
    @Operation(summary = "获取合规审计证据链", description = "导出具有加密散列与签名证明的数据已物理擦除证据。")
    @GetMapping("/requests/{requestId}/evidence")
    public ApiResponse<?> evidence(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        return ApiResponse.ok(service.evidence(user, requestId));
    }

    /**
     * 获取数据导出任务打包完成后的下载链接（含一次性 Token）。
     *
     * @param user 当前登录用户
     * @param requestId 工单 ID
     * @return 包含临时下载 URL 的响应
     */
    @Operation(summary = "获取导出数据下载链接", description = "获取加密 Zip 导出包的带鉴权临时下载链接。")
    @GetMapping("/requests/{requestId}/download")
    public ApiResponse<?> download(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        return ApiResponse.ok(service.download(user, requestId));
    }

    /**
     * 开立法律冻结（Legal Hold），强制禁止对特定范围的数据执行任何自动或手动擦除。
     *
     * @param user 当前登录用户
     * @param request 包含冻结代码、影响范围 JSON 与因由的请求
     * @return 冻结记录
     */
    @Operation(summary = "开立 Legal Hold 法律冻结", description = "因诉讼或监管调查冻结特定范围的数据，覆盖一切定时清理逻辑。")
    @PostMapping("/holds")
    public ApiResponse<?> hold(@AuthenticationPrincipal SecurityUser user, @RequestBody HoldRequest request) {
        admissionService.requireAdministrativeWrite(user, "创建法律冻结");
        return ApiResponse.ok(service.createHold(user, request.holdCode(), scopeJson(request.scope()), request.reason()));
    }

    /**
     * 接触指定的 Legal Hold 法律冻结状态。
     *
     * @param user 当前登录用户
     * @param holdId 冻结记录 ID
     * @return 解冻成功响应
     */
    @Operation(summary = "解冻 Legal Hold 法律状态", description = "解冻 Legal Hold，恢复默认的数据生命周期与保留清理策略。")
    @PostMapping("/holds/{holdId}/release")
    public ApiResponse<?> release(@AuthenticationPrincipal SecurityUser user, @PathVariable Long holdId) {
        admissionService.requireAdministrativeWrite(user, "解除法律冻结");
        service.releaseHold(user, holdId);
        return ApiResponse.ok(true);
    }

    /**
     * 校验并序列化结构化治理范围，原始 JSON 不进入外部接口契约。
     */
    private String scopeJson(GovernanceScope scope) {
        if (scope == null || !Set.of("ALL", "CONVERSATION", "DOCUMENT", "RUN", "AUDIT").contains(scope.dataCategory())) {
            throw new IllegalArgumentException("请选择有效的数据类别。");
        }
        if (scope.from() != null && scope.to() != null && scope.from().isAfter(scope.to())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期。");
        }
        try {
            return objectMapper.writeValueAsString(scope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("治理范围保存失败。", exception);
        }
    }
}

