package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService.CreateEvaluationRunRequest;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService.CreateSuiteRequest;
import com.acme.agentstudio.application.lifecycle.ApplicationEvaluationService.CreateSuiteVersionRequest;
import com.acme.agentstudio.application.lifecycle.ApplicationReleaseGateService;
import com.acme.agentstudio.application.lifecycle.ReleaseCandidateService;
import com.acme.agentstudio.application.workflow.OrchestrationApplicationService;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationPublishRequest;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationRollbackRequest;
import com.acme.agentstudio.domain.workflow.model.OrchestrationVersionSummary;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 应用生命周期与发布门禁 REST 控制器。
 * 负责提供发布候选版本（Candidate）建立、拓扑 Diff 比对、评测集（EvaluationSuite）管理、自动化质量门禁（GateReport）校验、人工豁免（Override）以及环境版本发布与回滚接口。
 */
@Tag(name = "应用生命周期管理", description = "候选版本、质量门禁、评测任务、发布与回滚")
@RestController
@RequestMapping("/api/runtime/applications")
public class ApplicationLifecycleController {

    /**
     * 发布候选版本服务
     */
    private final ReleaseCandidateService candidateService;

    /**
     * 自动化评测与 Suite 管理服务
     */
    private final ApplicationEvaluationService evaluationService;

    /**
     * 发布门禁卡点与豁免控制服务
     */
    private final ApplicationReleaseGateService gateService;

    /**
     * 编排应用核心发布服务
     */
    private final OrchestrationApplicationService orchestrationService;

