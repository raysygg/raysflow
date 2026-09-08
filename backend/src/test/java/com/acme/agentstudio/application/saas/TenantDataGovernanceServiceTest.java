package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SaasGovernanceProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasGovernanceRequestEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasGovernanceEvidenceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 导出删除权限、Legal Hold、跨存储部分失败和证据测试。 */
class TenantDataGovernanceServiceTest {

    /** 普通设计者不能审批高风险治理请求。 */
    @Test
    void designerCannotApproveGovernanceRequest() {
        Fixture fixture = fixture(List.of());

        assertThrows(AuthorizationDeniedException.class,
                () -> fixture.service.approve(new SecurityUser(2L, 1L, "designer", "DESIGNER"), 3L));
        verify(fixture.requestMapper, never()).update(any(), any());
    }

    /** 活动法律冻结必须在任何存储删除前阻断请求。 */
    @Test
    void legalHoldBlocksDeletePreview() {
        DataGovernanceStoreAdapter store = mock(DataGovernanceStoreAdapter.class);
        when(store.storeType()).thenReturn("DATABASE");
        when(store.preview(any(), any())).thenReturn(new DataGovernanceStoreAdapter.StorePreview(5, "五条记录", false));
        Fixture fixture = fixture(List.of(store));
        when(fixture.holdMapper.selectCount(any())).thenReturn(1L);

        var preview = fixture.service.previewDelete(complianceUser(), "{}");

        assertTrue(preview.legalHoldBlocked());
        assertTrue(preview.stores().get(0).blocked());
        verify(store, never()).delete(any(), any(), any());
    }

    /** 任一存储失败时结果必须保持部分失败并为每个阶段写证据。 */
    @Test
    void crossStoreDeletePreservesPartialFailure() {
        DataGovernanceStoreAdapter database = mock(DataGovernanceStoreAdapter.class);
        when(database.storeType()).thenReturn("DATABASE");
        when(database.preview(any(), any())).thenReturn(new DataGovernanceStoreAdapter.StorePreview(2, "主库记录", false));
        when(database.delete(any(), any(), any())).thenReturn(new DataGovernanceStoreAdapter.StoreExecution(true, 2, "checksum", "主库已删除"));
        DataGovernanceStoreAdapter vector = mock(DataGovernanceStoreAdapter.class);
        when(vector.storeType()).thenReturn("QDRANT");
        when(vector.preview(any(), any())).thenReturn(new DataGovernanceStoreAdapter.StorePreview(2, "向量记录", false));
        when(vector.delete(any(), any(), any())).thenThrow(new IllegalStateException("向量服务不可用"));
        Fixture fixture = fixture(List.of(database, vector));
        when(fixture.holdMapper.selectCount(any())).thenReturn(0L);
        when(fixture.requestMapper.selectOne(any())).thenReturn(approvedDelete());
        when(fixture.evidenceMapper.selectOne(any())).thenReturn(null);

        var result = fixture.service.executeDelete(complianceUser(), 8L);

        assertFalse(result.completed());
        assertEquals(2, result.stores().size());
        assertEquals(1, result.stores().stream().filter(TenantDataGovernanceService.StoreResult::success).count());
        verify(fixture.evidenceMapper, times(2)).insert(any(SaasGovernanceEvidenceEntity.class));
        verify(fixture.requestMapper, atLeastOnce()).update(isNull(), any());
    }

    private Fixture fixture(List<DataGovernanceStoreAdapter> stores) {
        SaasGovernanceRequestMapper requestMapper = mock(SaasGovernanceRequestMapper.class);
        SaasGovernanceEvidenceMapper evidenceMapper = mock(SaasGovernanceEvidenceMapper.class);
        SaasLegalHoldMapper holdMapper = mock(SaasLegalHoldMapper.class);
        TenantDataGovernanceService service = new TenantDataGovernanceService(
                mock(SaasDataRetentionPolicyMapper.class), requestMapper, evidenceMapper, holdMapper,
                stores, List.of(), new SaasGovernanceProperties());
        return new Fixture(service, requestMapper, evidenceMapper, holdMapper);
    }

    private SaasGovernanceRequestEntity approvedDelete() {
        SaasGovernanceRequestEntity entity = new SaasGovernanceRequestEntity();
        entity.setId(8L);
        entity.setTenantId(1L);
        entity.setRequestType("DELETE");
        entity.setRequestStatus("APPROVED");
        entity.setScopeJson("{}");
        return entity;
    }

    private SecurityUser complianceUser() {
        return new SecurityUser(9L, 1L, "compliance", "COMPLIANCE");
    }

    private record Fixture(TenantDataGovernanceService service, SaasGovernanceRequestMapper requestMapper,
                           SaasGovernanceEvidenceMapper evidenceMapper, SaasLegalHoldMapper holdMapper) { }
}
