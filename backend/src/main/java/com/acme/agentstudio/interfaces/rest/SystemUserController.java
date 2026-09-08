package com.acme.agentstudio.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.AuditLogEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysApiKeyEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysComplianceRuleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelRouterRuleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantBillingQuotaEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AuditLogMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysApiKeyMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysComplianceRuleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelRouterRuleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantBillingQuotaMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantMapper;
import com.acme.agentstudio.infrastructure.model.ChatModelRegistry;
import com.acme.agentstudio.infrastructure.rag.model.OpenAiCompatibleEmbeddingProvider;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.model.ModelCapability;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.application.model.ModelTestConnectionService;
import com.acme.agentstudio.application.model.ModelTestConnectionService.TestResult;
import com.acme.agentstudio.application.model.ModelCredentialService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 系统后台与多租户平台综合管理 REST 控制器。
 * 负责提供多租户系统用户账号 CRUD、角色权限绑定、AI 大模型底座 Endpoint/ApiKey 配置与探针连通性测试、安全护栏合规规则、智能模型路由规则、开发者 API 密钥管理、超级管理员全平台租户及配额管控、审计日志与计费看板接口。
 */
@Tag(name = "系统用户", description = "系统用户、模型底座、合规规则与平台管理")
@RestController
@RequestMapping("/api/system")
public class SystemUserController {

    /** 系统级别共享公共租户 ID 标识 */
    private static final Long SYSTEM_TENANT_ID = 1L;

    /** 超级管理员角色标识 */
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /** 租户管理员角色标识 */
    private static final String ROLE_ADMIN = "ADMIN";

    /** 系统用户 Mapper */
    private final SysUserMapper sysUserMapper;

    /** 系统角色 Mapper */
    private final SysRoleMapper sysRoleMapper;

    /** 用户角色关联 Mapper */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** 模型底座配置 Mapper */
    private final SysModelConfigMapper sysModelConfigMapper;

    /** 智能模型路由规则 Mapper */
    private final SysModelRouterRuleMapper sysModelRouterRuleMapper;

    /** 内容安全合规规则 Mapper */
    private final SysComplianceRuleMapper sysComplianceRuleMapper;

    /** 开发者 APIKey 密钥 Mapper */
    private final SysApiKeyMapper sysApiKeyMapper;

    /** 租户实体 Mapper */
    private final TenantMapper tenantMapper;

    /** 租户计费配额 Mapper */
    private final TenantBillingQuotaMapper tenantBillingQuotaMapper;

    /** 审计日志 Mapper */
    private final AuditLogMapper auditLogMapper;

    /** BCrypt 密码加密器 */
    private final PasswordEncoder passwordEncoder;

    /** 聊天的模型注册表与客户端缓存 */
    private final ChatModelRegistry chatModelRegistry;

    /** Embedding 向量模型 Provider */
    private final OpenAiCompatibleEmbeddingProvider embeddingProvider;

    /** 模型凭证引用服务 */
    private final ModelCredentialService modelCredentialService;

    /** 模型在线探针连通性测试服务 */
    private final ModelTestConnectionService modelTestConnectionService;

    /**
     * 构造函数注入系统设置所需的一切核心 Mapper 与应用依赖组件。
     */
    public SystemUserController(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysModelConfigMapper sysModelConfigMapper,
            SysModelRouterRuleMapper sysModelRouterRuleMapper,
            SysComplianceRuleMapper sysComplianceRuleMapper,
            SysApiKeyMapper sysApiKeyMapper,
            TenantMapper tenantMapper,
            TenantBillingQuotaMapper tenantBillingQuotaMapper,
            AuditLogMapper auditLogMapper,
            PasswordEncoder passwordEncoder,
            ChatModelRegistry chatModelRegistry,
            OpenAiCompatibleEmbeddingProvider embeddingProvider,
            ModelCredentialService modelCredentialService,
            ModelTestConnectionService modelTestConnectionService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysModelConfigMapper = sysModelConfigMapper;
        this.sysModelRouterRuleMapper = sysModelRouterRuleMapper;
        this.sysComplianceRuleMapper = sysComplianceRuleMapper;
        this.sysApiKeyMapper = sysApiKeyMapper;
        this.tenantMapper = tenantMapper;
        this.tenantBillingQuotaMapper = tenantBillingQuotaMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.chatModelRegistry = chatModelRegistry;
        this.embeddingProvider = embeddingProvider;
        this.modelCredentialService = modelCredentialService;
        this.modelTestConnectionService = modelTestConnectionService;
    }

