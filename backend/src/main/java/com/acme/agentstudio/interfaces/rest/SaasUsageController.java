package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.SaasUsageLedgerService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * SaaS 租户用量流水账本（Usage Ledger）REST 控制器。
 * 负责提供实时用量事件（Token / Storage / Execution Time）追加记账、手工用量调整（Adjustment）以及多维成本中心（Cost Center）用量聚合统计接口。
 */
@Tag(name = "SaaS 用量账本", description = "用量事件记账、人工用量调整与成本中心多维汇总")
@RestController
@RequestMapping("/api/saas/usage")
public class SaasUsageController {

    /** SaaS 用量账本应用服务 */
    private final SaasUsageLedgerService service;

    /**
     * 构造函数注入用量账本服务。
     */
    public SaasUsageController(SaasUsageLedgerService service) {
        this.service = service;
    }

    /**
     * 上报追加一条不可变用量扣费事实事件（Usage Event）。
     *
     * @param command 包含租户 ID、应用 ID、模型 Key、消耗 Token 数的记账指令
     * @return 记账成功响应
     */
    @Operation(summary = "追加用量扣费事件", description = "由 Runtime 引擎自动上报一次 API 调用或模型推理消耗的 Token/耗时用量。")
    @PostMapping("/events")
    public ApiResponse<?> append(@AuthenticationPrincipal SecurityUser user,
                                 @RequestBody SaasUsageLedgerService.UsageCommand command) {
        if (user == null || command == null || !user.getTenantId().equals(command.tenantId())) {
            throw new AuthorizationDeniedException("用量事件只能写入当前登录租户的账本。");
        }
        return ApiResponse.ok(service.append(command));
    }

    /**
     * 管理员人工补录或核减租户的特定用量额度（如冲抵测试耗损）。
     *
     * @param user 当前登录用户
     * @param command 包含调整数值、理由与关联单据的指令
     * @return 用量调整流水
     */
    @Operation(summary = "人工调整用量账目", description = "管理员因系统差错或测试补偿对租户用量进行人工加减调整。")
    @PostMapping("/adjustments")
    public ApiResponse<?> adjust(@AuthenticationPrincipal SecurityUser user,
                                 @RequestBody SaasUsageLedgerService.AdjustmentCommand command) {
        return ApiResponse.ok(service.adjust(user, command));
    }

    /**
     * 按时间段、应用 ID、LLM 模型 Key 或成本中心维度聚合查询用量与消费金库。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID（可选）
     * @param modelKey 模型 Key（可选）
     * @param costCenter 成本中心（可选）
     * @param from 起始时间（可选）
     * @param to 截止时间（可选）
     * @return 聚合统计汇总视图
     */
    @Operation(summary = "多维用量与成本汇总", description = "按部门成本中心、模型种类及应用筛选计算阶段用量与预估账面金额。")
    @GetMapping("/summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal SecurityUser user,
                                  @RequestParam(required = false) Long applicationId,
                                  @RequestParam(required = false) String modelKey,
                                  @RequestParam(required = false) String costCenter,
                                  @RequestParam(required = false) LocalDateTime from,
                                  @RequestParam(required = false) LocalDateTime to) {
        return ApiResponse.ok(service.aggregate(user.getTenantId(), applicationId, modelKey, costCenter, from, to));
    }
}

