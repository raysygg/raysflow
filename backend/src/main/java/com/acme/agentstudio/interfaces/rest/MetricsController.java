package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.metrics.MetricsApplicationService;
import com.acme.agentstudio.application.metrics.PlatformEventFactService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台指标与用量分析 REST 控制器。
 * 负责提供租户大模型调用 Token 消耗总量统计、多维度（按模型、应用、用户）费用拆分分析以及平台事实事件日志（Fact Events）查询接口。
 */
@Tag(name = "平台指标", description = "用量、成本、执行事件和指标查询")
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    /** 平台指标应用服务 */
    private final MetricsApplicationService metricsApplicationService;

    /** 平台事实事件分析服务 */
    private final PlatformEventFactService eventFactService;

    /**
     * 构造函数注入指标相关服务。
     */
    public MetricsController(MetricsApplicationService metricsApplicationService, PlatformEventFactService eventFactService) {
        this.metricsApplicationService = metricsApplicationService;
        this.eventFactService = eventFactService;
    }

    /**
     * 获取指定时间范围内的平台总调用次数与 Token 消耗汇总指标。
     *
     * @param user 当前登录用户
     * @param days 查询天数跨度（默认 30 天）
     * @return 用量汇总指标
     */
    @Operation(summary = "获取平台用量汇总指标", description = "获取租户在近指定天数内的模型调用量与 Token 消耗统计。")
    @GetMapping("/summary")
    public ApiResponse<?> summary(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ApiResponse.ok(metricsApplicationService.summary(user.getTenantId(), days));
    }

    /**
     * 获取按维度（MODEL / APPLICATION / USER）细分拆解的费用与消耗柱状图/折线图数据。
     *
     * @param user 当前登录用户
     * @param days 查询天数跨度（默认 30 天）
     * @param dimension 拆分维度枚举（MODEL / APPLICATION / USER，默认 MODEL）
     * @return 细分数据
     */
    @Operation(summary = "获取按维度拆分的指标细目", description = "按模型、应用或用户维度多角分析消耗指标与花费趋势。")
    @GetMapping("/breakdown")
    public ApiResponse<?> breakdown(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "MODEL") String dimension
    ) {
        return ApiResponse.ok(metricsApplicationService.breakdown(user.getTenantId(), days, dimension));
    }

    /**
     * 查询租户下大模型调用与工作流执行的原始事实事件明细列表。
     *
     * @param user 当前登录用户
     * @param days 查询天数跨度（默认 30 天）
     * @return 事实事件列表
     */
    @Operation(summary = "获取平台事件明细列表", description = "获取当前租户下发生的模型调用事实与执行事件列表。")
    @GetMapping("/facts")
    public ApiResponse<?> facts(@AuthenticationPrincipal SecurityUser user,
                                @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(eventFactService.list(user.getTenantId(), days));
    }
}