    /**
     * 从当前 Spring Security 上下文中安全提取登录用户信息。
     */
    private SecurityUser getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser;
        }
        throw new IllegalStateException("用户未登录或鉴权失效。");
    }

    // ==========================================
    // 1. 用户管理
    // ==========================================

    /**
     * 按当前登录用户的租户维度获取包含角色信息的账号列表。
     *
     * @return 包含账号基础属性与角色编码的用户视图列表
     */
    @Operation(summary = "获取系统用户列表", description = "按当前登录用户的租户维度获取包含角色的用户列表。")
    @GetMapping("/users")
    public ApiResponse<?> listUsers() {
        SecurityUser user = getAuthenticatedUser();
        List<SysUserEntity> users = sysUserMapper.selectList(
                new QueryWrapper<SysUserEntity>().eq("tenant_id", user.getTenantId())
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUserEntity u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("status", u.getStatus());
            map.put("createdAt", u.getCreatedAt());

            SysUserRoleEntity ur = sysUserRoleMapper.selectOne(new QueryWrapper<SysUserRoleEntity>().eq("user_id", u.getId()));
            if (ur != null) {
                SysRoleEntity role = sysRoleMapper.selectById(ur.getRoleId());
                if (role != null) {
                    map.put("role", role.getRoleCode());
                    map.put("roleName", role.getRoleName());
                }
            }
            result.add(map);
        }
        return ApiResponse.ok(result);
    }

    /** 创建系统用户请求体 */
    public record CreateUserRequest(String username, String password, String nickname, String email, String phone, String role) {}

    /**
     * 在当前租户下创建新用户并分配指定初始角色。
     *
     * @param request 用户创建配置
     * @return 结果响应
     */
    @Operation(summary = "创建系统用户", description = "在当前租户下创建新用户并分配相应角色。")
    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody CreateUserRequest request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限执行此操作。");
        }

        if (request.username() == null || request.username().trim().isEmpty() ||
            request.password() == null || request.password().trim().isEmpty()) {
            return ApiResponse.fail("用户名和密码不能为空。");
        }

        Long count = sysUserMapper.selectCount(
                new QueryWrapper<SysUserEntity>()
                        .eq("tenant_id", currentUser.getTenantId())
                        .eq("username", request.username())
        );
        if (count > 0) {
            return ApiResponse.fail("用户名在当前租户下已存在。");
        }

        SysUserEntity user = new SysUserEntity();
        user.setTenantId(currentUser.getTenantId());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);

        SysRoleEntity targetRole = sysRoleMapper.selectOne(
                new QueryWrapper<SysRoleEntity>()
                        .eq("tenant_id", currentUser.getTenantId())
                        .eq("role_code", request.role() == null ? "STAFF" : request.role())
        );
        if (targetRole != null) {
            SysUserRoleEntity ur = new SysUserRoleEntity();
            ur.setUserId(user.getId());
            ur.setRoleId(targetRole.getId());
            sysUserRoleMapper.insert(ur);
        }

        return ApiResponse.ok("用户创建成功", null);
    }

    /**
     * 修改指定用户的启用/禁用状态。
     *
     * @param id 用户 ID
     * @param status 新状态
     * @return 变更结果
     */
    @Operation(summary = "修改用户状态", description = "启用或冻结当前租户下的指定系统用户。")
    @PutMapping("/users/{id}/status")
    public ApiResponse<?> changeUserStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限。");
        }
        SysUserEntity user = sysUserMapper.selectById(id);
        if (user == null || !user.getTenantId().equals(currentUser.getTenantId())) {
            return ApiResponse.fail("用户不存在。");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ApiResponse.ok("修改成功", null);
    }

    // ==========================================
    // 2. 模型底座配置管理
    // ==========================================

    /**
     * 获取租户可用的模型底座及公共共享模型列表（API 凭证密文自动脱敏处理）。
     *
     * @param includeDisabled 是否包含已放停用的配置
     * @return 模型配置列表
     */
    @Operation(summary = "获取模型配置列表", description = "普通用户读取可用模型，管理员额外读取待配置的停用模板。")
    @GetMapping("/models")
    public ApiResponse<?> listModels(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        SecurityUser currentUser = getAuthenticatedUser();
        List<SysModelConfigEntity> models;
        if (ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
            QueryWrapper<SysModelConfigEntity> query = new QueryWrapper<SysModelConfigEntity>().orderByAsc("id");
            if (!includeDisabled) {
                query.eq("status", BusinessStatus.ACTIVE);
            }
            models = sysModelConfigMapper.selectList(query);
        } else {
            boolean includeManagedModels = includeDisabled && isModelManager(currentUser);
            models = new ArrayList<>(queryModels(currentUser.getTenantId(), includeManagedModels));
            if (!SYSTEM_TENANT_ID.equals(currentUser.getTenantId())) {
                models.addAll(queryModels(SYSTEM_TENANT_ID, false));
            }
        }

        for (SysModelConfigEntity m : models) {
            m.setCredentialConfigured(m.getCredentialRefId() != null);
        }
        return ApiResponse.ok(models);
    }

    /**
     * 查询当前租户可用于模型连接的受保护凭证引用。
     */
    @Operation(summary = "获取模型凭证引用", description = "仅返回凭证名称、状态和引用标识，不返回密钥内容。")
    @GetMapping("/model-credentials")
    public ApiResponse<?> listModelCredentials() {
        SecurityUser currentUser = getAuthenticatedUser();
        return ApiResponse.ok(modelCredentialService.list(modelManagementTenant(currentUser)));
    }

    /**
     * 创建模型凭证引用，密钥内容加密后不再通过接口返回。
     */
    @Operation(summary = "创建模型凭证引用", description = "安全保存模型密钥并只返回可选择的凭证引用。")
    @PostMapping("/model-credentials")
    public ApiResponse<?> createModelCredential(@RequestBody CreateCredentialRequest request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!isModelManager(currentUser)) {
            return ApiResponse.fail("无权限创建模型凭证。");
        }
        return ApiResponse.ok("模型凭证已安全保存。", modelCredentialService.create(
                modelManagementTenant(currentUser), currentUser.getUserId(), request.name(), request.secret()));
    }

    /**
     * 更新已有模型凭证引用名称或轮换密钥，新密钥加密后不再通过接口返回。
     *
     * @param id 凭证标识
     * @param request 包含名称或新密钥的更新契约
     * @return 更新后的凭证引用信息
     */
    @Operation(summary = "更新或轮换模型凭证", description = "支持重命名或轮换更新模型密钥。")
    @PutMapping("/model-credentials/{id}")
    public ApiResponse<?> updateModelCredential(@PathVariable Long id, @RequestBody UpdateCredentialRequest request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!isModelManager(currentUser)) {
            return ApiResponse.fail("无权限修改模型凭证。");
        }
        return ApiResponse.ok("模型凭证已成功更新。", modelCredentialService.update(
                modelManagementTenant(currentUser), id, request.name(), request.secret()));
    }

    /**
     * 安全删除未被模型引用的凭证。
     *
     * @param id 凭证标识
     * @return 删除结果
     */
    @Operation(summary = "删除模型凭证", description = "仅允许删除未被任何模型引用的安全凭证。")
    @DeleteMapping("/model-credentials/{id}")
    public ApiResponse<?> deleteModelCredential(@PathVariable Long id) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!isModelManager(currentUser)) {
            return ApiResponse.fail("无权限删除模型凭证。");
        }
        modelCredentialService.delete(modelManagementTenant(currentUser), id);
        return ApiResponse.ok("模型凭证已安全删除。", null);
    }

    /**
     * 添加全新的模型配置，并绑定已有的受保护凭证引用。
     *
     * @param model 模型配置实体
     * @return 成功响应
     */
    @Operation(summary = "添加模型配置", description = "录入新的 AI 模型服务并绑定受保护凭证引用。")
    @PostMapping("/models")
    public ApiResponse<?> createModel(@RequestBody SysModelConfigEntity model) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!ROLE_SUPER_ADMIN.equals(currentUser.getRole()) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限配置模型底座。");
        }

        if (model.getModelKey() == null || model.getModelKey().isBlank()
                || model.getModelName() == null || model.getModelName().isBlank()
                || model.getProvider() == null || model.getProvider().isBlank()
                || model.getCredentialRefId() == null
                || model.getBaseUrl() == null || model.getBaseUrl().isBlank()) {
            return ApiResponse.fail("模型编码、显示名称、供应商、接口地址和凭证引用均不能为空。");
        }
        if (!normalizeModelCapability(model)) {
            return ApiResponse.fail("模型能力只能填写 CHAT、EMBEDDING 或 RERANKER。");
        }
        if (ModelCapability.EMBEDDING.name().equals(model.getModelCapability())
                && (model.getVectorDimension() == null || model.getVectorDimension() <= 0)) {
            return ApiResponse.fail("Embedding 模型必须配置大于 0 的向量维度。");
        }

        if (ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
            model.setTenantId(SYSTEM_TENANT_ID);
        } else {
            model.setTenantId(currentUser.getTenantId());
        }
        modelCredentialService.requireAvailable(model.getTenantId(), model.getCredentialRefId());

        Long duplicateCount = sysModelConfigMapper.selectCount(
                new QueryWrapper<SysModelConfigEntity>()
                        .eq("tenant_id", model.getTenantId())
                        .eq("model_key", model.getModelKey().trim())
        );
        if (duplicateCount > 0) {
            return ApiResponse.fail("当前作用范围内已存在相同模型编码。");
        }

        if (model.getUpstreamModelName() != null && !model.getUpstreamModelName().isBlank()) {
            model.setUpstreamModelName(model.getUpstreamModelName().trim());
        } else {
            model.setUpstreamModelName(null);
        }
        model.setStatus(BusinessStatus.ACTIVE);
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());

        sysModelConfigMapper.insert(model);
        return ApiResponse.ok("模型底座配置已成功录入。", null);
    }

    /**
     * 在线探针发起网络 HTTP 请求测试大模型 Endpoint 与凭证的真实连通性。
     *
     * @param model 待测试的模型参数结构
     * @return 探针测试结果（包含网络耗时与状态说明）
     */
    @Operation(summary = "测试模型连通性", description = "在保存或配置模型时在线发起探针测试，返回连通状态与耗时。")
    @PostMapping("/models/test-connection")
    public ApiResponse<?> testModelConnection(@RequestBody SysModelConfigEntity model) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!ROLE_SUPER_ADMIN.equals(currentUser.getRole()) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限测试模型连接。");
        }
        if (model.getModelKey() == null || model.getModelKey().isBlank()
                || model.getProvider() == null || model.getProvider().isBlank()
                || model.getBaseUrl() == null || model.getBaseUrl().isBlank()
                || model.getModelCapability() == null || model.getModelCapability().isBlank()) {
            return ApiResponse.fail("模型编码、供应商、模型能力和接口地址不能为空。");
        }

        Long credentialTenantId = modelManagementTenant(currentUser);
        Long credentialRefId = model.getCredentialRefId();
        if (model.getId() != null) {
            SysModelConfigEntity existing = sysModelConfigMapper.selectById(model.getId());
            if (existing == null || !canManageModel(currentUser, existing)) {
                return ApiResponse.fail("模型不存在或无权执行探针测试。");
            }
            credentialTenantId = existing.getTenantId();
            credentialRefId = existing.getCredentialRefId();
        }
        String apiKey = modelCredentialService.resolveSecret(credentialTenantId, credentialRefId);

        ModelProvider provider = ModelProvider.fromCode(model.getProvider());
        ModelCapability capability;
        try {
            capability = ModelCapability.valueOf(model.getModelCapability().trim());
        } catch (Exception e) {
            return ApiResponse.fail("模型能力无效，可选值：CHAT、EMBEDDING、RERANKER。");
        }

        TestResult result = modelTestConnectionService.testConnection(provider, capability, model.getBaseUrl(),
                apiKey, model.resolveUpstreamModelName(), model.getVectorDimension());

        return ApiResponse.ok(result);
    }

    /**
     * 更新已有模型的名称、凭证引用、接口地址或维度设置，并清除注册表实例缓存。
     *
     * @param id 模型 ID
     * @param request 包含修改信息的实体
     * @return 成功响应
     */
    @Operation(summary = "更新模型配置", description = "更新已有模型的访问地址、显示名称或凭证引用。")
    @PutMapping("/models/{id}")
    public ApiResponse<?> updateModel(@PathVariable Long id, @RequestBody SysModelConfigEntity request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!ROLE_SUPER_ADMIN.equals(currentUser.getRole()) && !ROLE_ADMIN.equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限修改模型底座。");
        }
        SysModelConfigEntity existing = sysModelConfigMapper.selectById(id);
        if (existing == null || !canManageModel(currentUser, existing)) {
            return ApiResponse.fail("模型不存在或无权修改。");
        }
        if (request.getModelKey() == null || request.getModelKey().isBlank()
                || request.getModelName() == null || request.getModelName().isBlank()
                || request.getProvider() == null || request.getProvider().isBlank()
                || request.getBaseUrl() == null || request.getBaseUrl().isBlank()) {
            return ApiResponse.fail("供应商模型名、显示名称、供应商和接口地址不能为空。");
        }
        if (!normalizeModelCapability(request)) {
            return ApiResponse.fail("模型能力只能填写 CHAT、EMBEDDING 或 RERANKER。");
        }
        if (ModelCapability.EMBEDDING.name().equals(request.getModelCapability())
                && (request.getVectorDimension() == null || request.getVectorDimension() <= 0)) {
            return ApiResponse.fail("Embedding 模型必须配置大于 0 的向量维度。");
        }
        if (request.getCredentialRefId() == null) {
            return ApiResponse.fail("启用模型前必须选择有效的凭证引用。");
        }
        modelCredentialService.requireAvailable(existing.getTenantId(), request.getCredentialRefId());
        Long duplicateCount = sysModelConfigMapper.selectCount(new QueryWrapper<SysModelConfigEntity>()
                .eq("tenant_id", existing.getTenantId())
                .eq("model_key", request.getModelKey().trim())
                .ne("id", id));
        if (duplicateCount > 0) {
            return ApiResponse.fail("当前作用范围内已存在相同供应商模型名。");
        }
        String previousModelKey = existing.getModelKey();
        existing.setModelKey(request.getModelKey().trim());
        existing.setUpstreamModelName(request.getUpstreamModelName() != null && !request.getUpstreamModelName().isBlank()
                ? request.getUpstreamModelName().trim() : null);
        existing.setModelName(request.getModelName().trim());
        existing.setProvider(request.getProvider().trim());
        existing.setModelCapability(request.getModelCapability());
        existing.setVectorDimension(request.getVectorDimension());
        existing.setBaseUrl(request.getBaseUrl());
        existing.setCredentialRefId(request.getCredentialRefId());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existing.setStatus(request.getStatus().trim());
        } else {
            existing.setStatus(BusinessStatus.ACTIVE);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        sysModelConfigMapper.updateById(existing);
        chatModelRegistry.clearCache(existing.getTenantId(), previousModelKey);
        chatModelRegistry.clearCache(existing.getTenantId(), existing.getModelKey());
        embeddingProvider.clearRemoteModel(existing.getId());
        return ApiResponse.ok("模型底座配置已更新。", null);
    }

    /**
     * 删除当前租户自有的模型底座配置。
     *
     * @param id 模型配置 ID
     * @return 删除响应
     */
    @Operation(summary = "删除模型配置", description = "删除当前租户自有的模型底座配置。")
    @DeleteMapping("/models/{id}")
    public ApiResponse<?> deleteModel(@PathVariable Long id) {
        SecurityUser currentUser = getAuthenticatedUser();
        SysModelConfigEntity model = sysModelConfigMapper.selectById(id);
        if (model == null) {
            return ApiResponse.fail("模型不存在。");
        }

        if (ROLE_SUPER_ADMIN.equals(currentUser.getRole()) || model.getTenantId().equals(currentUser.getTenantId())) {
            sysModelConfigMapper.deleteById(id);
            chatModelRegistry.clearCache(model.getTenantId(), model.getModelKey());
            embeddingProvider.clearRemoteModel(model.getId());
            return ApiResponse.ok("模型已成功删除。", null);
        }
        return ApiResponse.fail("无权限删除此模型。");
    }

    /**
     * 辅助方法：校验用户是否有权编辑管理该模型配置。
     */
    private boolean canManageModel(SecurityUser currentUser, SysModelConfigEntity model) {
        if (ROLE_SUPER_ADMIN.equals(currentUser.getRole())) {
            return true;
        }
        return model.getTenantId().equals(currentUser.getTenantId());
    }

    /**
     * 辅助方法：查询租户下的模型配置列表。
     */
    private List<SysModelConfigEntity> queryModels(Long tenantId, boolean includeDisabled) {
        QueryWrapper<SysModelConfigEntity> query = new QueryWrapper<SysModelConfigEntity>()
                .eq("tenant_id", tenantId)
                .orderByAsc("id");
        if (!includeDisabled) {
            query.eq("status", BusinessStatus.ACTIVE);
        }
        return sysModelConfigMapper.selectList(query);
    }

    /**
     * 辅助方法：判断用户是否为管理员。
     */
    private boolean isModelManager(SecurityUser user) {
        return ROLE_SUPER_ADMIN.equals(user.getRole()) || ROLE_ADMIN.equals(user.getRole());
    }

    /**
     * 确定当前管理员创建模型和凭证时使用的事实租户范围。
     */
    private Long modelManagementTenant(SecurityUser user) {
        return ROLE_SUPER_ADMIN.equals(user.getRole()) ? SYSTEM_TENANT_ID : user.getTenantId();
    }

    /**
     * 辅助方法：规范化并校验模型能力枚举字符。
     */
    private boolean normalizeModelCapability(SysModelConfigEntity model) {
        if (model.getModelCapability() == null || model.getModelCapability().isBlank()) {
            model.setModelCapability(ModelCapability.CHAT.name());
            return true;
        }
        try {
            model.setModelCapability(ModelCapability.valueOf(model.getModelCapability().toUpperCase()).name());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    // ==========================================
    // 3. 安全合规配置 (Compliance Guardrails)
    // ==========================================

    /**
     * 获取租户下配置的内容安全检测与合规护栏规则列表。
     *
     * @return 合规规则列表
     */
    @Operation(summary = "获取安全合规规则列表", description = "获取租户下配置的安全内容护栏与审计规则。")
    @GetMapping("/compliance")
    public ApiResponse<?> listComplianceRules() {
        SecurityUser currentUser = getAuthenticatedUser();
        List<SysComplianceRuleEntity> rules = sysComplianceRuleMapper.selectList(
                new QueryWrapper<SysComplianceRuleEntity>().eq("tenant_id", currentUser.getTenantId())
        );
        return ApiResponse.ok(rules);
    }

    /**
     * 新增针对输入 Prompt 或输出 LLM 回复的内容安全防爆敏感词过滤规则。
     *
     * @param rule 规则对象
     * @return 成功响应
     */
    @Operation(summary = "创建安全合规规则", description = "添加针对输入输出文本的内容安全检测规则。")
    @PostMapping("/compliance")
    public ApiResponse<?> createComplianceRule(@RequestBody SysComplianceRuleEntity rule) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("无权限。");
        }
        rule.setTenantId(currentUser.getTenantId());
        rule.setStatus("ACTIVE");
        rule.setCreatedAt(LocalDateTime.now());
        sysComplianceRuleMapper.insert(rule);
        return ApiResponse.ok("合规规则创建成功。", null);
    }

    /**
     * 删除指定的安全合规规则。
     *
     * @param id 规则 ID
     * @return 成功响应
     */
    @Operation(summary = "删除安全合规规则", description = "删除指定的安全合规护栏规则。")
    @DeleteMapping("/compliance/{id}")
    public ApiResponse<?> deleteComplianceRule(@PathVariable Long id) {
        SecurityUser currentUser = getAuthenticatedUser();
        SysComplianceRuleEntity rule = sysComplianceRuleMapper.selectById(id);
        if (rule == null || !rule.getTenantId().equals(currentUser.getTenantId())) {
            return ApiResponse.fail("规则不存在。");
        }
        sysComplianceRuleMapper.deleteById(id);
        return ApiResponse.ok("规则已删除。", null);
    }

    // ==========================================
    // 4. 智能路由配置 (Smart Routing)
    // ==========================================

    /**
     * 获取租户配置的智能大模型路由策略规则列表。
     *
     * @return 路由规则列表
     */
    @Operation(summary = "获取智能模型路由规则", description = "获取当前租户配置的模型动态路由规则。")
    @GetMapping("/routers")
    public ApiResponse<?> listRouterRules() {
        SecurityUser currentUser = getAuthenticatedUser();
        List<SysModelRouterRuleEntity> rules = sysModelRouterRuleMapper.selectList(
                new QueryWrapper<SysModelRouterRuleEntity>().eq("tenant_id", currentUser.getTenantId())
        );
        return ApiResponse.ok(rules.stream().map(rule -> new RoutingRuleView(
                rule.getId(), rule.getRuleName(), rule.getPrimaryModelKey(), rule.getBackupModelKey(),
                rule.getStatus(), "按请求内容匹配", rule.getCreatedAt())).toList());
    }

    /**
     * 新增基于 Token 长度或优先级的智能模型分流路由规则。
     *
     * @param request 结构化路由规则请求
     * @return 成功响应
     */
    @Operation(summary = "创建智能模型路由规则", description = "配置基于条件选择目标大模型底座的路由规则。")
    @PostMapping("/routers")
    public ApiResponse<?> createRouterRule(@RequestBody RoutingRuleRequest request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!isModelManager(currentUser)) {
            return ApiResponse.fail("无权限配置智能路由。");
        }
        if (request.ruleName() == null || request.ruleName().isBlank()) {
            return ApiResponse.fail("请填写路由规则名称。");
        }
        if (!isAvailableModelKey(currentUser.getTenantId(), request.primaryModelKey())) {
            return ApiResponse.fail("请选择当前企业可用的主模型。");
        }
        if (request.backupModelKey() != null && !request.backupModelKey().isBlank()
                && !isAvailableModelKey(currentUser.getTenantId(), request.backupModelKey())) {
            return ApiResponse.fail("请选择当前企业可用的备用模型。");
        }
        SysModelRouterRuleEntity rule = new SysModelRouterRuleEntity();
        rule.setTenantId(currentUser.getTenantId());
        rule.setRuleName(request.ruleName().trim());
        rule.setPatternRegex(compileRoutingCondition(request.conditions()));
        rule.setPrimaryModelKey(request.primaryModelKey());
        rule.setBackupModelKey(request.backupModelKey());
        rule.setStatus(BusinessStatus.ACTIVE);
        rule.setCreatedAt(LocalDateTime.now());
        sysModelRouterRuleMapper.insert(rule);
        return ApiResponse.ok("路由规则创建成功。", null);
    }

    /**
     * 将单条结构化文本条件编译为内部正则，正则不进入产品表单或接口响应。
     */
    private String compileRoutingCondition(List<RoutingCondition> conditions) {
        if (conditions == null || conditions.size() != 1) {
            throw new IllegalArgumentException("每条路由规则需要配置一个请求内容条件。");
        }
        RoutingCondition condition = conditions.get(0);
        if (!"requestText".equals(condition.field()) || condition.value() == null || condition.value().isBlank()) {
            throw new IllegalArgumentException("请选择请求内容并填写比较值。");
        }
        String quoted = Pattern.quote(condition.value().trim());
        return switch (condition.operator()) {
            case "equals" -> "^" + quoted + "$";
            case "contains" -> quoted;
            default -> throw new IllegalArgumentException("路由条件只支持等于或包含。");
        };
    }

    /**
     * 校验模型编码属于当前租户或平台共享范围且处于启用状态。
     */
    private boolean isAvailableModelKey(Long tenantId, String modelKey) {
        if (modelKey == null || modelKey.isBlank()) {
            return false;
        }
        List<Long> tenantScope = SYSTEM_TENANT_ID.equals(tenantId)
                ? List.of(SYSTEM_TENANT_ID) : List.of(tenantId, SYSTEM_TENANT_ID);
        return sysModelConfigMapper.selectCount(new QueryWrapper<SysModelConfigEntity>()
                .in("tenant_id", tenantScope)
                .eq("model_key", modelKey)
                .eq("status", BusinessStatus.ACTIVE)) > 0;
    }

    /**
     * 删除指定的智能模型路由规则。
     *
     * @param id 路由规则 ID
     * @return 成功响应
     */
    @Operation(summary = "删除智能模型路由规则", description = "删除指定的模型路由规则。")
    @DeleteMapping("/routers/{id}")
    public ApiResponse<?> deleteRouterRule(@PathVariable Long id) {
        SecurityUser currentUser = getAuthenticatedUser();
        SysModelRouterRuleEntity rule = sysModelRouterRuleMapper.selectById(id);
        if (rule == null || !rule.getTenantId().equals(currentUser.getTenantId())) {
            return ApiResponse.fail("路由不存在。");
        }
        sysModelRouterRuleMapper.deleteById(id);
        return ApiResponse.ok("路由已删除。", null);
    }

    // ==========================================
    // 5. 开发者 API 秘钥 (Developer API Keys)
    // ==========================================

    /**
     * 获取租户名下的开发者 Open API 密钥列表（脱敏掩码展示）。
     *
     * @return APIKey 实体列表
     */
    @Operation(summary = "获取开发者 API 秘钥列表", description = "获取当前租户拥有的开发者 API 访问密钥脱敏列表。")
    @GetMapping("/api-keys")
    public ApiResponse<?> listApiKeys() {
        SecurityUser currentUser = getAuthenticatedUser();
        List<SysApiKeyEntity> keys = sysApiKeyMapper.selectList(
                new QueryWrapper<SysApiKeyEntity>().eq("tenant_id", currentUser.getTenantId())
        );
        return ApiResponse.ok(keys);
    }

    /**
     * 生成全新的 Open API 访问秘钥并返回仅本次可见的明文 key 字符串。
     *
     * @return 包含明文 sk-studio-xxx 的响应
     */
    @Operation(summary = "创建开发者 API 秘钥", description = "生成新的 Open API 访问秘钥并返回一次性明文。")
    @PostMapping("/api-keys")
    public ApiResponse<?> createApiKey() {
        SecurityUser currentUser = getAuthenticatedUser();
        String rawKey = "sk-studio-" + UUID.randomUUID().toString().replace("-", "");
        String mask = "sk-st..." + rawKey.substring(rawKey.length() - 6);
        String hash = sha256(rawKey);

        SysApiKeyEntity apiKey = new SysApiKeyEntity();
        apiKey.setTenantId(currentUser.getTenantId());
        apiKey.setApiKeyHash(hash);
        apiKey.setApiKeyMask(mask);
        apiKey.setOwnerUser(currentUser.getUsername());
        apiKey.setStatus("ACTIVE");
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setExpiresAt(LocalDateTime.now().plusMonths(6));

        sysApiKeyMapper.insert(apiKey);

        Map<String, Object> res = new HashMap<>();
        res.put("apiKey", rawKey);
        res.put("mask", mask);
        res.put("expiresAt", apiKey.getExpiresAt());
        return ApiResponse.ok("API Key 申请成功，请妥善保存（明文只展示一次）。", res);
    }

    /**
     * 吊销并删除指定 ID 的开发者 ApiKey。
     *
     * @param id ApiKey 记录 ID
     * @return 吊销成功响应
     */
    @Operation(summary = "吊销开发者 API 秘钥", description = "失效并彻底删除指定 ID 的 API Key。")
    @DeleteMapping("/api-keys/{id}")
    public ApiResponse<?> deleteApiKey(@PathVariable Long id) {
        SecurityUser currentUser = getAuthenticatedUser();
        SysApiKeyEntity key = sysApiKeyMapper.selectById(id);
        if (key == null || !key.getTenantId().equals(currentUser.getTenantId())) {
            return ApiResponse.fail("秘钥不存在。");
        }
        sysApiKeyMapper.deleteById(id);
        return ApiResponse.ok("秘钥已成功吊销。", null);
    }

    // ==========================================
    // 6. 平台级所有租户管理 (SaaS Tenants - Super Admin Only)
    // ==========================================

    /**
     * 超级管理员查看全平台所有注册租户信息清单。
     *
     * @return 租户实体列表
     */
    @Operation(summary = "获取全平台租户列表", description = "超级管理员查看所有注册企业租户及其基本信息。")
    @GetMapping("/tenants")
    public ApiResponse<?> listAllTenants() {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("非超级管理员，无权查看全局租户。");
        }
        List<TenantEntity> tenants = tenantMapper.selectList(null);
        return ApiResponse.ok(tenants);
    }

    /** 租户配额修改请求体 */
    public record TenantQuotaUpdateRequest(Long tokenLimit, Integer workflowLimit, Integer storageLimit) {}

    /**
     * 超级管理员手动调整特定租户的月度 Token 额度、并发数及存储容量上限。
     *
     * @param id 租户 ID
     * @param request 配额更新请求
     * @return 配置更新结果
     */
    @Operation(summary = "更新租户用量配额", description = "超级管理员调整指定租户的 Token、工作流及存储额度。")
    @PutMapping("/tenants/{id}/quota")
    public ApiResponse<?> updateTenantQuota(@PathVariable Long id, @RequestBody TenantQuotaUpdateRequest request) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("无权修改计费配额。");
        }
        TenantBillingQuotaEntity quota = tenantBillingQuotaMapper.selectOne(
                new QueryWrapper<TenantBillingQuotaEntity>().eq("tenant_id", id)
        );
        if (quota == null) {
            quota = new TenantBillingQuotaEntity();
            quota.setTenantId(id);
            quota.setMonthlyTokenLimit(request.tokenLimit());
            quota.setMonthlyWorkflowLimit(request.workflowLimit());
            quota.setStorageLimitMb(request.storageLimit());
            quota.setMonthlyTokenUsed(0L);
            quota.setMonthlyWorkflowUsed(0);
            quota.setUpdatedAt(LocalDateTime.now());
            tenantBillingQuotaMapper.insert(quota);
        } else {
            quota.setMonthlyTokenLimit(request.tokenLimit());
            quota.setMonthlyWorkflowLimit(request.workflowLimit());
            quota.setStorageLimitMb(request.storageLimit());
            quota.setUpdatedAt(LocalDateTime.now());
            tenantBillingQuotaMapper.updateById(quota);
        }
        return ApiResponse.ok("租户用量额度配置更新成功。", null);
    }

    /**
     * 超级管理员冻结或解冻特定企业租户。
     *
     * @param id 租户 ID
     * @param status 新状态
     * @return 变更响应
     */
    @Operation(summary = "修改租户启用状态", description = "超级管理员冻结或解冻指定企业租户。")
    @PutMapping("/tenants/{id}/status")
    public ApiResponse<?> changeTenantStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUser currentUser = getAuthenticatedUser();
        if (!"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ApiResponse.fail("无权冻结租户。");
        }
        TenantEntity tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            return ApiResponse.fail("租户不存在。");
        }
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        return ApiResponse.ok("租户状态变更成功。", null);
    }

    // ==========================================
    // 7. 审计日志查询 (Audit Logs)
    // ==========================================

    /**
     * 获取全平台或当前租户最新的系统操作审计日志明细（Limit 200）。
     *
     * @return 审计日志列表
     */
    @Operation(summary = "查询平台审计日志", description = "获取租户或全平台的系统操作痕迹与审计记录。")
    @GetMapping("/audit-logs")
    public ApiResponse<?> getAuditLogs() {
        SecurityUser currentUser = getAuthenticatedUser();
        List<AuditLogEntity> logs;
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            logs = auditLogMapper.selectList(new QueryWrapper<AuditLogEntity>().orderByDesc("id").last("limit 200"));
        } else {
            logs = auditLogMapper.selectList(
                    new QueryWrapper<AuditLogEntity>()
                            .eq("tenant_id", currentUser.getTenantId())
                            .orderByDesc("id")
                            .last("limit 200")
            );
        }
        return ApiResponse.ok(logs);
    }

    // ==========================================
    // 8. 计量计费看板数据 (Billing & Metering)
    // ==========================================

    /**
     * 获取当前租户的计量计费已用资源指标与最大配额限制。
     *
     * @return 配额与用量对象
     */
    @Operation(summary = "获取计量计费配额看板", description = "获取当前租户的已用资源指标与最大限制配额。")
    @GetMapping("/billing")
    public ApiResponse<?> getBillingInfo() {
        SecurityUser currentUser = getAuthenticatedUser();
        TenantBillingQuotaEntity quota = tenantBillingQuotaMapper.selectOne(
                new QueryWrapper<TenantBillingQuotaEntity>().eq("tenant_id", currentUser.getTenantId())
        );
        if (quota == null) {
            quota = new TenantBillingQuotaEntity();
            quota.setTenantId(currentUser.getTenantId());
            quota.setMonthlyTokenLimit(1000000L);
            quota.setMonthlyTokenUsed(0L);
            quota.setMonthlyWorkflowLimit(1000);
            quota.setMonthlyWorkflowUsed(0);
            quota.setStorageLimitMb(100);
            quota.setUpdatedAt(LocalDateTime.now());
            tenantBillingQuotaMapper.insert(quota);
        }
        return ApiResponse.ok(quota);
    }

    /**
     * SHA-256 哈希计算辅助方法，用于生成 ApiKey 摘要散列。
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 算法不可用。", e);
        }
    }

    /** 创建受保护模型凭证引用的请求契约。 */
    public record CreateCredentialRequest(String name, String secret) {
    }

    /** 更新或轮换模型凭证引用的请求契约。 */
    public record UpdateCredentialRequest(String name, String secret) {
    }

    /** 智能路由结构化条件。 */
    public record RoutingCondition(String field, String operator, String value) {
    }

    /** 创建智能路由规则的强类型请求。 */
    public record RoutingRuleRequest(
            String ruleName,
            List<RoutingCondition> conditions,
            String primaryModelKey,
            String backupModelKey
    ) {
    }

    /** 不暴露内部正则表达式的路由规则视图。 */
    public record RoutingRuleView(
            Long id,
            String ruleName,
            String primaryModelKey,
            String backupModelKey,
            String status,
            String conditionSummary,
            LocalDateTime createdAt
    ) {
    }
}

