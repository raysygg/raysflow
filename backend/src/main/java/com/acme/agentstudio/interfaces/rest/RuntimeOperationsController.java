package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.RuntimeOperationsService;
import com.acme.agentstudio.application.runtime.RuntimeRecoveryCommandService;
import com.acme.agentstudio.application.runtime.RuntimeRecoveryPreviewService;
import com.acme.agentstudio.application.runtime.RuntimeBatchDispositionService;
import com.acme.agentstudio.application.runtime.RuntimeOperationsHealthService;
import com.acme.agentstudio.application.runtime.RuntimeSloPolicyService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.runtime.RuntimeRecoveryContracts.RecoveryAction;
import com.acme.agentstudio.application.workflow.OrchestrationAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运行中心运维与断点恢复 REST 控制器。
 * 负责提供运行故障明细检索、执行 Trace 时间线、恢复影响预检（Preview）、死信消息重放（Replay）、批量故障处置、运维健康大盘、SLO 服务等级策略与版本质量对比接口。
 */
@Tag(name = "运行运维管理", description = "故障分析、死信重放、SLO 策略与批量处置")
@RestController
@RequestMapping("/api/runtime/operations")
public class RuntimeOperationsController {

    /** 基础运行运维服务 */
    private final RuntimeOperationsService operationsService;

    /** 恢复预检预览服务 */
    private final RuntimeRecoveryPreviewService previewService;

    /** 重放与指令恢复服务 */
    private final RuntimeRecoveryCommandService commandService;

    /** 批量处置服务 */
    private final RuntimeBatchDispositionService batchService;

    /** 运维健康大盘服务 */
    private final RuntimeOperationsHealthService healthService;

    /** SLO 质量策略服务 */
    private final RuntimeSloPolicyService sloPolicyService;

    /** 权限与授权服务 */
    private final OrchestrationAuthorizationService authorizationService;

    /**
     * 构造函数注入运行运维相关服务。
     */
    public RuntimeOperationsController(RuntimeOperationsService operationsService,
                                       RuntimeRecoveryPreviewService previewService,
                                       RuntimeRecoveryCommandService commandService,
                                       RuntimeBatchDispositionService batchService,
                                       RuntimeOperationsHealthService healthService,
                                       RuntimeSloPolicyService sloPolicyService,
                                       OrchestrationAuthorizationService authorizationService) {
        this.operationsService = operationsService;
        this.previewService = previewService;
        this.commandService = commandService;
        this.batchService = batchService;
        this.healthService = healthService;
        this.sloPolicyService = sloPolicyService;
        this.authorizationService = authorizationService;
    }

