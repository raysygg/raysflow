package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.saas.EnterpriseIdentityLifecycleService;
import com.acme.agentstudio.application.connector.ConnectorResourceService;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformCredentialRefEntity;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 企业身份认证与 SCIM 账号同步 REST 控制器。
 * 负责提供 SSO 单点登录配置草稿保存、联调校验、激活/停用、SCIM 规范用户与用户组同步、MFA 多因素认证策略以及紧急越权通道（Emergency Access）授权接口。
 */
@Tag(name = "企业身份治理", description = "SSO 单点登录、SCIM 账号同步、MFA 策略与紧急通道")
@RestController
@RequestMapping("/api/saas/identity")
public class EnterpriseIdentityController {

    /** 可管理企业身份的角色 */
    private static final Set<String> IDENTITY_MANAGER_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    /** 企业身份生命周期服务 */
    private final EnterpriseIdentityLifecycleService service;
    private final com.acme.agentstudio.application.saas.TenantEntitlementService admissionService;
    /** 身份凭证引用服务。 */
    private final ConnectorResourceService credentialService;

    /**
     * 构造函数注入身份生命周期服务。
     *
     * @param service 身份生命周期服务
     */
    public EnterpriseIdentityController(EnterpriseIdentityLifecycleService service,
                                        com.acme.agentstudio.application.saas.TenantEntitlementService admissionService,
                                        ConnectorResourceService credentialService) {
        this.service = service;
        this.admissionService = admissionService;
        this.credentialService = credentialService;
    }

    /**
     * 查询当前租户身份配置的全部版本。
     *
     * @param user 当前登录用户
     * @return 身份配置版本列表
     */
    @GetMapping("/configs")
    public ApiResponse<?> configurations(@AuthenticationPrincipal SecurityUser user) {
        requireManager(user);
        return ApiResponse.ok(service.configurations(user));
    }

    /**
     * 查询身份认证可选择的安全凭证引用。
     */
    @GetMapping("/credentials")
    public ApiResponse<?> credentials(@AuthenticationPrincipal SecurityUser user) {
        requireManager(user);
        return ApiResponse.ok(credentialService.listCredentialReferences(user.getTenantId(), "IDENTITY_SECRET"));
    }

    /**
     * 创建身份认证安全凭证引用，密钥只在保存时接收一次。
     */
    @PostMapping("/credentials")
    public ApiResponse<?> createCredential(@AuthenticationPrincipal SecurityUser user,
                                           @RequestBody CredentialCommand command) {
        requireManager(user);
        PlatformCredentialRefEntity created = credentialService.saveCredential(user.getTenantId(), command.name(),
                "IDENTITY_SECRET", command.secret(), "v1", user.getUserId());
        return ApiResponse.ok("身份凭证已安全保存。", new CredentialView(created.getId(), created.getCredentialName(), created.getStatus()));
    }

