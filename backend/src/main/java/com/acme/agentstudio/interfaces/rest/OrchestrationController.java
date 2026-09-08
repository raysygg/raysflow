package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.MigrationReadOnlyException;
import com.acme.agentstudio.application.workflow.OrchestrationApplicationService;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationDraftRequest;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationPublishRequest;
import com.acme.agentstudio.interfaces.rest.dto.OrchestrationRollbackRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformConversationMessageMapper;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformConversationMessageEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.acme.agentstudio.application.connector.ConnectorResourceService;
import com.acme.agentstudio.application.chat.ConversationExecutionAggregationService;

/**
 * 工作流与 Agent 编排 REST 控制器。
 * 负责提供流程图拓扑绘制草稿保存、图合法性校验、多环境发布、版本回滚、会话关联轨迹以及节点调试等 API 端点。
 */
@Tag(name = "工作流编排", description = "工作流图、节点、发布、执行和回滚管理")
@RestController
@RequestMapping("/api/orchestration")
public class OrchestrationController {

    /** 编排应用核心逻辑服务 */
    private final OrchestrationApplicationService orchestrationService;

    /** 持久化编排执行与轨迹查询服务 */
    private final com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService executionService;

    /** 会话历史消息数据库 Mapper */
    private final PlatformConversationMessageMapper messageMapper;

    /** 流程控制与可恢复任务管理服务 */
    private final com.acme.agentstudio.application.workflow.ExecutionControlService controlService;

    /** 连接器资源服务 */
    private final ConnectorResourceService connectorResourceService;

    /** 会话执行轨迹聚合服务 */
    private final ConversationExecutionAggregationService conversationAggregationService;

    /**
     * 构造函数注入编排控制器所需的依赖项。
     */
    public OrchestrationController(OrchestrationApplicationService orchestrationService,
                                   com.acme.agentstudio.application.workflow.PersistentOrchestrationExecutionService executionService,
                                   PlatformConversationMessageMapper messageMapper,
                                   com.acme.agentstudio.application.workflow.ExecutionControlService controlService,
                                   ConnectorResourceService connectorResourceService,
                                   ConversationExecutionAggregationService conversationAggregationService) {
        this.orchestrationService = orchestrationService;
        this.executionService = executionService;
        this.messageMapper = messageMapper;
        this.controlService = controlService;
        this.connectorResourceService = connectorResourceService;
        this.conversationAggregationService = conversationAggregationService;
    }

    /**
     * 获取可视化画布支持的所有节点类型及属性定义规范。
     *
     * @return 包含节点元数据与 Schema 的响应对象
     */
    @Operation(summary = "获取编排节点类型", description = "获取系统支持的所有可视化工作流节点类型及配置规范。")
    @GetMapping("/node-types")
    public ApiResponse<?> nodeTypes() {
        return ApiResponse.ok(orchestrationService.listNodeTypes());
    }

    /**
     * 查询租户空间下已激活的第三方连接器资源。
     *
     * @param user 当前登录用户
     * @return 可用连接器列表
     */
    @Operation(summary = "获取可用连接器资源", description = "获取当前租户下启用的连接器资源列表。")
    @GetMapping("/resources/connectors")
    public ApiResponse<?> connectors(@AuthenticationPrincipal SecurityUser user) {
        if (user == null || user.getTenantId() == null) {
            throw new IllegalArgumentException("缺少身份信息，请重新登录。");
        }
        return ApiResponse.ok(connectorResourceService.listActive(user.getTenantId()));
    }

    /**
     * 保存或更新工作流/Agent 编排草稿。
     *
     * @param user 当前登录用户
     * @param request 包含画布拓扑 JSON 的草稿保存请求
     * @return 草稿保存处理结果
     */
    @Operation(summary = "保存应用编排草稿", description = "保存或更新编排应用的流程节点草稿。")
    @PostMapping("/drafts")
    public ApiResponse<?> saveDraft(@AuthenticationPrincipal SecurityUser user, @RequestBody OrchestrationDraftRequest request) {
        return ApiResponse.ok("草稿已保存。", orchestrationService.saveDraft(user, request));
    }

