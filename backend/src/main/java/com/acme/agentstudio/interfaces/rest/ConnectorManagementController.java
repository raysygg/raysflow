package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.connector.ConnectorResourceService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformCredentialRefEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业连接器配置控制器，提供结构化连接器与受保护凭证引用管理接口。
 */
@Tag(name = "连接器管理", description = "企业工具连接器和安全凭证引用管理")
@RestController
@RequestMapping("/api/system/connectors")
public class ConnectorManagementController {

    /** 连接器与凭证资源服务。 */
    private final ConnectorResourceService connectorService;

    /**
     * 注入连接器资源服务。
     */
    public ConnectorManagementController(ConnectorResourceService connectorService) {
        this.connectorService = connectorService;
    }

    /**
     * 查询当前租户的全部连接器配置。
     */
    @Operation(summary = "查询连接器配置")
    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal SecurityUser user) {
        requireManager(user);
        return ApiResponse.ok(connectorService.listManaged(user.getTenantId()));
    }

    /**
     * 查询当前租户可选择的连接器凭证引用。
     */
    @Operation(summary = "查询连接器凭证引用")
    @GetMapping("/credentials")
    public ApiResponse<?> credentials(@AuthenticationPrincipal SecurityUser user) {
        requireManager(user);
        return ApiResponse.ok(connectorService.listCredentialReferences(user.getTenantId()));
    }

    /**
     * 创建连接器凭证引用，响应不返回密钥内容或密文。
     */
    @Operation(summary = "创建连接器凭证引用")
    @PostMapping("/credentials")
    public ApiResponse<?> createCredential(@AuthenticationPrincipal SecurityUser user,
                                           @RequestBody CredentialCommand command) {
        requireManager(user);
        PlatformCredentialRefEntity created = connectorService.saveCredential(user.getTenantId(), command.name(),
                "CONNECTOR_SECRET", command.secret(), "v1", user.getUserId());
        return ApiResponse.ok("连接器凭证已安全保存。",
                new ConnectorResourceService.CredentialSummary(created.getId(), created.getCredentialName(), created.getStatus()));
    }

    /**
     * 创建结构化连接器配置。
     */
    @Operation(summary = "创建连接器")
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user,
                                 @RequestBody ConnectorResourceService.ConnectorCommand command) {
        requireManager(user);
        return ApiResponse.ok("连接器已保存。", connectorService.saveConnector(user.getTenantId(), user.getUserId(), command));
    }

    /**
     * 更新当前租户的连接器配置。
     */
    @Operation(summary = "更新连接器")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id,
                                 @RequestBody ConnectorResourceService.ConnectorCommand command) {
        requireManager(user);
        ConnectorResourceService.ConnectorCommand scoped = new ConnectorResourceService.ConnectorCommand(id,
                command.name(), command.type(), command.endpoint(), command.credentialRefId(), command.timeoutMs(),
                command.retryCount(), command.backoffMs());
        return ApiResponse.ok("连接器已更新。", connectorService.saveConnector(user.getTenantId(), user.getUserId(), scoped));
    }

    /**
     * 删除当前租户的连接器配置。
     */
    @Operation(summary = "删除连接器")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        requireManager(user);
        connectorService.deleteConnector(user.getTenantId(), id);
        return ApiResponse.ok("连接器已删除。", null);
    }

    /**
     * 校验当前用户具备企业资源管理角色。
     */
    private void requireManager(SecurityUser user) {
        if (user == null || !("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()))) {
            throw new IllegalArgumentException("无权限管理企业连接器。");
        }
    }

    /** 创建连接器凭证引用的请求契约。 */
    public record CredentialCommand(String name, String secret) {
    }
}
