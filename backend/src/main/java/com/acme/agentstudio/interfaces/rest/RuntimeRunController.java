package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.RuntimeRunApplicationService;
import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.acme.agentstudio.application.workflow.ExecutionControlService;
import com.acme.agentstudio.domain.runtime.model.RuntimeApiContract;
import com.acme.agentstudio.domain.runtime.model.RunControlRequest;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.InvocationRequest;
import com.acme.agentstudio.application.runtime.EntrypointInvocationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
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

import java.util.Map;
import java.time.LocalDateTime;

/**
 * Runtime 运行实例（Run）与节点单步调试 REST 控制器。
 * 负责提供未发布草稿调试测试（Draft Test）、单节点孤立在线调试（Debug Node）、历史 Run 列表、细粒度节点事件检索、原始 Event 审计日志查询以及运行控制（Cancel / Pause / Resume）接口。
 */
@Tag(name = "Runtime Run", description = "运行详情、事件检索、节点调试与运行控制")
@RestController
@RequestMapping("/api/runtime/runs")
public class RuntimeRunController {

    /** 运行实例核心应用服务 */
    private final RuntimeRunApplicationService runService;

    /** 运行控制服务 */
    private final ExecutionControlService controlService;

    /** 审计服务 */
    private final AuditApplicationService auditService;

    /** 入口触发服务 */
    private final EntrypointInvocationService invocationService;

    /**
     * 构造函数注入运行与调试依赖服务。
     */
    public RuntimeRunController(RuntimeRunApplicationService runService,
                                ExecutionControlService controlService,
                                AuditApplicationService auditService,
                                EntrypointInvocationService invocationService) {
        this.runService = runService;
        this.controlService = controlService;
        this.auditService = auditService;
        this.invocationService = invocationService;
    }

    /** 草稿在线测试请求体 */
    public record DraftTestRequest(Long applicationId, String idempotencyKey, String conversationId,
                                   String messageId, Map<String, Object> input) { }

    /** 单节点孤立调试请求体 */
    public record DebugNodeRequest(Long applicationId, String nodeType, JsonNode config,
                                   Map<String, Object> input) {
    }

    /**
     * 针对尚未发布的应用草稿版本（Draft）发起在线调试与运行测试。
     *
     * @param user 当前登录用户
     * @param request 草稿测试请求体
     * @return 运行实例 Trigger 响应结果
     */
    @Operation(summary = "提交草稿版本运行测试", description = "在画布编辑阶段对尚未发布的草稿流程发起模拟在线运行调试。")
    @PostMapping("/draft-test")
    public ApiResponse<?> draftTest(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody DraftTestRequest request) {
        requireUser(user);
        if (request == null || request.applicationId() == null) {
            throw new IllegalArgumentException("测试应用标识不能为空");
        }
        return ApiResponse.ok(invocationService.submitDraftTest(user, request.applicationId(),
                new InvocationRequest(request.idempotencyKey(), request.conversationId(),
                        request.messageId(), request.input())));
    }

    /**
     * 条件分页查询当前用户或租户下的所有历史 Run 运行记录列表。
     *
     * @param user 当前登录用户
     * @param limit 获取条数限制（默认 20）
     * @param offset 起始偏移量（默认 0）
     * @return Run 列表
     */
    @Operation(summary = "获取 Runtime Run 列表", description = "按租户与权限分页拉取历史 Run 实例基础状态信息。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user,
                               @RequestParam(defaultValue = "20") int limit,
                               @RequestParam(defaultValue = "0") int offset) {
        requireUser(user);
        return ApiResponse.ok(runService.list(user, limit, offset));
    }

