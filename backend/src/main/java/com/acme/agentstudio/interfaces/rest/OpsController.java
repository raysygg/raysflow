package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.application.ops.OpsApplicationService;
import com.acme.agentstudio.application.platform.PlatformReadinessApplicationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维管理与系统就绪度诊断 REST 控制器。
 * 负责提供租户运行健康快照（Snapshot）与平台基础依赖（MySQL/Redis/Qdrant/Model API）就绪度探针检查接口。
 */
@Tag(name = "运维管理", description = "平台运行状态、任务和运维操作")
@RestController
@RequestMapping("/api/ops")
public class OpsController {

    /** 运维应用层服务 */
    private final OpsApplicationService opsApplicationService;

    /** 平台就绪度探针服务 */
    private final PlatformReadinessApplicationService platformReadinessApplicationService;

    /**
     * 构造函数注入运维与探针服务。
     */
    public OpsController(OpsApplicationService opsApplicationService,
                         PlatformReadinessApplicationService platformReadinessApplicationService) {
        this.opsApplicationService = opsApplicationService;
        this.platformReadinessApplicationService = platformReadinessApplicationService;
    }

    /**
     * 获取当前租户下的系统运行快照（包含活期 Worker 节点、排队任务数与异常指标）。
     *
     * @param user 当前登录用户
     * @return 运维快照数据
     */
    @Operation(summary = "获取运维快照数据", description = "获取当前租户下的运维快照与系统监控状态。")
    @GetMapping("/snapshot")
    public ApiResponse<?> snapshot(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(opsApplicationService.getSnapshot(user.getTenantId()));
    }

    /**
     * 执行平台基础设施就绪度诊断探针检查。
     *
     * @return 包含数据源、缓存、向量库与模型调用的综合健康指标
     */
    @Operation(summary = "检查平台就绪状态", description = "返回平台依赖和运行能力的诊断结果，供管理页面和发布前检查使用。")
    @GetMapping("/readiness")
    public ApiResponse<?> readiness() {
        return ApiResponse.ok(platformReadinessApplicationService.check());
    }
}