    /**
     * 构造函数注入生命周期管理所需的基础依赖。
     */
    public ApplicationLifecycleController(ReleaseCandidateService candidateService,
                                          ApplicationEvaluationService evaluationService,
                                          ApplicationReleaseGateService gateService,
                                          OrchestrationApplicationService orchestrationService) {
        this.candidateService = candidateService;
        this.evaluationService = evaluationService;
        this.gateService = gateService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * 为指定应用创建发布候选版本（Candidate）。
     *
     * @param user          当前登录用户
     * @param applicationId 目标应用 ID
     * @param request       创建载荷（期待修订号、变更摘要）
     * @return 候选版本创建结果
     */
    @Operation(summary = "创建发布候选版本", description = "将当前草稿固化为可评测、可测试的 Candidate 候选版本。")
    @PostMapping("/{applicationId}/candidates")
    public ApiResponse<?> createCandidate(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                          @RequestBody(required = false) CandidateCreateRequest request) {
        return ApiResponse.ok("候选版本已创建。", candidateService.create(user, applicationId,
                request == null ? null : request.expectedRevisionNo(), request == null ? null : request.changeSummary()));
    }

    /**
     * 获取应用的所有历史发布候选版本列表。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @return 候选版本列表数据
     */
    @Operation(summary = "获取候选版本列表", description = "按应用 ID 查询历史上固化的所有 Candidate 列表。")
    @GetMapping("/{applicationId}/candidates")
    public ApiResponse<?> candidates(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(candidateService.list(user, applicationId));
    }

    /**
     * 获取指定候选版本的详细元数据与拓扑定义。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId   候选版本 ID
     * @return 候选版本详情
     */
    @Operation(summary = "获取候选版本详情", description = "查看具体 Candidate 版本的草稿状态、快照与拓扑定义。")
    @GetMapping("/{applicationId}/candidates/{candidateId}")
    public ApiResponse<?> candidate(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                    @PathVariable Long candidateId) {
        return ApiResponse.ok(candidateService.detail(user, applicationId, candidateId));
    }

    /**
     * 比对指定候选版本与当前线上/前一版本的节点图 Diff 差异。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId   候选版本 ID
     * @return 节点与连线 Diff 数据
     */
    @Operation(summary = "获取候选版本 Diff 差异", description = "比对当前 Candidate 与上一次线上版本的图节点和参数差异。")
    @GetMapping("/{applicationId}/candidates/{candidateId}/diff")
    public ApiResponse<?> diff(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                               @PathVariable Long candidateId) {
        return ApiResponse.ok(candidateService.diff(user, applicationId, candidateId));
    }

    /**
     * 为应用创建新的质量评测集（Evaluation Suite）。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param request       包含测试用例规范与期望产出的请求
     * @return 评测集创建结果
     */
    @Operation(summary = "创建评测集", description = "为应用新建用于回归验证的结构化测试用例集。")
    @PostMapping("/{applicationId}/evaluation-suites")
    public ApiResponse<?> createSuite(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                      @RequestBody CreateSuiteRequest request) {
        return ApiResponse.ok("评测集版本已创建。", evaluationService.createSuite(user, applicationId, request));
    }

    /** 查询应用下的命名评测集。 */
    @GetMapping("/{applicationId}/evaluation-suites")
    public ApiResponse<?> suites(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(evaluationService.listSuites(user, applicationId));
    }

    /** 查询评测集的不可变历史版本。 */
    @GetMapping("/{applicationId}/evaluation-suites/{suiteId}/versions")
    public ApiResponse<?> suiteVersions(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                        @PathVariable Long suiteId) {
        return ApiResponse.ok(evaluationService.listVersions(user, applicationId, suiteId));
    }

    /** 创建评测集的新不可变版本。 */
    @PostMapping("/{applicationId}/evaluation-suites/{suiteId}/versions")
    public ApiResponse<?> createSuiteVersion(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                             @PathVariable Long suiteId, @RequestBody CreateSuiteVersionRequest request) {
        return ApiResponse.ok("评测集新版本已创建。", evaluationService.createVersion(user, applicationId, suiteId, request));
    }

    /** 查询指定评测集版本的样例列表。 */
    @GetMapping("/{applicationId}/evaluation-suite-versions/{suiteVersionId}/cases")
    public ApiResponse<?> suiteCases(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                     @PathVariable Long suiteVersionId) {
        return ApiResponse.ok(evaluationService.listCases(user, applicationId, suiteVersionId));
    }

    /**
     * 针对候选版本触发自动化质量评测任务。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param request       包含评测集 ID 与 Candidate ID 的请求
     * @return 异步评测任务提交状态
     */
    @Operation(summary = "触发评测任务", description = "对指定 Candidate 运行评测集，计算正确率、幻觉率与平均延迟。")
    @PostMapping("/{applicationId}/evaluations")
    public ApiResponse<?> createEvaluation(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                           @RequestBody CreateEvaluationRunRequest request) {
        return ApiResponse.ok("评测任务已创建。", evaluationService.createRun(user, applicationId, request));
    }

    /**
     * 查询应用历史触发的所有评测运行记录列表。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @return 评测运行历史列表
     */
    @Operation(summary = "获取评测运行列表", description = "查询应用历史提交的评测任务及其汇总指标。")
    @GetMapping("/{applicationId}/evaluations")
    public ApiResponse<?> evaluations(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId) {
        return ApiResponse.ok(evaluationService.listRuns(user, applicationId));
    }

    /**
     * 查看特定评测任务的具体得分报告与用例明细。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param runId         评测运行 ID
     * @return 评测报告详情数据
     */
    @Operation(summary = "获取评测报告详情", description = "查看某次评测运行的具体测试用例打分与出参。")
    @GetMapping("/{applicationId}/evaluations/{runId}")
    public ApiResponse<?> evaluation(@AuthenticationPrincipal SecurityUser user,
                                     @PathVariable Long applicationId,
                                     @PathVariable Long runId) {
        return ApiResponse.ok(evaluationService.detail(user, applicationId, runId));
    }

    /** 为失败或部分完成的评测创建新的重试任务。 */
    @PostMapping("/{applicationId}/evaluations/{runId}/retry")
    public ApiResponse<?> retryEvaluation(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long applicationId,
                                          @PathVariable Long runId) {
        return ApiResponse.ok("评测重试任务已创建。", evaluationService.retry(user, applicationId, runId));
    }

    /** 删除指定的评测任务及结果数据。 */
    @Operation(summary = "删除评测任务", description = "删除指定的历史评测运行记录及其关联的测试用例打分与证据。")
    @DeleteMapping("/{applicationId}/evaluations/{runId}")
    public ApiResponse<?> deleteEvaluation(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long applicationId,
                                           @PathVariable Long runId) {
        evaluationService.deleteRun(user, applicationId, runId);
        return ApiResponse.ok("评测任务已删除。");
    }

    /** 批量清理评测任务（默认清理未通过的失败任务，也可清空全部）。 */
    @Operation(summary = "批量清理评测任务", description = "按范围清理当前应用下的历史评测任务。")
    @DeleteMapping("/{applicationId}/evaluations")
    public ApiResponse<?> cleanEvaluations(@AuthenticationPrincipal SecurityUser user,
                                           @PathVariable Long applicationId,
                                           @RequestParam(defaultValue = "failed") String scope) {
        int count = evaluationService.cleanRuns(user, applicationId, scope);
        return ApiResponse.ok("已清理 " + count + " 条历史评测任务。");
    }

    /**
     * 获取候选版本的最新质量门禁检测报告（Gate Report）。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId   候选版本 ID
     * @return 质量门禁报告对象
     */
    @Operation(summary = "获取质量门禁报告", description = "检测候选版本是否达到上线标准的评测通过率、防幻觉要求与延迟硬指标。")
    @GetMapping("/{applicationId}/candidates/{candidateId}/gate")
    public ApiResponse<?> gate(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                               @PathVariable Long candidateId) {
        return ApiResponse.ok(gateService.latest(user, applicationId, candidateId));
    }

    /**
     * 为未完全通过门禁的候选版本录入管理员人工发布豁免审批（Override）。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId   候选版本 ID
     * @param request       包含豁免原因与有效期限的请求
     * @return 豁免录入结果
     */
    @Operation(summary = "提交发布门禁豁免", description = "对于未达标但紧急需上线的 Candidate，录入管理员审批原因强制豁免卡点。")
    @PostMapping("/{applicationId}/candidates/{candidateId}/override")
    public ApiResponse<?> override(@AuthenticationPrincipal SecurityUser user,
                                   @PathVariable Long applicationId,
                                   @PathVariable Long candidateId,
                                   @RequestBody GateOverrideRequest request) {
        return ApiResponse.ok("发布豁免已记录。", gateService.override(user, applicationId, candidateId,
                request.reason(), request.scopeJson(), request.expiresAt()));
    }

    /**
     * 正式发布候选版本至线上环境。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param candidateId   候选版本 ID
     * @param request       发布请求体
     * @return 线上正式 Version 对象与发布门禁验证快照
     */
    @Operation(summary = "正式发布候选版本", description = "在通过门禁或取得豁免的前提下，将 Candidate 发布为特定环境的线上正式版本。")
    @PostMapping("/{applicationId}/candidates/{candidateId}/publish")
    public ApiResponse<?> publish(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                  @PathVariable Long candidateId, @RequestBody OrchestrationPublishRequest request) {
        if (request == null || !candidateId.equals(request.candidateId())) {
            throw new IllegalArgumentException("发布请求中的 Candidate 与路径不一致。");
        }
        OrchestrationVersionSummary release = orchestrationService.publish(user, applicationId, request);
        return ApiResponse.ok("应用已发布。", new LifecyclePublishResponse(release, gateService.latest(user, applicationId, candidateId)));
    }

    /**
     * 将线上版本紧急回滚至指定的历史版本。
     *
     * @param user          当前登录用户
     * @param applicationId 应用 ID
     * @param versionId     目标历史版本 ID
     * @param request       包含回滚说明的请求
     * @return 回滚成功响应
     */
    @Operation(summary = "紧急回滚线上版本", description = "把线上 PROD 环境的运行版本回退至之前的指定历史版本。")
    @PostMapping("/{applicationId}/releases/{versionId}/rollback")
    public ApiResponse<?> rollback(@AuthenticationPrincipal SecurityUser user, @PathVariable Long applicationId,
                                   @PathVariable String versionId, @RequestBody(required = false) RollbackRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok("应用 Release 已回滚。", orchestrationService.rollback(user, applicationId,
                new OrchestrationRollbackRequest(versionId, "PRODUCTION", reason)));
    }

    /**
     * 候选版本创建载荷结构
     */
    public record CandidateCreateRequest(Integer expectedRevisionNo, String changeSummary) {
    }

    /**
     * 版本回滚请求载荷结构
     */
    public record RollbackRequest(String reason) {
    }

    /**
     * 门禁豁免申请载荷结构
     */
    public record GateOverrideRequest(String reason, String scopeJson, java.time.LocalDateTime expiresAt) {
    }

    /**
     * 成功发布后的综合响应结构
     */
    public record LifecyclePublishResponse(OrchestrationVersionSummary release,
                                           com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.GateReport gateReport) {
    }
}
