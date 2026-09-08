package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasUsageEventEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasInvoiceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 幂等计量、未知成本和财务字段权限测试。 */
class SaasBillingAndUsageServiceTest {

    /** Worker 重复上报同一幂等键时不得重复计量。 */
    @Test
    void appendReturnsExistingUsageForDuplicateKey() {
        SaasUsageEventMapper usageMapper = mock(SaasUsageEventMapper.class);
        SaasUsageEventEntity existing = new SaasUsageEventEntity();
        existing.setId(9L);
        when(usageMapper.selectOne(any())).thenReturn(existing);
        SaasUsageLedgerService service = usageService(usageMapper, mock(PlatformPriceVersionMapper.class), mock(ModelPriceMapper.class));

        SaasUsageEventEntity result = service.append(command("same-key"));

        assertSame(existing, result);
        verify(usageMapper, never()).insert(any(SaasUsageEventEntity.class));
    }

    /** 缺少价格时成本必须保持未知，不能写成零。 */
    @Test
    void appendKeepsCostUnknownWhenPriceIsMissing() {
        SaasUsageEventMapper usageMapper = mock(SaasUsageEventMapper.class);
        PlatformPriceVersionMapper platformPriceMapper = mock(PlatformPriceVersionMapper.class);
        ModelPriceMapper modelPriceMapper = mock(ModelPriceMapper.class);
        when(usageMapper.selectOne(any())).thenReturn(null);
        when(platformPriceMapper.selectOne(any())).thenReturn(null);
        when(modelPriceMapper.selectOne(any())).thenReturn(null);
        SaasUsageLedgerService service = usageService(usageMapper, platformPriceMapper, modelPriceMapper);

        SaasUsageEventEntity result = service.append(command("new-key"));

        assertEquals("UNKNOWN", result.getCostStatus());
        assertNull(result.getCostAmount());
        verify(usageMapper).insert(result);
    }

    /** 无财务权限的应用设计者不得导出账单字段。 */
    @Test
    void invoiceExportRequiresFinanceRole() {
        SaasBillingLifecycleService service = new SaasBillingLifecycleService(
                mock(SaasBillingPeriodMapper.class), mock(SaasInvoiceMapper.class),
                mock(SaasInvoiceLineMapper.class), mock(SaasUsageEventMapper.class),
                mock(SaasBillingNoteMapper.class), mock(SaasBillingSyncMapper.class),
                List.of(), mock(AuditApplicationService.class));

        assertThrows(AuthorizationDeniedException.class,
                () -> service.exportInvoice(new SecurityUser(3L, 1L, "designer", "DESIGNER"), 8L));
    }

    /** 已确认账单再次确认时不得原地更新财务快照。 */
    @Test
    void finalizedInvoiceRemainsImmutable() {
        SaasInvoiceMapper invoiceMapper = mock(SaasInvoiceMapper.class);
        SaasInvoiceEntity invoice = new SaasInvoiceEntity();
        invoice.setId(8L);
        invoice.setTenantId(1L);
        invoice.setStatus("FINALIZED");
        when(invoiceMapper.selectOne(any())).thenReturn(invoice);
        SaasBillingLifecycleService service = new SaasBillingLifecycleService(
                mock(SaasBillingPeriodMapper.class), invoiceMapper, mock(SaasInvoiceLineMapper.class),
                mock(SaasUsageEventMapper.class), mock(SaasBillingNoteMapper.class),
                mock(SaasBillingSyncMapper.class), List.of(), mock(AuditApplicationService.class));

        assertSame(invoice, service.finalizeInvoice(new SecurityUser(7L, 1L, "finance", "FINANCE"), 8L));
        verify(invoiceMapper, never()).update(any(), any());
    }

    private SaasUsageLedgerService usageService(SaasUsageEventMapper usageMapper,
                                                PlatformPriceVersionMapper platformPriceMapper,
                                                ModelPriceMapper modelPriceMapper) {
        return new SaasUsageLedgerService(usageMapper, mock(SaasUsageAdjustmentMapper.class),
                platformPriceMapper, modelPriceMapper);
    }

    private SaasUsageLedgerService.UsageCommand command(String key) {
        return new SaasUsageLedgerService.UsageCommand(1L, 2L, "release", "run", "model",
                "MODEL_TOKEN", "研发", key, BigDecimal.TEN, "TOKEN", true,
                6, 4, LocalDateTime.now());
    }
}