    /**
     * 条件分页查询租户下的运行失败与异常记录列表。
     *
     * @param user 当前登录用户
     * @param applicationId 目标应用 ID
     * @param releaseId 线上发布版本号
     * @param errorCategory 错误归类枚举
     * @param assignee 处理责任人用户 ID
     * @param status 故障处置状态
     * @param page 请求页码（默认 1）
     * @param pageSize 每页记录数（默认 20）
     * @return 失败记录分页列表
     */
    @Operation(summary = "查询失败记录列表", description = "按错误分类、应用版本与责任人筛选异常运行实例。")
    @GetMapping("/failures")
    public ApiResponse<?> failures(@AuthenticationPrincipal SecurityUser user,
                                   @RequestParam(required = false) Long applicationId,
                                   @RequestParam(required = false) String releaseId,
                                   @RequestParam(required = false) String errorCategory,
                                   @RequestParam(required = false) Long assignee,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "20") long pageSize) {
        authorizationService.require(user, applicationId, "RUN_VIEW");
        return ApiResponse.ok(operationsService.failures(user, applicationId, releaseId, errorCategory, assignee, status, page, pageSize));
    }

    /**
     * 获取指定运行实例（RunID）的详细节点 Event 执行时间线。
     *
     * @param user 当前登录用户
     * @param runId 运行实例 ID
     * @return 时间线事件列表
     */
    @Operation(summary = "获取运行执行时间线", description = "按事件发生顺序还原节点启动、耗时与报错快照。")
    @GetMapping("/runs/{runId}/timeline")
    public ApiResponse<?> timeline(@AuthenticationPrincipal SecurityUser user, @PathVariable String runId) {
        authorizationService.require(user, null, "RUN_VIEW");
        return ApiResponse.ok(operationsService.timeline(user, runId));
    }

    /** 恢复动作影响预检请求体 */
    public record PreviewRequest(String runId, RecoveryAction action, String operationKey, List<String> runIds) { }

    /**
     * 在真正执行重试或恢复动作之前，预览评估受影响的节点与数据侧影响。
     *
     * @param user 当前登录用户
     * @param request 包含操作类型与 RunID 的请求
     * @return 预检分析结果
     */
    @Operation(summary = "恢复动作预检", description = "在重放或恢复前模拟评估预估影响的下游节点数。")
    @PostMapping("/recovery/preview")
    public ApiResponse<?> preview(@AuthenticationPrincipal SecurityUser user, @RequestBody PreviewRequest request) {
        authorizationService.require(user, null, "RUN_RECOVERY_PREVIEW");
        return ApiResponse.ok(previewService.preview(user, request.runId(), request.action(), request.operationKey(), request.runIds()));
    }

    /** 重放死信请求体 */
    public record ReplayRequest(String runId, Long deadLetterId, String idempotencyKey, boolean confirmed) { }

    /**
     * 确认并重新播放指定的死信（DeadLetter）队列消息或失败运行实例。
     *
     * @param user 当前登录用户
     * @param request 包含确认标志与死信 ID 的重放请求
     * @return 重放触发响应
     */
    @Operation(summary = "重放死信运行", description = "重新将死信队列中积压或执行中途崩溃的任务投递回队列执行。")
    @PostMapping("/replay")
    public ApiResponse<?> replay(@AuthenticationPrincipal SecurityUser user, @RequestBody ReplayRequest request) {
        authorizationService.require(user, null, "RUN_REPLAY");
        if (request == null || !request.confirmed()) {
            throw new IllegalArgumentException("重放前必须确认影响范围");
        }
        return ApiResponse.ok(commandService.replay(user, request.runId(), request.deadLetterId(), request.idempotencyKey()));
    }

    /**
     * 批量处置（忽略/重试/转交责任人）多个运行失败记录。
     *
     * @param user 当前登录用户
     * @param command 批量处置命令
     * @return 批量操作处理结果
     */
    @Operation(summary = "批量处置故障", description = "对选中的批量失败 Run 统一执行忽略告警或责任转派。")
    @PostMapping("/failures/batch")
    public ApiResponse<?> batch(@AuthenticationPrincipal SecurityUser user,
                                @RequestBody RuntimeBatchDispositionService.BatchCommand command) {
        authorizationService.require(user, null, "RUN_BULK_DISPOSITION");
        return ApiResponse.ok(batchService.execute(user, command));
    }

    /**
     * 获取运维健康状态概览大盘（包含任务积压量、死信深度与调度 Worker 状态）。
     *
     * @param user 当前登录用户
     * @return 运维健康大盘数据
     */
    @Operation(summary = "获取运维健康大盘", description = "统计实时积压队列深度、死信增长率与 Worker 保持心跳数。")
    @GetMapping("/health")
    public ApiResponse<?> health(@AuthenticationPrincipal SecurityUser user) {
        authorizationService.require(user, null, "RUN_VIEW");
        return ApiResponse.ok(healthService.summary(user));
    }

    /**
     * 查询应用的 SLO（服务等级目标）策略列表。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID（可选）
     * @return SLO 策略配置列表
     */
    @Operation(summary = "获取 SLO 策略列表", description = "查询应用配置的响应时间、成功率与幻觉阈值 SLO 监控策略。")
    @GetMapping("/slo-policies")
    public ApiResponse<?> policies(@AuthenticationPrincipal SecurityUser user,
                                   @RequestParam(required = false) Long applicationId) {
        return ApiResponse.ok(sloPolicyService.list(user, applicationId));
    }

    /**
     * 保存或更新应用的 SLO 服务等级目标策略。
     *
     * @param user 当前登录用户
     * @param command SLO 策略保存对象
     * @return 保存成功响应
     */
    @Operation(summary = "保存 SLO 策略", description = "配置或更新针对特定应用或全局的质量 SLO 判定指标。")
    @PostMapping("/slo-policies")
    public ApiResponse<?> savePolicy(@AuthenticationPrincipal SecurityUser user,
                                     @RequestBody RuntimeSloPolicyService.PolicyCommand command) {
        return ApiResponse.ok(sloPolicyService.save(user, command));
    }

    /**
     * 手动触发评估特定版本在特定 SLO 策略下的达标情况。
     *
     * @param user 当前登录用户
     * @param policyCode 策略编码
     * @param applicationId 应用 ID
     * @param releaseId 发布版本 ID
     * @return SLO 评估报告结果
     */
    @Operation(summary = "评估 SLO 策略", description = "对指定 release 版本运行 SLO 达标判定，返回得分与达标结果。")
    @PostMapping("/slo-policies/{policyCode}/evaluate")
    public ApiResponse<?> evaluatePolicy(@AuthenticationPrincipal SecurityUser user,
                                         @PathVariable String policyCode,
                                         @RequestParam Long applicationId,
                                         @RequestParam(required = false) String releaseId) {
        return ApiResponse.ok(sloPolicyService.evaluate(user, applicationId, releaseId, policyCode));
    }

    /**
     * 比对特定 release 版本与基线（Baseline）版本在窗口期内的指标表现差异。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID
     * @param releaseId 目标测试版本
     * @param baselineReleaseId 参照基线版本
     * @param windowMinutes 评估时间窗口跨度（分钟，默认 60min）
     * @return 双版本质量对比数据
     */
    @Operation(summary = "版本质量基线对比", description = "横向比对新上线版本与旧基线版本在近窗口期内的 SLO 达成率。")
    @GetMapping("/release-comparison")
    public ApiResponse<?> compareReleases(@AuthenticationPrincipal SecurityUser user,
                                           @RequestParam Long applicationId,
                                           @RequestParam String releaseId,
                                           @RequestParam String baselineReleaseId,
                                           @RequestParam(defaultValue = "60") int windowMinutes) {
        return ApiResponse.ok(sloPolicyService.compare(user, applicationId, releaseId, baselineReleaseId, windowMinutes));
    }
}

