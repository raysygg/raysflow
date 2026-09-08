package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.application.security.UserSessionService;
import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.IdentityConfigStatus;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.IdentityProtocol;
import com.acme.agentstudio.infrastructure.persistence.entity.EnterpriseIdentityConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasEmergencyAccessEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasMfaPolicyEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasScimGroupEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasScimIdentityEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserRoleEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.EnterpriseIdentityConfigMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasEmergencyAccessMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasMfaPolicyMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasScimGroupMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasScimIdentityMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysRoleMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SaaS 企业身份与访问治理（Enterprise Identity & Access Governance）生命周期服务。
 * 负责统一管理租户单点登录（SSO/SAML/OIDC）草稿、验证与激活，SCIM 2.0 自动同步（Deprovisioning），阶梯式 MFA 动态提权校验（Step-Up Challenge）以及 Break-Glass 紧急避险通道授权。
 */
@Service
public class EnterpriseIdentityLifecycleService {

    /** 状态标识：活动中 */
    private static final String ACTIVE = "ACTIVE";

    /** 状态标识：已禁用 */
    private static final String DISABLED = "DISABLED";

    /** 企业 SSO 身份配置 Mapper */
    private final EnterpriseIdentityConfigMapper configMapper;

    /** SCIM 2.0 身份映射 Mapper */
    private final SaasScimIdentityMapper scimMapper;

    /** SCIM 2.0 用户组映射 Mapper */
    private final SaasScimGroupMapper scimGroupMapper;

    /** MFA 认证策略 Mapper */
    private final SaasMfaPolicyMapper mfaMapper;

    /** Break-Glass 紧急访问通道 Mapper */
    private final SaasEmergencyAccessMapper emergencyMapper;

    /** 系统用户 Mapper */
    private final SysUserMapper userMapper;

    /** 系统角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 用户-角色关联 Mapper */
    private final SysUserRoleMapper userRoleMapper;

    /** 用户会话服务 */
    private final UserSessionService sessionService;

    /** 身份协议适配器列表 */
    private final List<EnterpriseIdentityAdapter> adapters;

    /** 平台审计服务 */
    private final AuditApplicationService auditService;

    /** 密码加密编码器 */
    private final PasswordEncoder passwordEncoder;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** SaaS 治理配置属性 */
    private final SaasGovernanceProperties properties;

