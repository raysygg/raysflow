package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.application.security.UserSessionService;
import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.EnterpriseIdentityConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasMfaPolicyEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasScimIdentityEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 身份提供方故障、配置回滚和批量去配置保护测试。 */
class EnterpriseIdentityLifecycleServiceTest {

    /** 身份提供方故障必须返回可读失败结果并保留现有配置状态。 */
    @Test
    void validationFailureDoesNotActivateBrokenConfig() {
        EnterpriseIdentityConfigMapper configMapper = mock(EnterpriseIdentityConfigMapper.class);
        EnterpriseIdentityAdapter adapter = mock(EnterpriseIdentityAdapter.class);
        EnterpriseIdentityConfigEntity config = config(4L, "DRAFT");
        when(configMapper.selectOne(any())).thenReturn(config);
        when(adapter.supports(any())).thenReturn(true);
        when(adapter.validate(any())).thenThrow(new IllegalStateException("连接失败"));
        EnterpriseIdentityLifecycleService service = service(configMapper, List.of(adapter), new SaasGovernanceProperties());

        var result = service.validate(user(), 4L);

        assertFalse(result.valid());
        assertTrue(result.issues().get(0).contains("暂时不可用"));
        verify(configMapper, never()).update(isNull(), any());
    }

    /** 恢复历史版本必须创建新草稿，不能原地改写旧版本。 */
    @Test
    void restoreCreatesNewDraftVersion() {
        EnterpriseIdentityConfigMapper configMapper = mock(EnterpriseIdentityConfigMapper.class);
        EnterpriseIdentityConfigEntity old = config(4L, "DISABLED");
        old.setVersionNo(2);
        when(configMapper.selectOne(any())).thenReturn(old);
        when(configMapper.selectList(any())).thenReturn(List.of(old));
        EnterpriseIdentityLifecycleService service = service(configMapper, List.of(), new SaasGovernanceProperties());

        EnterpriseIdentityConfigEntity restored = service.restoreAsDraft(user(), 4L);

        assertEquals("DRAFT", restored.getStatus());
        assertEquals(3, restored.getVersionNo());
        ArgumentCaptor<EnterpriseIdentityConfigEntity> captor = ArgumentCaptor.forClass(EnterpriseIdentityConfigEntity.class);
        verify(configMapper).insert(captor.capture());
        assertNotSame(old, captor.getValue());
    }

    /** 超过批处理上限时必须在处理任何成员前拒绝。 */
    @Test
    void batchDeprovisionHonorsSafetyLimit() {
        SaasGovernanceProperties properties = new SaasGovernanceProperties();
        properties.setGovernanceBatchLimit(1);
        EnterpriseIdentityLifecycleService service = service(mock(EnterpriseIdentityConfigMapper.class), List.of(), properties);
        var command = new EnterpriseIdentityLifecycleService.ScimUserCommand("source", "id", "user", "用户",
                "user@example.com", false, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.deprovisionScimUsers(user(), List.of(command, command)));
    }

