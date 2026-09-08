package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.EntrypointInvocationService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.InvocationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台内部已登录用户通过指定入口（Entrypoint）触发运行 REST 控制器。
 * 负责提供登录用户通过应用端点触发执行流程、传递动态参数输入并生成全新运行实例 RunID 接口。
 */
@Tag(name = "Runtime 内部入口", description = "已登录用户触发端点入口创建运行实例")
@RestController
@RequestMapping("/api/runtime/entrypoints")
public class RuntimeEntrypointController {

    /** 入口触发调用服务 */
    private final EntrypointInvocationService invocationService;

    /**
     * 构造函数注入触发调用服务。
     */
    public RuntimeEntrypointController(EntrypointInvocationService invocationService) {
        this.invocationService = invocationService;
    }

    /**
     * 已登录用户触发指定 Entrypoint 入口创建并启动新的 Run 运行实例。
     *
     * @param user 当前登录用户
     * @param entrypointId 入口 ID
     * @param request 包含幂等号、会话 ID 与参数的调用请求
     * @return 触发响应结果
     */
    @Operation(summary = "内部触发应用入口", description = "登录用户按入口规范触发执行流程并产生实时 Run 实例。")
    @PostMapping("/{entrypointId}/invoke")
    public ApiResponse<?> invoke(@AuthenticationPrincipal SecurityUser user, @PathVariable Long entrypointId,
                                 @RequestBody InvocationRequest request) {
        return ApiResponse.ok(invocationService.invokeInternal(user, entrypointId, request));
    }
}