    /**
     * 按应用、版本、运行 ID、节点 ID、错误码及时间段筛选检索细粒度事件日志。
     *
     * @param user 当前登录用户
     * @param applicationId 应用 ID（可选）
     * @param versionId 版本 ID（可选）
     * @param runId 运行 ID（可选）
     * @param taskId 任务 ID（可选）
     * @param nodeId 节点 ID（可选）
     * @param errorCode 错误码（可选）
     * @param from 开始时间（可选）
     * @param to 结束时间（可选）
     * @return 事件列表
     */
    @Operation(summary = "多维检索运行事件日志", description = "按节点、时间范围与错误码精细查找流转事件。")
    @GetMapping("/events/query")
    public ApiResponse<?> queryEvents(@AuthenticationPrincipal SecurityUser user,
                                      @RequestParam(required = false) Long applicationId,
                                      @RequestParam(required = false) Long versionId,
                                      @RequestParam(required = false) String runId,
                                      @RequestParam(required = false) Long taskId,
                                      @RequestParam(required = false) String nodeId,
                                      @RequestParam(required = false) String errorCode,
                                      @RequestParam(required = false) LocalDateTime from,
                                      @RequestParam(required = false) LocalDateTime to) {
        requireUser(user);
        return ApiResponse.ok(runService.queryEvents(user, applicationId, versionId, runId, taskId,
                nodeId, errorCode, from, to));
    }

    /**
     * 审计权限下读取单条运行事件的原始 payload 详情，并记录审计日志。
     *
     * @param user 当前登录用户
     * @param eventId 事件 ID
     * @return 事件原始数据
     */
    @Operation(summary = "读取事件原始 Payload", description = "具有安全审计权限的用户查看指定事件的未脱敏数据细节。")
    @GetMapping("/events/{eventId}/raw")
    public ApiResponse<?> rawEvent(@AuthenticationPrincipal SecurityUser user, @PathVariable Long eventId) {
        requireUser(user);
        Map<String, Object> result = runService.rawEvent(user, eventId);
        auditService.recordWorkflowAction(user.getTenantId(), user.getUsername(), "RUNTIME_EVENT_RAW_READ",
                eventId, Map.of("eventId", eventId, "runId", String.valueOf(result.get("runId"))));
        return ApiResponse.ok(result);
    }

    /**
     * 对单个 Flow 节点（如 Code/LLM/HTTP 节点）进行脱离大流的在线独立运行调试。
     *
     * @param user 当前登录用户
     * @param request 包含节点类型、配置 JSON 与入参映射的调试请求
     * @return 节点单步计算输出结果与耗时
     */
    @Operation(summary = "调试单个 Runtime 节点", description = "孤立运行单个节点并提供 Mock 输入，快速验证节点逻辑与格式化。")
    @PostMapping("/debug-node")
    public ApiResponse<?> debugNode(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody DebugNodeRequest request) {
        requireUser(user);
        if (request == null) {
            throw new IllegalArgumentException("节点调试请求不能为空");
        }
        return ApiResponse.ok(runService.debugNode(user, request.applicationId(), request.nodeType(),
                request.config(), request.input()));
    }

    /**
     * 获取指定 RunID 的完整聚合详情（包括当前状态、拓扑、耗时与游标以后的 Event 增量）。
     *
     * @param user 当前登录用户
     * @param runId 运行 ID
     * @param afterSequence 增量事件游标位置（默认 0）
     * @return 运行详情聚合对象
     */
    @Operation(summary = "获取 Run 运行聚合详情", description = "获取 Run 状态、DAG 节点拓扑状态及后续增量 Event 事件。")
    @GetMapping("/{runId}")
    public ApiResponse<?> detail(@AuthenticationPrincipal SecurityUser user, @PathVariable String runId,
                                 @RequestParam(defaultValue = "0") long afterSequence) {
        requireUser(user);
        return ApiResponse.ok(runService.detailView(user, runId,
                Math.max(RuntimeApiContract.FIRST_EVENT_CURSOR, afterSequence)));
    }

    /**
     * 发送 Cancel（取消）/ Pause（暂停）/ Resume（恢复）控制指令给正在执行的 Run 实例。
     *
     * @param user 当前登录用户
     * @param runId 运行 ID
     * @param request 包含控制指令类型的请求
     * @return 控制指令应用结果
     */
    @Operation(summary = "发送运行控制指令", description = "对正在运行中的任务下发取消、中断或人工干预恢复指令。")
    @PostMapping("/{runId}/control")
    public ApiResponse<?> control(@AuthenticationPrincipal SecurityUser user, @PathVariable String runId,
                                  @RequestBody RunControlRequest request) {
        requireUser(user);
        return ApiResponse.ok(controlService.control(user, runId, request));
    }

    /**
     * 辅助校验当前操作用户的有效性。
     */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException(RuntimeApiContract.ERROR_INVALID_IDENTITY + ": 当前身份无效");
        }
    }
}

