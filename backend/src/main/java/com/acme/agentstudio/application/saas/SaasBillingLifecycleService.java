package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.application.audit.AuditApplicationService;
import com.acme.agentstudio.common.exception.AuthorizationDeniedException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.CostStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasBillingNoteEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasBillingPeriodEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasBillingSyncEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasInvoiceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasInvoiceLineEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasUsageEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasBillingNoteMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasBillingPeriodMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasBillingSyncMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasInvoiceLineMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasInvoiceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasUsageEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SaaS 租户账期（Billing Period）、账单（Invoice）、红蓝字调整单（Credit/Debit Note）与外部财务同步状态机服务。
 * 负责管理账期的开启（OPEN）、草稿账单汇总生成（DRAFT）、账单确认冻结（FINALIZED）、退款/冲抵调整单及与外部 Stripe/ERP 系统的幂等同步。
 */
@Service
public class SaasBillingLifecycleService {

    /** 账单状态：草稿 */
    private static final String DRAFT = "DRAFT";

    /** 账单状态：已确认终态 */
    private static final String FINALIZED = "FINALIZED";

    /** 可查看账单与成本的角色 */
    private static final Set<String> BILLING_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "FINANCE");

    /** 可导出账单敏感财务字段的角色 */
    private static final Set<String> BILLING_EXPORT_ROLES = Set.of("SUPER_ADMIN", "FINANCE");

    /** 账期 Mapper */
    private final SaasBillingPeriodMapper periodMapper;

    /** 账单 Mapper */
    private final SaasInvoiceMapper invoiceMapper;

    /** 账单明细 Mapper */
    private final SaasInvoiceLineMapper lineMapper;

    /** 计费用量事件 Mapper */
    private final SaasUsageEventMapper usageMapper;

    /** 账单调整单 Mapper */
    private final SaasBillingNoteMapper noteMapper;

    /** 外部计费同步 Mapper */
    private final SaasBillingSyncMapper syncMapper;

    /** 外部财务适配器列表 */
    private final List<BillingIntegrationAdapter> adapters;

    /** 统一审计服务 */
    private final AuditApplicationService auditService;

    /**
     * 构造函数注入所有依赖 Mapper 与适配器组件。
     */
    public SaasBillingLifecycleService(SaasBillingPeriodMapper periodMapper,
                                       SaasInvoiceMapper invoiceMapper,
                                       SaasInvoiceLineMapper lineMapper,
                                       SaasUsageEventMapper usageMapper,
                                       SaasBillingNoteMapper noteMapper,
                                       SaasBillingSyncMapper syncMapper,
                                       List<BillingIntegrationAdapter> adapters,
                                       AuditApplicationService auditService) {
        this.periodMapper = periodMapper;
        this.invoiceMapper = invoiceMapper;
        this.lineMapper = lineMapper;
        this.usageMapper = usageMapper;
        this.noteMapper = noteMapper;
        this.syncMapper = syncMapper;
        this.adapters = adapters;
        this.auditService = auditService;
    }

    /**
     * 查询当前租户账期、账单、调整项和外部同步状态。
     *
     * @param actor 当前操作用户
     * @return 财务工作台快照
     */
    public BillingOverview overview(SecurityUser actor) {
        requireRole(actor, BILLING_VIEW_ROLES, "当前账号没有查看账单与成本信息的权限。");
        Long tenantId = actor.getTenantId();
        return new BillingOverview(
                periodMapper.selectList(new LambdaQueryWrapper<SaasBillingPeriodEntity>()
                        .eq(SaasBillingPeriodEntity::getTenantId, tenantId)
                        .orderByDesc(SaasBillingPeriodEntity::getPeriodStart)),
                invoiceMapper.selectList(new LambdaQueryWrapper<SaasInvoiceEntity>()
                        .eq(SaasInvoiceEntity::getTenantId, tenantId)
                        .orderByDesc(SaasInvoiceEntity::getCreatedAt)),
                noteMapper.selectList(new LambdaQueryWrapper<SaasBillingNoteEntity>()
                        .eq(SaasBillingNoteEntity::getTenantId, tenantId)
                        .orderByDesc(SaasBillingNoteEntity::getCreatedAt)),
                syncMapper.selectList(new LambdaQueryWrapper<SaasBillingSyncEntity>()
                        .eq(SaasBillingSyncEntity::getTenantId, tenantId)
                        .orderByDesc(SaasBillingSyncEntity::getCreatedAt)));
    }

    /**
     * 导出已确认账单的安全明细并记录财务审计。
     *
     * @param actor 当前操作用户
     * @param invoiceId 账单 ID
     * @return 可下载的逗号分隔文本
     */
    public BillingExport exportInvoice(SecurityUser actor, Long invoiceId) {
        requireRole(actor, BILLING_EXPORT_ROLES, "当前账号没有导出账单的财务权限。");
        SaasInvoiceEntity invoice = requireInvoice(actor.getTenantId(), invoiceId);
        if (!FINALIZED.equals(invoice.getStatus())) {
            throw new IllegalStateException("草稿账单尚未确认，不能导出正式财务文件。");
        }
        List<SaasInvoiceLineEntity> lines = lineMapper.selectList(new LambdaQueryWrapper<SaasInvoiceLineEntity>()
                .eq(SaasInvoiceLineEntity::getTenantId, actor.getTenantId())
                .eq(SaasInvoiceLineEntity::getInvoiceId, invoiceId)
                .orderByAsc(SaasInvoiceLineEntity::getId));
        StringBuilder content = new StringBuilder("功能,成本中心,数量,单位,单价,金额\r\n");
        for (SaasInvoiceLineEntity line : lines) {
            content.append(csv(line.getFeatureCode())).append(',').append(csv(line.getCostCenter())).append(',')
                    .append(line.getQuantity()).append(',').append(csv(line.getUnit())).append(',')
                    .append(line.getUnitPrice()).append(',').append(line.getAmount()).append("\r\n");
        }
        auditService.recordWorkflowAction(actor.getTenantId(), String.valueOf(actor.getUserId()),
                "BILLING_INVOICE_EXPORTED", invoiceId, Map.of("invoiceNumber", invoice.getInvoiceNumber(), "lineCount", lines.size()));
        return new BillingExport("账单-" + invoice.getInvoiceNumber() + ".csv", content.toString());
    }

    /**
     * 开启或获取指定租户在某时间段内的计费账期（Billing Period）。
     *
     * @param tenantId 租户 ID
     * @param start 账期起始日期
     * @param end 账期截止日期
     * @param currency 结算币种（如 CNY / USD）
     * @return 账期实体对象
     */
    @Transactional
    public SaasBillingPeriodEntity openPeriod(Long tenantId, LocalDate start, LocalDate end, String currency) {
        SaasBillingPeriodEntity existing = periodMapper.selectOne(new LambdaQueryWrapper<SaasBillingPeriodEntity>()
                .eq(SaasBillingPeriodEntity::getTenantId, tenantId)
                .eq(SaasBillingPeriodEntity::getPeriodStart, start)
                .eq(SaasBillingPeriodEntity::getPeriodEnd, end)
                .eq(SaasBillingPeriodEntity::getCurrency, currency));
        if (existing != null) {
            return existing;
        }

        SaasBillingPeriodEntity period = new SaasBillingPeriodEntity();
        period.setTenantId(tenantId);
        period.setPeriodStart(start);
        period.setPeriodEnd(end);
        period.setCurrency(currency);
        period.setPeriodStatus("OPEN");
        period.setCreatedAt(LocalDateTime.now());
        periodMapper.insert(period);
        return period;
    }

    /**
     * 根据账期内已记录的用量事实（Usage Events）自动聚合计算并生成草稿账单（Draft Invoice）与明细行。
     *
     * @param actor 当前操作用户
     * @param periodId 目标账期 ID
     * @return 草稿账单实体
     */
    @Transactional
    public SaasInvoiceEntity generateDraft(SecurityUser actor, Long periodId) {
        requireActor(actor);
        SaasBillingPeriodEntity period = requirePeriod(actor.getTenantId(), periodId);

        SaasInvoiceEntity existing = invoiceMapper.selectOne(new LambdaQueryWrapper<SaasInvoiceEntity>()
                .eq(SaasInvoiceEntity::getTenantId, actor.getTenantId())
                .eq(SaasInvoiceEntity::getBillingPeriodId, periodId));
        if (existing != null) {
            return existing;
        }

        List<SaasUsageEventEntity> events = usageMapper.selectList(new LambdaQueryWrapper<SaasUsageEventEntity>()
                .eq(SaasUsageEventEntity::getTenantId, actor.getTenantId())
                .ge(SaasUsageEventEntity::getOccurredAt, period.getPeriodStart().atStartOfDay())
                .lt(SaasUsageEventEntity::getOccurredAt, period.getPeriodEnd().plusDays(1).atStartOfDay()));

        Map<LineKey, LineTotal> totals = new LinkedHashMap<>();
        for (SaasUsageEventEntity event : events) {
            LineKey key = new LineKey(event.getFeatureCode(), event.getCostCenter(), event.getUnit(), event.getUnitPrice());
            LineTotal current = totals.getOrDefault(key, new LineTotal(BigDecimal.ZERO, BigDecimal.ZERO));
            totals.put(key, new LineTotal(
                    current.quantity().add(event.getQuantity()),
                    current.amount().add(event.getCostAmount() == null ? BigDecimal.ZERO : event.getCostAmount())
            ));
        }

        SaasInvoiceEntity invoice = new SaasInvoiceEntity();
        invoice.setTenantId(actor.getTenantId());
        invoice.setBillingPeriodId(periodId);
        invoice.setStatus(DRAFT);
        invoice.setCurrency(period.getCurrency());
        invoice.setSubtotal(totals.values().stream().map(LineTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        invoice.setTotal(invoice.getSubtotal());
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(invoice.getCreatedAt());
        invoiceMapper.insert(invoice);

        totals.forEach((key, total) -> {
            SaasInvoiceLineEntity line = new SaasInvoiceLineEntity();
            line.setTenantId(actor.getTenantId());
            line.setInvoiceId(invoice.getId());
            line.setFeatureCode(key.feature());
            line.setCostCenter(key.costCenter());
            line.setUnit(key.unit());
            line.setUnitPrice(key.unitPrice());
            line.setQuantity(total.quantity());
            line.setAmount(total.amount());
            line.setSourceSummary("账期内用量事件自动汇总数据");
            line.setCreatedAt(LocalDateTime.now());
            lineMapper.insert(line);
        });

        return invoice;
    }

    /**
     * 确认并关账冻结草稿账单（生成正式的关账单号 INV-yyyyMMdd-id，并不再允许修改或重新计算）。
     *
     * @param actor 当前操作用户
     * @param invoiceId 待关账冻结的账单 ID
     * @return 确认后的终态账单实体
     */
    @Transactional
    public SaasInvoiceEntity finalizeInvoice(SecurityUser actor, Long invoiceId) {
        requireActor(actor);
        SaasInvoiceEntity invoice = requireInvoice(actor.getTenantId(), invoiceId);
        if (FINALIZED.equals(invoice.getStatus())) {
            return invoice;
        }

        SaasBillingPeriodEntity period = requirePeriod(actor.getTenantId(), invoice.getBillingPeriodId());

        long unknown = usageMapper.selectCount(new LambdaQueryWrapper<SaasUsageEventEntity>()
                .eq(SaasUsageEventEntity::getTenantId, actor.getTenantId())
                .eq(SaasUsageEventEntity::getCostStatus, CostStatus.UNKNOWN.name())
                .ge(SaasUsageEventEntity::getOccurredAt, period.getPeriodStart().atStartOfDay())
                .lt(SaasUsageEventEntity::getOccurredAt, period.getPeriodEnd().plusDays(1).atStartOfDay()));

        if (unknown > 0 || invoice.getCurrency() == null || invoice.getCurrency().isBlank()) {
            throw new IllegalStateException("当前账期内存在成本状态未知的用量明细或结算币种缺失，无法确认生成最终账单。");
        }

        String number = "INV-" + period.getPeriodStart().toString().replace("-", "") + "-" + invoice.getId();
        invoiceMapper.update(null, new LambdaUpdateWrapper<SaasInvoiceEntity>()
                .eq(SaasInvoiceEntity::getTenantId, actor.getTenantId())
                .eq(SaasInvoiceEntity::getId, invoiceId)
                .eq(SaasInvoiceEntity::getStatus, DRAFT)
                .set(SaasInvoiceEntity::getStatus, FINALIZED)
                .set(SaasInvoiceEntity::getInvoiceNumber, number)
                .set(SaasInvoiceEntity::getFinalizedAt, LocalDateTime.now()));

        periodMapper.update(null, new LambdaUpdateWrapper<SaasBillingPeriodEntity>()
                .eq(SaasBillingPeriodEntity::getTenantId, actor.getTenantId())
                .eq(SaasBillingPeriodEntity::getId, period.getId())
                .set(SaasBillingPeriodEntity::getPeriodStatus, "FINALIZED")
                .set(SaasBillingPeriodEntity::getFinalizedAt, LocalDateTime.now()));

        return requireInvoice(actor.getTenantId(), invoiceId);
    }

    /**
     * 针对已确认的最终账单创建红字/蓝字调整单（Credit / Debit Note），用于冲抵或修正退款。
     *
     * @param actor 当前操作用户
     * @param command 调整单创建命令参数
     * @return 调整单实体
     */
    @Transactional
    public SaasBillingNoteEntity createNote(SecurityUser actor, NoteCommand command) {
        requireActor(actor);
        SaasInvoiceEntity invoice = requireInvoice(actor.getTenantId(), command.invoiceId());
        if (!FINALIZED.equals(invoice.getStatus())) {
            throw new IllegalStateException("只允许对已确认关账（FINALIZED）的账单创建红蓝字调整单。");
        }

        SaasBillingNoteEntity note = new SaasBillingNoteEntity();
        note.setTenantId(actor.getTenantId());
        note.setInvoiceId(command.invoiceId());
        note.setNoteType(command.noteType());
        note.setNoteNumber(command.noteType() + "-" + invoice.getId() + "-" + System.currentTimeMillis());
        note.setAmount(command.amount());
        note.setCurrency(invoice.getCurrency());
        note.setReason(command.reason());
        note.setCreatedBy(actor.getUserId());
        note.setCreatedAt(LocalDateTime.now());
        noteMapper.insert(note);
        return note;
    }

    /**
     * 将关账后的正式账单幂等同步至外部第三方财务/发票系统。
     *
     * @param actor 当前操作用户
     * @param invoiceId 目标账单 ID
     * @param adapterType 目标财务适配器类型（如 STRIPE / ERP）
     * @param idempotencyKey 幂等校验 Key
     * @return 外部同步记录实体
     */
    @Transactional
    public SaasBillingSyncEntity sync(SecurityUser actor, Long invoiceId, String adapterType, String idempotencyKey) {
        requireActor(actor);
        SaasInvoiceEntity invoice = requireInvoice(actor.getTenantId(), invoiceId);
        if (!FINALIZED.equals(invoice.getStatus())) {
            throw new IllegalStateException("只有处于确认终态（FINALIZED）的账单才可以同步至外部财务系统。");
        }

        SaasBillingSyncEntity existing = syncMapper.selectOne(new LambdaQueryWrapper<SaasBillingSyncEntity>()
                .eq(SaasBillingSyncEntity::getTenantId, actor.getTenantId())
                .eq(SaasBillingSyncEntity::getIdempotencyKey, idempotencyKey));
        if (existing != null) {
            return existing;
        }

        BillingIntegrationAdapter adapter = adapters.stream()
                .filter(item -> item.type().equals(adapterType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到配置的外部财务系统适配器：" + adapterType));

        BillingIntegrationAdapter.BillingSyncResult result = adapter.submit(
                new BillingIntegrationAdapter.BillingSyncRequest(
                        actor.getTenantId(),
                        invoiceId,
                        invoice.getInvoiceNumber(),
                        invoice.getCurrency(),
                        idempotencyKey
                )
        );

        SaasBillingSyncEntity sync = new SaasBillingSyncEntity();
        sync.setTenantId(actor.getTenantId());
        sync.setInvoiceId(invoiceId);
        sync.setAdapterType(adapterType);
        sync.setIdempotencyKey(idempotencyKey);
        sync.setSyncStatus(result.status().name());
        sync.setExternalReference(result.externalReference());
        sync.setSafeSummary(result.safeSummary());
        sync.setRetryCount(0);

        if (result.status() == BillingIntegrationAdapter.SyncStatus.UNKNOWN || result.status() == BillingIntegrationAdapter.SyncStatus.FAILED) {
            sync.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
        }
        if (result.status() == BillingIntegrationAdapter.SyncStatus.CONFIRMED) {
            sync.setConfirmedAt(LocalDateTime.now());
        }

        sync.setCreatedAt(LocalDateTime.now());
        sync.setUpdatedAt(sync.getCreatedAt());
        syncMapper.insert(sync);
        return sync;
    }

    /** 校验账期是否存在 */
    private SaasBillingPeriodEntity requirePeriod(Long tenantId, Long id) {
        SaasBillingPeriodEntity value = periodMapper.selectOne(new LambdaQueryWrapper<SaasBillingPeriodEntity>()
                .eq(SaasBillingPeriodEntity::getTenantId, tenantId)
                .eq(SaasBillingPeriodEntity::getId, id));
        if (value == null) {
            throw new IllegalArgumentException("未找到对应的计费账期记录。");
        }
        return value;
    }

    /** 校验账单是否存在 */
    private SaasInvoiceEntity requireInvoice(Long tenantId, Long id) {
        SaasInvoiceEntity value = invoiceMapper.selectOne(new LambdaQueryWrapper<SaasInvoiceEntity>()
                .eq(SaasInvoiceEntity::getTenantId, tenantId)
                .eq(SaasInvoiceEntity::getId, id));
        if (value == null) {
            throw new IllegalArgumentException("未找到对应的计费账单记录。");
        }
        return value;
    }

    /** 校验 Actor 非空 */
    private void requireActor(SecurityUser actor) {
        if (actor == null || actor.getTenantId() == null || actor.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份无效，请重新登录。");
        }
    }

    /** 校验财务角色边界 */
    private void requireRole(SecurityUser actor, Set<String> allowedRoles, String message) {
        requireActor(actor);
        if (actor.getRoles().stream().noneMatch(allowedRoles::contains)) {
            throw new AuthorizationDeniedException(message);
        }
    }

    /** 转义逗号分隔文本字段 */
    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    /** 内部行键组装 Record */
    private record LineKey(String feature, String costCenter, String unit, BigDecimal unitPrice) { }

    /** 内部行小计 Record */
    private record LineTotal(BigDecimal quantity, BigDecimal amount) { }

    /** 账单调整单命令 Record */
    public record NoteCommand(Long invoiceId, String noteType, BigDecimal amount, String reason) { }

    /** 财务工作台快照 */
    public record BillingOverview(List<SaasBillingPeriodEntity> periods, List<SaasInvoiceEntity> invoices,
                                  List<SaasBillingNoteEntity> notes, List<SaasBillingSyncEntity> syncs) { }

    /** 账单导出结果 */
    public record BillingExport(String fileName, String content) { }
}