    /** SCIM 重放必须更新已有成员，不得再次创建用户或绑定记录。 */
    @Test
    void scimReplayUpdatesExistingIdentity() {
        SaasScimIdentityMapper scimMapper = mock(SaasScimIdentityMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SaasScimIdentityEntity identity = new SaasScimIdentityEntity();
        identity.setId(6L);
        identity.setTenantId(1L);
        identity.setUserId(12L);
        SysUserEntity existing = new SysUserEntity();
        existing.setId(12L);
        existing.setTenantId(1L);
        existing.setStatus("ACTIVE");
        when(scimMapper.selectOne(any())).thenReturn(identity);
        when(userMapper.selectOne(any())).thenReturn(existing);
        EnterpriseIdentityLifecycleService service = new EnterpriseIdentityLifecycleService(
                mock(EnterpriseIdentityConfigMapper.class), scimMapper, mock(SaasScimGroupMapper.class),
                mock(SaasMfaPolicyMapper.class), mock(SaasEmergencyAccessMapper.class), userMapper,
                mock(SysRoleMapper.class), mock(SysUserRoleMapper.class), mock(UserSessionService.class),
                List.of(), mock(AuditApplicationService.class), mock(PasswordEncoder.class),
                new com.fasterxml.jackson.databind.ObjectMapper(), new SaasGovernanceProperties());

        var result = service.upsertScimUser(user(), new EnterpriseIdentityLifecycleService.ScimUserCommand(
                "source", "external-1", "member", "成员", "member@example.com", true, List.of()));

        assertFalse(result.created());
        verify(userMapper, never()).insert(any(SysUserEntity.class));
        verify(scimMapper, never()).insert(any(SaasScimIdentityEntity.class));
        verify(userMapper).updateById(existing);
        verify(scimMapper).updateById(identity);
    }

    /** MFA 时效过期后必须要求重新完成强化认证。 */
    @Test
    void expiredMfaRequiresStepUp() {
        SaasMfaPolicyMapper mfaMapper = mock(SaasMfaPolicyMapper.class);
        SaasMfaPolicyEntity policy = new SaasMfaPolicyEntity();
        policy.setRequired(true);
        policy.setStepUpMinutes(15);
        when(mfaMapper.selectOne(any())).thenReturn(policy);
        EnterpriseIdentityLifecycleService service = new EnterpriseIdentityLifecycleService(
                mock(EnterpriseIdentityConfigMapper.class), mock(SaasScimIdentityMapper.class),
                mock(SaasScimGroupMapper.class), mfaMapper, mock(SaasEmergencyAccessMapper.class),
                mock(SysUserMapper.class), mock(SysRoleMapper.class), mock(SysUserRoleMapper.class),
                mock(UserSessionService.class), List.of(), mock(AuditApplicationService.class),
                mock(PasswordEncoder.class), new com.fasterxml.jackson.databind.ObjectMapper(),
                new SaasGovernanceProperties());

        var decision = service.requireStepUp(user(), "修改身份配置", LocalDateTime.now().minusMinutes(30));

        assertTrue(decision.challengeRequired());
    }

    /** 紧急访问只能由授权目标用户在同一租户内消费。 */
    @Test
    void emergencyAccessRejectsUnmatchedActor() {
        SaasEmergencyAccessMapper emergencyMapper = mock(SaasEmergencyAccessMapper.class);
        EnterpriseIdentityLifecycleService service = new EnterpriseIdentityLifecycleService(
                mock(EnterpriseIdentityConfigMapper.class), mock(SaasScimIdentityMapper.class),
                mock(SaasScimGroupMapper.class), mock(SaasMfaPolicyMapper.class), emergencyMapper,
                mock(SysUserMapper.class), mock(SysRoleMapper.class), mock(SysUserRoleMapper.class),
                mock(UserSessionService.class), List.of(), mock(AuditApplicationService.class),
                mock(PasswordEncoder.class), new com.fasterxml.jackson.databind.ObjectMapper(),
                new SaasGovernanceProperties());
        when(emergencyMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.consumeEmergencyAccess(user(), 99L));
        verify(emergencyMapper, never()).update(any(), any());
    }

    private EnterpriseIdentityLifecycleService service(EnterpriseIdentityConfigMapper configMapper,
                                                       List<EnterpriseIdentityAdapter> adapters,
                                                       SaasGovernanceProperties properties) {
        return new EnterpriseIdentityLifecycleService(configMapper, mock(SaasScimIdentityMapper.class),
                mock(SaasScimGroupMapper.class), mock(SaasMfaPolicyMapper.class),
                mock(SaasEmergencyAccessMapper.class), mock(SysUserMapper.class), mock(SysRoleMapper.class),
                mock(SysUserRoleMapper.class), mock(UserSessionService.class), adapters,
                mock(AuditApplicationService.class), mock(PasswordEncoder.class),
                new com.fasterxml.jackson.databind.ObjectMapper(), properties);
    }

    private EnterpriseIdentityConfigEntity config(Long id, String status) {
        EnterpriseIdentityConfigEntity entity = new EnterpriseIdentityConfigEntity();
        entity.setId(id);
        entity.setTenantId(1L);
        entity.setVersionNo(1);
        entity.setProtocol("OIDC");
        entity.setStatus(status);
        entity.setIssuer("https://identity.example.com");
        entity.setCallbackUrl("https://studio.example.com/callback");
        entity.setOrganizationClaim("organization");
        entity.setClaimMappingJson("{}");
        entity.setMfaPolicyJson("{}");
        entity.setScimEnabled(true);
        entity.setSecretRef("secret://identity");
        return entity;
    }

    private SecurityUser user() {
        return new SecurityUser(7L, 1L, "admin", "ADMIN");
    }
}