    /**
     * 获取应用当前处于编辑状态的最新草稿拓扑。
     *
     * @param user 当前登录用户
     * @param appId 编排应用 ID
     * @return 编排草稿对象
     */
    @Operation(summary = "获取应用编排草稿", description = "获取指定编排应用的最新编辑草稿拓扑。")
    @GetMapping("/apps/{appId}/draft")
    public ApiResponse<?> draft(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        return ApiResponse.ok(orchestrationService.getDraft(user, appId));
    }

    /**
     * 静态校验编排草稿流程图的合法性（检查死循环、孤立节点、端口未连线等）。
     *
     * @param user 当前登录用户
     * @param appId 编排应用 ID
     * @return 图校验结果与警告列表
     */
    @Operation(summary = "检查编排草稿合法性", description = "校验编排草稿节点的未连接端口、缺少参数等异常情况。")
    @GetMapping("/apps/{appId}/checks")
    public ApiResponse<?> checks(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        return ApiResponse.ok(orchestrationService.checkDraft(user, appId));
    }

    /**
     * 将当前草稿发布为指定环境的线上可运行版本。
     *
     * @param user 当前登录用户
     * @param appId 编排应用 ID
     * @param request 发布信息（包含变更日志、期待修订号等）
     * @return 发布成功响应
     */
    @Operation(summary = "发布编排应用版本", description = "将当前草稿发布为特定环境的可用版本。")
    @PostMapping("/apps/{appId}/publish")
    public ApiResponse<?> publish(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                  @RequestBody(required = false) OrchestrationPublishRequest request) {
        return ApiResponse.ok("编排版本已发布。", orchestrationService.publish(user, appId, request));
    }

    /**
     * 将特定环境的在线版本快捷回滚至之前的某次历史版本。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param request 包含回滚目标版本 ID 和原因的请求体
     * @return 回滚成功响应
     */
    @Operation(summary = "回滚编排环境版本", description = "将编排应用在特定环境的版本回滚至之前的历史版本。")
    @PostMapping("/apps/{appId}/rollback")
    public ApiResponse<?> rollback(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                   @RequestBody OrchestrationRollbackRequest request) {
        return ApiResponse.ok("编排环境已回滚。", orchestrationService.rollback(user, appId, request));
    }

    /**
     * 查询编排应用在指定部署环境中的当前运行版本状态。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param environment 部署环境编码（如 PROD）
     * @return 当前活动版本信息
     */
    @Operation(summary = "获取当前生效版本", description = "获取编排应用在指定环境中的当前运行版本信息。")
    @GetMapping("/apps/{appId}/current-version")
    public ApiResponse<?> currentVersion(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                         @RequestParam(required = false) String environment) {
        return ApiResponse.ok(orchestrationService.currentVersion(user, appId, environment));
    }

    /**
     * 提交编排应用执行任务（已停用的旧 API）。
     */
    @Operation(summary = "提交编排应用执行任务", description = "入队并异步执行编排应用实例。")
    @PostMapping("/apps/{appId}/execute")
    public ApiResponse<?> execute(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        throw new MigrationReadOnlyException("旧编排执行入口已停用，请使用 Runtime Run 接口");
    }

    /**
     * 根据执行 ID 查询该次编排运行的任务状态、节点轨迹与输入输出。
     *
     * @param user 当前登录用户
     * @param executionId 唯一执行记录 ID
     * @return 编排任务详情数据
     */
    @Operation(summary = "获取编排执行实例详情", description = "查询具体执行任务的状态、输入输出及节点痕迹。")
    @GetMapping("/executions/{executionId}")
    public ApiResponse<?> execution(@AuthenticationPrincipal SecurityUser user, @PathVariable String executionId) {
        return ApiResponse.ok(executionService.get(user, executionId));
    }

