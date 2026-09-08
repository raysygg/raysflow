package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.application.runtime.RuntimeApplicationConfigurationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.SaveEntrypointRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Runtime 应用运行配置与对外开放端点（Entrypoint）REST 控制器。
 * 负责提供应用提示词 Prompt 变量渲染预览、对外 API 开放端点列表、端点新增与修改，以及端点 Secret 调用密钥安全轮换（Rotate Secret）接口。
 */
@Tag(name = "应用运行配置", description = "提示词预览、开放入口和调用密钥轮换")
@RestController
@RequestMapping("/api/runtime/applications")
public class RuntimeApplicationConfigurationController {

    /** 应用运行配置服务 */
    private final RuntimeApplicationConfigurationService configurationService;

    /** 应用开放端点服务 */
    private final ApplicationEntrypointService entrypointService;

    /**
     * 构造函数注入运行配置与端点服务。
     */
    public RuntimeApplicationConfigurationController(RuntimeApplicationConfigurationService configurationService,
                                                             ApplicationEntrypointService entrypointService) {
        this.configurationService = configurationService;
        this.entrypointService = entrypointService;
    }

    /**
     * 获取指定应用的系统 Prompt、模型绑定与变量 Schema 基础配置。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @return 运行配置数据
     */
    @Operation(summary = "获取应用运行配置", description = "获取指定 Runtime 应用的 Prompt 模板、模型配置与变量 Schema。")
    @GetMapping("/{appId}/configuration")
    public ApiResponse<?> configuration(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        return ApiResponse.ok(configurationService.configuration(requireUser(user), appId));
    }

    /**
     * 传入变量值预览与测试系统 Prompt 的动态替换渲染效果。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param request 包含测试变量 KV 映射的请求体
     * @return 渲染后的 Prompt 字符串结果
     */
    @Operation(summary = "预览 Prompt 渲染效果", description = "填入样例变量测试并预览系统 Prompt 提示词的最终渲染组装文本。")
    @PostMapping("/{appId}/prompt-preview")
    public ApiResponse<?> previewPrompt(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                         @RequestBody PromptPreviewRequest request) {
        return ApiResponse.ok(configurationService.previewPrompt(requireUser(user), appId,
                request == null ? Map.of() : request.input()));
    }

    /**
     * 查询指定应用配置的所有对外开放端点（Entrypoint）列表。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @return 开放端点列表
     */
    @Operation(summary = "获取开放端点列表", description = "查询应用关联的所有 API / Webhook 对外调用端点。")
    @GetMapping("/{appId}/entrypoints")
    public ApiResponse<?> entrypoints(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId) {
        return ApiResponse.ok(entrypointService.list(requireUser(user), appId));
    }

    /**
     * 为应用创建新的 API 或 Webhook 开放端点。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param request 端点保存配置
     * @return 创建结果（包含仅本次展示的一度 API Key Secret）
     */
    @Operation(summary = "创建开放端点", description = "生成全新的对外调用端点与 ApiKey 凭证密钥。")
    @PostMapping("/{appId}/entrypoints")
    public ApiResponse<?> createEntrypoint(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                            @RequestBody SaveEntrypointRequest request) {
        return ApiResponse.ok("应用入口已创建，调用密钥仅在本次响应中展示。",
                entrypointService.create(requireUser(user), appId, request));
    }

    /**
     * 修改已有开放端点的名称、速率限制或启用状态。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param entrypointId 端点 ID
     * @param request 端点配置体
     * @return 更新后的端点信息
     */
    @Operation(summary = "更新开放端点配置", description = "修改端点访问频次限流或禁用状态。")
    @PutMapping("/{appId}/entrypoints/{entrypointId}")
    public ApiResponse<?> updateEntrypoint(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                            @PathVariable Long entrypointId,
                                            @RequestBody SaveEntrypointRequest request) {
        return ApiResponse.ok(entrypointService.update(requireUser(user), appId, entrypointId, request));
    }

    /**
     * 安全轮换（Rotate）指定开放端点的 ApiKey Secret 凭证，废弃老密钥。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param entrypointId 端点 ID
     * @return 包含新生成 Secret 的端点对象
     */
    @Operation(summary = "轮换端点调用密钥", description = "立即废弃旧 ApiKey 密钥并自动重新生成新凭证。")
    @PostMapping("/{appId}/entrypoints/{entrypointId}/rotate-secret")
    public ApiResponse<?> rotateSecret(@AuthenticationPrincipal SecurityUser user, @PathVariable Long appId,
                                        @PathVariable Long entrypointId) {
        return ApiResponse.ok("入口密钥已轮换，旧密钥立即失效。",
                entrypointService.rotateSecret(requireUser(user), appId, entrypointId));
    }

    /** Prompt 渲染预览请求体 */
    public record PromptPreviewRequest(Map<String, Object> input) {
    }

    /**
     * 辅助校验当前操作用户的有效性。
     */
    private SecurityUser requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
        return user;
    }
}