    /**
     * 保存或更新企业 SSO 单点登录草稿配置。
     *
     * @param user 当前登录用户
     * @param command SSO 配置指令对象
     * @return 配置结果
     */
    @Operation(summary = "保存 SSO 配置草稿", description = "保存或更新 SAML 2.0 / OIDC 单点登录集成配置草稿。")
    @PostMapping("/drafts")
    public ApiResponse<?> saveDraft(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody EnterpriseIdentityLifecycleService.IdentityConfigCommand command) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "保存身份配置草稿");
        try {
            credentialService.requireCredentialReference(user.getTenantId(), Long.valueOf(command.secretRef()), "IDENTITY_SECRET");
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("请选择有效的身份凭证引用。");
        }
        return ApiResponse.ok(service.saveDraft(user, command));
    }

    /**
     * 测试并校验 SSO 配置与 IdP 服务连通性。
     *
     * @param user 当前登录用户
     * @param configId 配置记录 ID
     * @return 校验报告
     */
    @Operation(summary = "校验 SSO 连通性", description = "测试并验证指定身份配置在 IdP 服务侧的响应连通性。")
    @PostMapping("/{configId}/validate")
    public ApiResponse<?> validate(@AuthenticationPrincipal SecurityUser user, @PathVariable Long configId) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "验证身份配置");
        return ApiResponse.ok(service.validate(user, configId));
    }

    /**
     * 正式启用指定的企业 SSO 单点登录配置。
     *
     * @param user 当前登录用户
     * @param configId 配置 ID
     * @return 操作成功响应
     */
    @Operation(summary = "启用身份配置", description = "正式启用该 Identity Provider (IdP) 单点登录集成。")
    @PostMapping("/{configId}/activate")
    public ApiResponse<?> activate(@AuthenticationPrincipal SecurityUser user, @PathVariable Long configId) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "启用身份配置");
        service.activate(user, configId);
        return ApiResponse.ok("身份配置已启用", null);
    }

    /**
     * 停用指定的企业 SSO 单点登录配置。
     *
     * @param user 当前登录用户
     * @param configId 配置 ID
     * @return 操作成功响应
     */
    @Operation(summary = "停用身份配置", description = "停用该 IdP 接口，恢复为本地或备用登录模式。")
    @PostMapping("/{configId}/disable")
    public ApiResponse<?> disable(@AuthenticationPrincipal SecurityUser user, @PathVariable Long configId) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "停用身份配置");
        service.disable(user, configId);
        return ApiResponse.ok("身份配置已停用", null);
    }

    /**
     * 将历史身份配置恢复为新的待验证草稿。
     *
     * @param user 当前登录用户
     * @param configId 历史配置 ID
     * @return 新草稿版本
     */
    @PostMapping("/{configId}/restore")
    public ApiResponse<?> restore(@AuthenticationPrincipal SecurityUser user, @PathVariable Long configId) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "恢复身份配置");
        return ApiResponse.ok(service.restoreAsDraft(user, configId));
    }

    /**
     * 接收 SCIM 协议的用户新增或更新指令。
     *
     * @param user 当前登录用户
     * @param command SCIM 用户数据
     * @return 同步结果
     */
    @Operation(summary = "SCIM 用户数据同步", description = "按 SCIM 协议同步企业 IdP 推送的用户账号。")
    @PutMapping("/scim/users")
    public ApiResponse<?> scimUser(@AuthenticationPrincipal SecurityUser user,
                                   @RequestBody EnterpriseIdentityLifecycleService.ScimUserCommand command) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "同步企业成员");
        return ApiResponse.ok(service.upsertScimUser(user, command));
    }

    /**
     * 批量执行成员去配置，单条失败时整体事务回滚。
     *
     * @param user 当前登录用户
     * @param commands 待去配置成员列表
     * @return 成员处理结果
     */
    @PostMapping("/scim/users/deprovision")
    public ApiResponse<?> deprovisionUsers(@AuthenticationPrincipal SecurityUser user,
                                           @RequestBody List<EnterpriseIdentityLifecycleService.ScimUserCommand> commands) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "批量去配置成员");
        return ApiResponse.ok(service.deprovisionScimUsers(user, commands));
    }

    /**
     * 接收 SCIM 协议的用户组/部门新增或更新指令。
     *
     * @param user 当前登录用户
     * @param command SCIM 用户组数据
     * @return 同步结果
     */
    @Operation(summary = "SCIM 用户组同步", description = "按 SCIM 协议同步企业 IdP 推送的组织架构与组。")
    @PutMapping("/scim/groups")
    public ApiResponse<?> scimGroup(@AuthenticationPrincipal SecurityUser user,
                                    @RequestBody EnterpriseIdentityLifecycleService.ScimGroupCommand command) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "同步企业用户组");
        return ApiResponse.ok(service.upsertScimGroup(user, command));
    }

    /**
     * 配置或更新租户 MFA 多因素认证策略。
     *
     * @param user 当前登录用户
     * @param command MFA 策略指令
     * @return 保存结果
     */
    @Operation(summary = "保存 MFA 强制策略", description = "配置针对管理员或全员的二次 MFA 强制认证触发规则。")
    @PostMapping("/mfa/policies")
    public ApiResponse<?> mfa(@AuthenticationPrincipal SecurityUser user,
                              @RequestBody EnterpriseIdentityLifecycleService.MfaPolicyCommand command) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "保存强化认证策略");
        return ApiResponse.ok(service.saveMfaPolicy(user, command));
    }

    /**
     * 申请并授权管理员紧急临时越权访问凭证（Emergency Access）。
     *
     * @param user 当前登录用户
     * @param targetUserId 目标被越权用户 ID
     * @param reason 越权审计原因
     * @param minutes 有效时限（分钟，默认 30min）
     * @return 紧急授权凭证信息
     */
    @Operation(summary = "申请紧急越权访问", description = "在故障排查等紧急场景下申请受审计的临时高权访问权限。")
    @PostMapping("/emergency")
    public ApiResponse<?> emergency(@AuthenticationPrincipal SecurityUser user,
                                    @RequestParam Long targetUserId,
                                    @RequestParam String reason,
                                    @RequestParam(defaultValue = "30") int minutes) {
        requireManager(user);
        admissionService.requireAdministrativeWrite(user, "授权紧急访问");
        return ApiResponse.ok(service.grantEmergencyAccess(user, targetUserId, reason, minutes));
    }

    /**
     * 消费使用指定的紧急越权访问凭证。
     *
     * @param user 当前登录用户
     * @param accessId 紧急授权 ID
     * @return 消费响应
     */
    @Operation(summary = "消费紧急越权访问", description = "校验并一次性使用已获批的紧急通道授权。")
    @PostMapping("/emergency/{accessId}/consume")
    public ApiResponse<?> consumeEmergency(@AuthenticationPrincipal SecurityUser user,
                                            @PathVariable Long accessId) {
        return ApiResponse.ok(service.consumeEmergencyAccess(user, accessId));
    }

    /** 校验企业身份管理角色 */
    private void requireManager(SecurityUser user) {
        if (user == null || user.getRoles().stream().noneMatch(IDENTITY_MANAGER_ROLES::contains)) {
            throw new AuthorizationDeniedException("当前账号没有管理企业身份配置的权限。");
        }
    }

    /** 身份凭证创建请求。 */
    public record CredentialCommand(String name, String secret) { }

    /** 不含密文的身份凭证引用。 */
    public record CredentialView(Long id, String name, String status) { }
}

