package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.ConversationRuntimeService;
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

/**
 * Runtime 交互式对话会话（Conversation）工作台 REST 控制器。
 * 负责提供对话会话新建、历史会话列表查询、会话归档（Archive）、恢复（Restore）、上下文擦除（Clear）、分支衍生分叉（Branch Fork）以及会话 JSON 数据导出接口。
 */
@Tag(name = "Runtime 会话工作台", description = "会话历史、分支衍生和执行轨迹关联")
@RestController
@RequestMapping("/api/runtime/conversations")
public class RuntimeConversationController {

    /** 对话会话运行时服务 */
    private final ConversationRuntimeService conversationService;

    /**
     * 构造函数注入会话运行时服务。
     */
    public RuntimeConversationController(ConversationRuntimeService conversationService) {
        this.conversationService = conversationService;
    }

    /** 会话创建/拉取请求载荷 */
    public record CreateRequest(Long appId, String versionId, String conversationId) {
    }

    /**
     * 创建新对话会话或恢复已有会话实例。
     *
     * @param user 当前登录用户
     * @param request 包含应用 ID 与版本 ID 的创建载荷
     * @return 会话元数据对象
     */
    @Operation(summary = "创建或获取 Runtime 会话", description = "初始化指定 Runtime 应用的在线聊天对话上下文与临时 Memory。")
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user, @RequestBody CreateRequest request) {
        requireUser(user);
        if (request == null || request.appId() == null) {
            throw new IllegalArgumentException("应用标识不能为空");
        }
        return ApiResponse.ok(conversationService.create(user.getTenantId(), request.appId(), request.versionId(),
                user.getUserId(), request.conversationId()));
    }

    /**
     * 获取当前登录用户在租户下的所有历史对话会话列表。
     *
     * @param user 当前登录用户
     * @return 会话历史列表
     */
    @Operation(summary = "获取 Runtime 会话列表", description = "获取当前用户创建的历史会话记录及其最后活跃时间。")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        requireUser(user);
        return ApiResponse.ok(conversationService.list(user.getTenantId(), user.getUserId()));
    }

    /**
     * 将指定的对话会话置为归档状态。
     *
     * @param user 当前登录用户
     * @param conversationId 会话全局编码 ID
     * @return 归档成功响应
     */
    @Operation(summary = "归档指定会话", description = "把历史会话移入归档夹，不再占用主菜单列表空间。")
    @PostMapping("/{conversationId}/archive")
    public ApiResponse<?> archive(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId) {
        requireUser(user);
        return ApiResponse.ok(conversationService.archive(user.getTenantId(), user.getUserId(), conversationId));
    }

    /**
     * 清空指定会话的上下文 Token 历史，重新开始对话交互。
     *
     * @param user 当前登录用户
     * @param conversationId 会话全局编码 ID
     * @return 上下文清空响应
     */
    @Operation(summary = "清空会话上下文", description = "重置会话中的记忆与消息上下文，历史 Run 实例仍保留在运行中心。")
    @PostMapping("/{conversationId}/clear")
    public ApiResponse<?> clear(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId) {
        requireUser(user);
        return ApiResponse.ok(conversationService.clearContext(user.getTenantId(), user.getUserId(), conversationId));
    }

    /**
     * 从归档状态恢复对话会话。
     *
     * @param user 当前登录用户
     * @param conversationId 会话全局编码 ID
     * @return 恢复成功响应
     */
    @Operation(summary = "恢复已归档会话", description = "将已归档的会话重新恢复到活跃会话列表中。")
    @PostMapping("/{conversationId}/restore")
    public ApiResponse<?> restore(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId) {
        requireUser(user);
        return ApiResponse.ok(conversationService.restore(user.getTenantId(), user.getUserId(), conversationId));
    }

    /**
     * 从指定会话复制建立新的分支会话（Branch Fork），实现平行会话分支探索。
     *
     * @param user 当前登录用户
     * @param conversationId 源会话全局编码 ID
     * @param newConversationId 新分支会话编码 ID（可选）
     * @return 分支建立成功响应
     */
    @Operation(summary = "创建会话分支", description = "复制当前会话的上下文消息快照，衍生出独立的新分支对话。")
    @PostMapping("/{conversationId}/branch")
    public ApiResponse<?> branch(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId,
                                 @RequestParam(required = false) String newConversationId) {
        requireUser(user);
        return ApiResponse.ok(conversationService.branch(user.getTenantId(), user.getUserId(), conversationId,
                newConversationId));
    }

    /**
     * 导出完整会话记录（包含每轮 Query、出参与命中节点分析数据）。
     *
     * @param user 当前登录用户
     * @param conversationId 会话全局编码 ID
     * @return 导出的完整会话 JSON 载荷
     */
    @Operation(summary = "导出会话记录", description = "导出完整会话的消息历史、运行 Trace 与相关评估结果。")
    @GetMapping("/{conversationId}/export")
    public ApiResponse<?> export(@AuthenticationPrincipal SecurityUser user, @PathVariable String conversationId) {
        requireUser(user);
        return ApiResponse.ok(conversationService.export(user.getTenantId(), user.getUserId(), conversationId));
    }

    /**
     * 辅助校验当前操作用户的有效性。
     */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
    }
}

