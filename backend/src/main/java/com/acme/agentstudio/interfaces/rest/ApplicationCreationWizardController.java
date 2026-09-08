package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.runtime.ApplicationCreationWizardService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.domain.application.model.ApplicationDraftConfiguration;
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

/**
 * 应用创建向导 REST 控制器。
 * 负责提供新手/高级应用创建向导步骤定义与向导草稿创建接口。
 */
@Tag(name = "应用创建向导", description = "新手和高级应用配置步骤")
@RestController
@RequestMapping("/api/runtime/application-wizard")
public class ApplicationCreationWizardController {

    /** 应用向导业务服务 */
    private final ApplicationCreationWizardService wizardService;

    /**
     * 构造函数注入应用向导服务。
     *
     * @param wizardService 向导服务
     */
    public ApplicationCreationWizardController(ApplicationCreationWizardService wizardService) {
        this.wizardService = wizardService;
    }

    /**
     * 获取应用创建向导的元数据定义与步骤交互表单 Schema。
     *
     * @param advanced 是否为高级向导模式（默认 false）
     * @return 向导定义配置数据
     */
    @Operation(summary = "获取应用创建向导", description = "获取新手模式或高级模式的应用步骤定义与契约规则。")
    @GetMapping
    public ApiResponse<?> definition(@RequestParam(defaultValue = "false") boolean advanced) {
        return ApiResponse.ok(wizardService.definition(advanced));
    }

    /**
     * 根据向导配置创建新的 Runtime 应用草稿及其默认流程图。
     *
     * @param user 当前登录用户
     * @param request 包含应用名称与草稿配置的请求载荷
     * @return 新建应用的摘要信息（包含 AppID 与 AppCode）
     */
    @Operation(summary = "创建 Runtime 应用草稿", description = "按向导步骤提交应用基础元数据，自动生成主工作流草稿与默认入口。")
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user, @RequestBody CreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("应用创建请求不能为空");
        }
        return ApiResponse.ok(wizardService.createDraft(user, request.name(), request.configuration()));
    }

    /**
     * 应用创建向导请求载荷结构。
     *
     * @param name 应用展示名称
     * @param configuration 草稿配置项（输入输出名称、业务目标等）
     */
    public record CreateRequest(String name, ApplicationDraftConfiguration configuration) {
    }
}