    /**
     * 查询某次对话会话关联的最近一次编排执行实例。
     *
     * @param user 当前登录用户
     * @param conversationId 对话会话 ID
     * @return 最近关联的执行记录
     */
    @Operation(summary = "获取对话关联的最新编排执行", description = "用于恢复历史会话的节点状态和回复。")
    @GetMapping("/executions/conversation/{conversationId}")
    public ApiResponse<?> latestConversationExecution(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId) {
        return ApiResponse.ok(executionService.latestConversationExecution(user, conversationId));
    }

    /**
     * 获取租户空间下的编排执行历史记录列表。
     *
     * @param user 当前登录用户
     * @return 历史任务列表
     */
    @Operation(summary = "获取编排执行列表", description = "获取当前租户下的编排应用执行记录列表。")
    @GetMapping("/executions")
    public ApiResponse<?> executions(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(executionService.list(user));
    }

    /**
     * 按会话 ID 获取详细的消息对话历史记录。
     *
     * @param user 当前登录用户
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @Operation(summary = "获取会话消息历史", description = "按会话 ID 查询对应的对话消息及执行关联数据。")
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<?> conversationMessages(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable String conversationId) {
        return ApiResponse.ok(messageMapper.selectList(new LambdaQueryWrapper<PlatformConversationMessageEntity>()
                .eq(PlatformConversationMessageEntity::getTenantId, user.getTenantId())
                .eq(PlatformConversationMessageEntity::getConversationId, conversationId)
                .orderByAsc(PlatformConversationMessageEntity::getCreatedAt)));
    }

    /**
     * 聚合返回会话的所有消息、上下文、执行状态与节点轨迹数据。
     *
     * @param user 当前登录用户
     * @param conversationId 会话 ID
     * @return 会话全量上下文聚合对象
     */
    @Operation(summary = "恢复会话及关联执行", description = "一次返回会话消息、执行上下文、版本和节点轨迹。")
    @GetMapping("/conversations/{conversationId}/aggregate")
    public ApiResponse<?> conversationAggregate(@AuthenticationPrincipal SecurityUser user,
                                                @PathVariable String conversationId) {
        return ApiResponse.ok(conversationAggregationService.aggregate(user, conversationId));
    }

    /** 认领编排执行任务（已停用的旧 API） */
    @Operation(summary = "认领编排执行任务", description = "工作节点认领或续约正在等待/执行中的编排任务。")
    @PostMapping("/executions/{executionId}/claim")
    public ApiResponse<?> claim(@AuthenticationPrincipal SecurityUser user, @PathVariable String executionId,
                                @RequestParam String worker, @RequestParam(defaultValue = "30") int leaseSeconds) {
        throw new MigrationReadOnlyException("旧编排任务认领入口已停用，请使用 Runtime 运行中心");
    }

    /** 控制编排任务状态（已停用的旧 API） */
    @Operation(summary = "控制编排任务状态", description = "对指定编排任务下发暂停、恢复或取消等控制指令。")
    @PostMapping("/executions/{executionId}/control")
    public ApiResponse<?> control(@AuthenticationPrincipal SecurityUser user, @PathVariable String executionId,
                                  @RequestParam String action) {
        throw new MigrationReadOnlyException("旧编排控制入口已停用，请使用 Runtime Run 控制接口");
    }

    /**
     * 查询租户内可以恢复或断点重新运行的编排任务列表。
     *
     * @param user 当前登录用户
     * @return 可恢复任务列表
     */
    @Operation(summary = "获取可恢复执行列表", description = "获取租户下处于挂起或待恢复状态的编排任务。")
    @GetMapping("/executions/recoverable")
    public ApiResponse<?> recoverable(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(controlService.recoverable(user));
    }

    /** 单节点调试请求载荷结构 */
    public record DebugNodeRequest(String nodeType, JsonNode config, Map<String, Object> input) {}

    /** 单节点调试接口（已停用） */
    @Operation(summary = "单节点单体调试", description = "独立运行调试指定类型的编排节点，验证节点入参出参及算力响应。")
    @PostMapping("/apps/{appId}/debug-node")
    public ApiResponse<?> debugNode(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                    @RequestBody DebugNodeRequest request) {
        throw new MigrationReadOnlyException("旧节点调试入口已停用，请使用流程设计器的 DEBUG Run");
    }
}