    /**
     * 构造函数注入所有依赖组件。
     */
    public EnterpriseIdentityLifecycleService(EnterpriseIdentityConfigMapper configMapper,
                                                SaasScimIdentityMapper scimMapper,
                                                SaasScimGroupMapper scimGroupMapper,
                                                SaasMfaPolicyMapper mfaMapper,
                                                SaasEmergencyAccessMapper emergencyMapper,
                                                SysUserMapper userMapper,
                                                SysRoleMapper roleMapper,
                                                SysUserRoleMapper userRoleMapper,
                                                UserSessionService sessionService,
                                                List<EnterpriseIdentityAdapter> adapters,
                                                AuditApplicationService auditService,
                                                PasswordEncoder passwordEncoder,
                                                ObjectMapper objectMapper,
                                                SaasGovernanceProperties properties) {
        this.configMapper = configMapper;
        this.scimMapper = scimMapper;
        this.scimGroupMapper = scimGroupMapper;
        this.mfaMapper = mfaMapper;
        this.emergencyMapper = emergencyMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.sessionService = sessionService;
        this.adapters = adapters;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 保存企业身份 SSO 配置草稿。
     *
     * @param actor 当前操作用户
     * @param command 身份配置提交命令对象
     * @return 保存后的身份配置实体
     */
    @Transactional
    public EnterpriseIdentityConfigEntity saveDraft(SecurityUser actor, IdentityConfigCommand command) {
        requireActor(actor);

        int version = configMapper.selectList(new LambdaQueryWrapper<EnterpriseIdentityConfigEntity>()
                        .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                        .orderByDesc(EnterpriseIdentityConfigEntity::getVersionNo))
                .stream()
                .findFirst()
                .map(EnterpriseIdentityConfigEntity::getVersionNo)
                .orElse(0) + 1;

        EnterpriseIdentityConfigEntity entity = new EnterpriseIdentityConfigEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setVersionNo(version);
        entity.setProtocol(command.protocol().name());
        entity.setStatus(IdentityConfigStatus.DRAFT.name());
        entity.setIssuer(command.issuer());
        entity.setEntityId(command.entityId());
        entity.setOrganizationClaim(command.organizationClaim());
        entity.setCallbackUrl(command.callbackUrl());
        entity.setClaimMappingJson(command.claimMappingJson());
        entity.setScimEnabled(command.scimEnabled());
        entity.setMfaPolicyJson(command.mfaPolicyJson());
        entity.setSecretRef(command.secretRef());
        entity.setCreatedBy(actor.getUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());

        configMapper.insert(entity);
        audit(actor, "IDENTITY_CONFIG_DRAFT_CREATED", entity.getId(), Map.of("version", version, "protocol", command.protocol().name()));
        return entity;
    }

    /**
     * 查询当前租户的身份配置版本，供管理页面刷新后恢复状态。
     *
     * @param actor 当前操作用户
     * @return 按版本倒序排列的配置列表
     */
    public List<EnterpriseIdentityConfigEntity> configurations(SecurityUser actor) {
        requireActor(actor);
        return configMapper.selectList(new LambdaQueryWrapper<EnterpriseIdentityConfigEntity>()
                .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                .orderByDesc(EnterpriseIdentityConfigEntity::getVersionNo));
    }

    /**
     * 将历史配置复制为新的草稿版本，避免原地修改审计历史。
     *
     * @param actor 当前操作用户
     * @param configId 历史配置 ID
     * @return 新建的草稿版本
     */
    @Transactional
    public EnterpriseIdentityConfigEntity restoreAsDraft(SecurityUser actor, Long configId) {
        EnterpriseIdentityConfigEntity source = requireConfig(actor, configId);
        EnterpriseIdentityConfigEntity draft = saveDraft(actor, new IdentityConfigCommand(
                IdentityProtocol.valueOf(source.getProtocol()), source.getIssuer(), source.getEntityId(),
                source.getOrganizationClaim(), source.getCallbackUrl(), source.getClaimMappingJson(),
                Boolean.TRUE.equals(source.getScimEnabled()), source.getMfaPolicyJson(), source.getSecretRef()));
        audit(actor, "IDENTITY_CONFIG_RESTORED", draft.getId(), Map.of("sourceConfigId", source.getId()));
        return draft;
    }

    /**
     * 校验测试特定版本的身份配置（元数据与 HTTPS 契约）。
     *
     * @param actor 当前操作用户
     * @param configId 配置实体 ID
     * @return 协议校验结果
     */
    @Transactional
    public EnterpriseIdentityAdapter.IdentityValidationResult validate(SecurityUser actor, Long configId) {
        EnterpriseIdentityConfigEntity entity = requireConfig(actor, configId);
        EnterpriseIdentityAdapter adapter = adapters.stream()
                .filter(item -> item.supports(IdentityProtocol.valueOf(entity.getProtocol())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前身份认证协议暂无可用适配器。"));

        EnterpriseIdentityAdapter.IdentityValidationResult result;
        try {
            result = adapter.validate(new EnterpriseIdentityAdapter.IdentityValidationRequest(
                    IdentityProtocol.valueOf(entity.getProtocol()), entity.getIssuer(), entity.getEntityId(),
                    entity.getCallbackUrl(), entity.getOrganizationClaim(), entity.getSecretRef()));
        } catch (RuntimeException exception) {
            // 外部身份源不可用时保留旧配置，只返回可操作的安全错误分类。
            result = new EnterpriseIdentityAdapter.IdentityValidationResult(false,
                    List.of("身份提供方暂时不可用，请检查网络和元数据地址后重试。"), null);
        }

        if (result.valid()) {
            configMapper.update(null, new LambdaUpdateWrapper<EnterpriseIdentityConfigEntity>()
                    .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                    .eq(EnterpriseIdentityConfigEntity::getId, configId)
                    .eq(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.DRAFT.name())
                    .set(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.VALIDATED.name())
                    .set(EnterpriseIdentityConfigEntity::getValidatedAt, LocalDateTime.now()));
        }

        audit(actor, "IDENTITY_CONFIG_VALIDATED", configId, Map.of("valid", result.valid(), "issueCount", result.issues().size()));
        return result;
    }

    /**
     * 激活企业身份配置（先禁用已有 ACTIVE 配置，再原子切为新配置）。
     *
     * @param actor 当前操作用户
     * @param configId 待激活的配置 ID
     */
    @Transactional
    public void activate(SecurityUser actor, Long configId) {
        EnterpriseIdentityConfigEntity entity = requireConfig(actor, configId);
        if (!IdentityConfigStatus.VALIDATED.name().equals(entity.getStatus())) {
            throw new IllegalStateException("身份配置必须在通过测试验证后才能正式激活使用。");
        }

        configMapper.update(null, new LambdaUpdateWrapper<EnterpriseIdentityConfigEntity>()
                .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                .eq(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.ACTIVE.name())
                .set(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.DISABLED.name()));

        configMapper.update(null, new LambdaUpdateWrapper<EnterpriseIdentityConfigEntity>()
                .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                .eq(EnterpriseIdentityConfigEntity::getId, configId)
                .eq(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.VALIDATED.name())
                .set(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.ACTIVE.name())
                .set(EnterpriseIdentityConfigEntity::getActivatedAt, LocalDateTime.now()));

        audit(actor, "IDENTITY_CONFIG_ACTIVATED", configId, Map.of("version", entity.getVersionNo()));
    }

    /**
     * 禁用指定的身份配置。
     *
     * @param actor 当前操作用户
     * @param configId 待禁用的配置 ID
     */
    @Transactional
    public void disable(SecurityUser actor, Long configId) {
        requireConfig(actor, configId);
        configMapper.update(null, new LambdaUpdateWrapper<EnterpriseIdentityConfigEntity>()
                .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                .eq(EnterpriseIdentityConfigEntity::getId, configId)
                .set(EnterpriseIdentityConfigEntity::getStatus, IdentityConfigStatus.DISABLED.name()));

        audit(actor, "IDENTITY_CONFIG_DISABLED", configId, Map.of());
    }

    /**
     * 处理 SCIM 2.0 用户同步/回收（Upsert & Deprovisioning）。
     *
     * @param actor 操作用户/系统调用者
     * @param command SCIM 用户参数
     * @return SCIM 操作处理结果
     */
    @Transactional
    public ScimResult upsertScimUser(SecurityUser actor, ScimUserCommand command) {
        requireActor(actor);

        SaasScimIdentityEntity identity = scimMapper.selectOne(new LambdaQueryWrapper<SaasScimIdentityEntity>()
                .eq(SaasScimIdentityEntity::getTenantId, actor.getTenantId())
                .eq(SaasScimIdentityEntity::getSourceSystem, command.sourceSystem())
                .eq(SaasScimIdentityEntity::getExternalId, command.externalId()));

        SysUserEntity user = identity == null
                ? null
                : userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getTenantId, actor.getTenantId())
                .eq(SysUserEntity::getId, identity.getUserId()));

        boolean created = user == null;
        if (created) {
            user = new SysUserEntity();
            user.setTenantId(actor.getTenantId());
            user.setUsername(command.userName());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setCreatedAt(LocalDateTime.now());
        }

        if (!command.active() && user != null && protectedAdministrator(user.getId())) {
            throw new IllegalStateException("受保护的核心超级管理员账户无法通过 SCIM 协议下线禁用。");
        }

        user.setNickname(command.displayName());
        user.setEmail(command.email());
        user.setStatus(command.active() ? ACTIVE : DISABLED);
        user.setUpdatedAt(LocalDateTime.now());

        if (created) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }

        if (identity == null) {
            identity = new SaasScimIdentityEntity();
            identity.setTenantId(actor.getTenantId());
            identity.setUserId(user.getId());
            identity.setExternalId(command.externalId());
            identity.setSourceSystem(command.sourceSystem());
            identity.setCreatedAt(LocalDateTime.now());
        }

        identity.setExternalUserName(command.userName());
        identity.setSyncStatus(command.active() ? ACTIVE : DISABLED);
        identity.setLastSyncedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());

        if (identity.getId() == null) {
            scimMapper.insert(identity);
        } else {
            scimMapper.updateById(identity);
        }

        bindRoles(actor.getTenantId(), user.getId(), command.roleCodes());

        if (!command.active()) {
            sessionService.revokeAll(actor.getTenantId(), user.getId());
        }

        audit(actor, command.active() ? "SCIM_USER_SYNCED" : "SCIM_USER_DEPROVISIONED", user.getId(),
                Map.of("created", created, "source", command.sourceSystem()));

        return new ScimResult(user.getId(), identity.getId(), created, user.getStatus());
    }

    /**
     * 批量执行 SCIM 去配置，并复用单用户的管理员保护、会话撤销和审计规则。
     *
     * @param actor 当前操作用户
     * @param commands 待去配置成员列表
     * @return 每个成员的真实处理结果
     */
    @Transactional
    public List<ScimResult> deprovisionScimUsers(SecurityUser actor, List<ScimUserCommand> commands) {
        requireActor(actor);
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("批量去配置成员列表不能为空。");
        }
        if (commands.size() > properties.getGovernanceBatchLimit()) {
            throw new IllegalArgumentException("单次批量去配置成员数量超过平台限制。");
        }
        return commands.stream()
                .map(command -> upsertScimUser(actor, new ScimUserCommand(command.sourceSystem(), command.externalId(),
                        command.userName(), command.displayName(), command.email(), false, command.roleCodes())))
                .toList();
    }

    /**
     * 处理 SCIM 2.0 用户组/部门映射同步。
     *
     * @param actor 当前操作用户
     * @param command SCIM 组参数
     * @return 组实体
     */
    @Transactional
    public SaasScimGroupEntity upsertScimGroup(SecurityUser actor, ScimGroupCommand command) {
        requireActor(actor);

        SaasScimGroupEntity group = scimGroupMapper.selectOne(new LambdaQueryWrapper<SaasScimGroupEntity>()
                .eq(SaasScimGroupEntity::getTenantId, actor.getTenantId())
                .eq(SaasScimGroupEntity::getSourceSystem, command.sourceSystem())
                .eq(SaasScimGroupEntity::getExternalId, command.externalId()));

        if (group == null) {
            group = new SaasScimGroupEntity();
            group.setTenantId(actor.getTenantId());
            group.setSourceSystem(command.sourceSystem());
            group.setExternalId(command.externalId());
            group.setCreatedAt(LocalDateTime.now());
        }

        group.setDisplayName(command.displayName());
        group.setMappedRoleCode(command.mappedRoleCode());
        group.setSyncStatus(command.active() ? ACTIVE : DISABLED);
        group.setLastSyncedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        if (group.getId() == null) {
            scimGroupMapper.insert(group);
        } else {
            scimGroupMapper.updateById(group);
        }
        return group;
    }

    /**
     * 保存或更新 MFA 认证与 Step-Up 提权策略。
     *
     * @param actor 当前操作用户
     * @param command MFA 策略提交参数
     * @return MFA 策略实体
     */
    @Transactional
    public SaasMfaPolicyEntity saveMfaPolicy(SecurityUser actor, MfaPolicyCommand command) {
        requireActor(actor);

        SaasMfaPolicyEntity entity = new SaasMfaPolicyEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setRoleCode(command.roleCode());
        entity.setRequired(command.required());
        entity.setStepUpMinutes(Math.max(1, command.stepUpMinutes()));
        entity.setHighRiskActionsJson(command.highRiskActionsJson());
        entity.setVersionNo(nextMfaVersion(actor.getTenantId(), command.roleCode()));
        entity.setStatus(ACTIVE);
        entity.setUpdatedBy(actor.getUserId());
        entity.setUpdatedAt(LocalDateTime.now());

        mfaMapper.insert(entity);
        return entity;
    }

    /**
     * 判断当前用户在执行高危敏感操作时是否需要强制再做 Step-Up 二次二次 MFA 验证。
     *
     * @param user 当前登录用户
     * @param action 操作名称
     * @param lastVerifiedAt 上次 MFA 验证通过的时间
     * @return StepUpDecision 决策对象
     */
    public StepUpDecision requireStepUp(SecurityUser user, String action, LocalDateTime lastVerifiedAt) {
        requireActor(user);

        SaasMfaPolicyEntity policy = mfaMapper.selectOne(new LambdaQueryWrapper<SaasMfaPolicyEntity>()
                .eq(SaasMfaPolicyEntity::getTenantId, user.getTenantId())
                .eq(SaasMfaPolicyEntity::getRoleCode, user.getRole())
                .eq(SaasMfaPolicyEntity::getStatus, ACTIVE)
                .orderByDesc(SaasMfaPolicyEntity::getVersionNo)
                .last("LIMIT 1"));

        if (policy == null || !Boolean.TRUE.equals(policy.getRequired())) {
            return new StepUpDecision(false, null, "当前操作角色无需提权二次验证。");
        }

        boolean expired = lastVerifiedAt == null || lastVerifiedAt.plusMinutes(policy.getStepUpMinutes()).isBefore(LocalDateTime.now());
        return new StepUpDecision(
                expired,
                expired ? LocalDateTime.now().plusMinutes(policy.getStepUpMinutes()) : null,
                expired ? "触发高危敏感操作提权规则，请完成 MFA 强化验证后继续。" : "MFA 强化验证仍在有效时间内。"
        );
    }

    /**
     * 签发 Break-Glass 紧急避险访问通道授权。
     *
     * @param actor 当前授权管理员
     * @param userId 获得授权的目标用户 ID
     * @param reason 紧急避险授权原因说明
     * @param minutes 授权有效时长（分钟）
     * @return 紧急访问记录实体
     */
    @Transactional
    public SaasEmergencyAccessEntity grantEmergencyAccess(SecurityUser actor, Long userId, String reason, int minutes) {
        requireActor(actor);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("申请紧急避险访问必须详细填写授权原因。");
        }

        SaasEmergencyAccessEntity entity = new SaasEmergencyAccessEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setUserId(userId);
        entity.setReasonSummary(reason.trim());
        entity.setAccessStatus("READY");
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, minutes)));
        entity.setCreatedAt(LocalDateTime.now());

        emergencyMapper.insert(entity);
        audit(actor, "EMERGENCY_ACCESS_GRANTED", entity.getId(), Map.of("userId", userId, "expiresAt", entity.getExpiresAt().toString()));
        return entity;
    }

    /**
     * 消费使用 Break-Glass 紧急避险通道。
     *
     * @param actor 提交消费的操作用户
     * @param accessId 紧急访问记录 ID
     * @return 更新状态后的紧急访问实体
     */
    @Transactional
    public SaasEmergencyAccessEntity consumeEmergencyAccess(SecurityUser actor, Long accessId) {
        requireActor(actor);

        SaasEmergencyAccessEntity access = emergencyMapper.selectOne(new LambdaQueryWrapper<SaasEmergencyAccessEntity>()
                .eq(SaasEmergencyAccessEntity::getTenantId, actor.getTenantId())
                .eq(SaasEmergencyAccessEntity::getId, accessId)
                .eq(SaasEmergencyAccessEntity::getUserId, actor.getUserId()));

        if (access == null || !"READY".equals(access.getAccessStatus()) || access.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("紧急避险访问记录不存在、已被使用或已超出允许有效期。");
        }

        emergencyMapper.update(null, new LambdaUpdateWrapper<SaasEmergencyAccessEntity>()
                .eq(SaasEmergencyAccessEntity::getTenantId, actor.getTenantId())
                .eq(SaasEmergencyAccessEntity::getId, accessId)
                .eq(SaasEmergencyAccessEntity::getAccessStatus, "READY")
                .set(SaasEmergencyAccessEntity::getAccessStatus, "USED")
                .set(SaasEmergencyAccessEntity::getUsedAt, LocalDateTime.now()));

        access.setAccessStatus("USED");
        access.setUsedAt(LocalDateTime.now());

        audit(actor, "EMERGENCY_ACCESS_USED", accessId, Map.of("expiresAt", access.getExpiresAt().toString()));
        return access;
    }

    /** 校验配置并提取实体 */
    private EnterpriseIdentityConfigEntity requireConfig(SecurityUser actor, Long id) {
        requireActor(actor);
        EnterpriseIdentityConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<EnterpriseIdentityConfigEntity>()
                .eq(EnterpriseIdentityConfigEntity::getTenantId, actor.getTenantId())
                .eq(EnterpriseIdentityConfigEntity::getId, id));
        if (entity == null) {
            throw new IllegalArgumentException("未找到对应的企业身份 SSO 配置记录。");
        }
        return entity;
    }

    /** 计算下一版本的 MFA 策略版本号 */
    private int nextMfaVersion(Long tenantId, String roleCode) {
        return mfaMapper.selectList(new LambdaQueryWrapper<SaasMfaPolicyEntity>()
                        .eq(SaasMfaPolicyEntity::getTenantId, tenantId)
                        .eq(SaasMfaPolicyEntity::getRoleCode, roleCode)
                        .orderByDesc(SaasMfaPolicyEntity::getVersionNo))
                .stream()
                .findFirst()
                .map(SaasMfaPolicyEntity::getVersionNo)
                .orElse(0) + 1;
    }

    /** 校验用户是否为受保护的核心管理员 */
    private boolean protectedAdministrator(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getUserId, userId))
                .stream()
                .map(SysUserRoleEntity::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return false;
        }

        return userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId)) > 0
                && roleMapper.selectCount(new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getId, roleIds)
                .in(SysRoleEntity::getRoleCode, "SUPER_ADMIN", "ADMIN")
                .eq(SysRoleEntity::getStatus, ACTIVE)) > 0;
    }

    /** 绑定系统角色 */
    private void bindRoles(Long tenantId, Long userId, List<String> roleCodes) {
        if (roleCodes == null) {
            return;
        }
        for (String code : roleCodes) {
            SysRoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getTenantId, tenantId)
                    .eq(SysRoleEntity::getRoleCode, code)
                    .eq(SysRoleEntity::getStatus, ACTIVE));
            if (role == null) {
                continue;
            }
            Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleEntity>()
                    .eq(SysUserRoleEntity::getUserId, userId)
                    .eq(SysUserRoleEntity::getRoleId, role.getId()));
            if (count == 0) {
                SysUserRoleEntity binding = new SysUserRoleEntity();
                binding.setUserId(userId);
                binding.setRoleId(role.getId());
                userRoleMapper.insert(binding);
            }
        }
    }

    /** 记录审计 */
    private void audit(SecurityUser actor, String action, Long targetId, Map<String, Object> detail) {
        auditService.recordWorkflowAction(actor.getTenantId(), actor.getUsername(), action, targetId, detail);
    }

    /** 校验 Actor 非空 */
    private void requireActor(SecurityUser actor) {
        if (actor == null || actor.getTenantId() == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份无效，请重新登录。");
        }
    }

    /** 身份配置提交 Command */
    public record IdentityConfigCommand(
            IdentityProtocol protocol,
            String issuer,
            String entityId,
            String organizationClaim,
            String callbackUrl,
            String claimMappingJson,
            boolean scimEnabled,
            String mfaPolicyJson,
            String secretRef
    ) { }

    /** SCIM 用户同步 Command */
    public record ScimUserCommand(
            String sourceSystem,
            String externalId,
            String userName,
            String displayName,
            String email,
            boolean active,
            List<String> roleCodes
    ) { }

    /** SCIM 同步结果 Result */
    public record ScimResult(
            Long userId,
            Long identityId,
            boolean created,
            String status
    ) { }

    /** SCIM 组 Command */
    public record ScimGroupCommand(
            String sourceSystem,
            String externalId,
            String displayName,
            String mappedRoleCode,
            boolean active
    ) { }

    /** MFA 策略 Command */
    public record MfaPolicyCommand(
            String roleCode,
            boolean required,
            int stepUpMinutes,
            String highRiskActionsJson
    ) { }

    /** Step-Up 判定结果 Result */
    public record StepUpDecision(
            boolean challengeRequired,
            LocalDateTime validUntil,
            String message
    ) { }
}

