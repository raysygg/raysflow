package com.acme.agentstudio.application.saas;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.CostStatus;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.UsageAdjustmentType;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.UsageSource;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelPriceEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformPriceVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasUsageAdjustmentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SaasUsageEventEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelPriceMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformPriceVersionMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasUsageAdjustmentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SaasUsageEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * SaaS 租户用量账本（Usage Ledger）与计费成本计算服务。
 * 负责接收大模型及工具调用的真实 Usage 事实事件（只读追加 append），匹配平台/模型单价版本（Price Version），记录评估计算成本（CALCULATED/ESTIMATED/UNKNOWN）及人工调整明细（Adjustment）。
 */
@Service
public class SaasUsageLedgerService {

    /** 状态标识：活动中 */
    private static final String ACTIVE = "ACTIVE";

    /** 计费用量事件 Mapper */
    private final SaasUsageEventMapper usageMapper;

    /** 计费用量调整单 Mapper */
    private final SaasUsageAdjustmentMapper adjustmentMapper;

    /** 平台计费价格版本 Mapper */
    private final PlatformPriceVersionMapper platformPriceMapper;

    /** 模型基准价格 Mapper */
    private final ModelPriceMapper modelPriceMapper;

    /**
     * 构造函数注入用量账本相关依赖。
     */
    public SaasUsageLedgerService(SaasUsageEventMapper usageMapper,
                                  SaasUsageAdjustmentMapper adjustmentMapper,
                                  PlatformPriceVersionMapper platformPriceMapper,
                                  ModelPriceMapper modelPriceMapper) {
        this.usageMapper = usageMapper;
        this.adjustmentMapper = adjustmentMapper;
        this.platformPriceMapper = platformPriceMapper;
        this.modelPriceMapper = modelPriceMapper;
    }

    /**
     * 追加写入一条计费用量事件记录（支持基于 idempotencyKey 的幂等去重）。
     *
     * @param command 用量事件提交命令
     * @return 插入或已存在的用量事件实体
     */
    @Transactional
    public SaasUsageEventEntity append(UsageCommand command) {
        if (command == null || command.tenantId() == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("提交用量事件时必须包含有效的租户 ID 与幂等校验 Key。");
        }

        SaasUsageEventEntity existing = usageMapper.selectOne(new LambdaQueryWrapper<SaasUsageEventEntity>()
                .eq(SaasUsageEventEntity::getTenantId, command.tenantId())
                .eq(SaasUsageEventEntity::getIdempotencyKey, command.idempotencyKey()));
        if (existing != null) {
            return existing;
        }

        UsageEvidence evidence = resolveEvidence(command);

        SaasUsageEventEntity entity = new SaasUsageEventEntity();
        entity.setTenantId(command.tenantId());
        entity.setApplicationId(command.applicationId());
        entity.setReleaseId(command.releaseId());
        entity.setRunId(command.runId());
        entity.setModelKey(command.modelKey());
        entity.setFeatureCode(command.featureCode());
        entity.setCostCenter(command.costCenter());
        entity.setIdempotencyKey(command.idempotencyKey());
        entity.setQuantity(command.quantity());
        entity.setUnit(command.unit());
        entity.setUsageSource(evidence.source().name());
        entity.setInputTokens(command.inputTokens());
        entity.setOutputTokens(command.outputTokens());
        entity.setPriceVersionId(evidence.priceVersionId());
        entity.setCurrency(evidence.currency());
        entity.setUnitPrice(evidence.unitPrice());
        entity.setCostAmount(evidence.costAmount());
        entity.setCostStatus(evidence.costStatus().name());
        entity.setCostReason(evidence.reason());
        entity.setOccurredAt(command.occurredAt() == null ? LocalDateTime.now() : command.occurredAt());
        entity.setCreatedAt(LocalDateTime.now());

        usageMapper.insert(entity);
        return entity;
    }

    /**
     * 对特定的历史用量事件进行补录、扣减或冲销调整（Adjustment）。
     *
     * @param actor 当前操作用户
     * @param command 用量调整命令
     * @return 调整单实体
     */
    @Transactional
    public SaasUsageAdjustmentEntity adjust(SecurityUser actor, AdjustmentCommand command) {
        if (actor == null || actor.getTenantId() == null || command == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("用量调整事件参数无效，请检查登录状态与幂等 Key。");
        }

        SaasUsageAdjustmentEntity existing = adjustmentMapper.selectOne(new LambdaQueryWrapper<SaasUsageAdjustmentEntity>()
                .eq(SaasUsageAdjustmentEntity::getTenantId, actor.getTenantId())
                .eq(SaasUsageAdjustmentEntity::getIdempotencyKey, command.idempotencyKey()));
        if (existing != null) {
            return existing;
        }

        SaasUsageAdjustmentEntity entity = new SaasUsageAdjustmentEntity();
        entity.setTenantId(actor.getTenantId());
        entity.setUsageEventId(command.usageEventId());
        entity.setAdjustmentType(command.type().name());
        entity.setQuantity(command.quantity());
        entity.setAmount(command.amount());
        entity.setCurrency(command.currency());
        entity.setReason(command.reason());
        entity.setIdempotencyKey(command.idempotencyKey());
        entity.setCreatedBy(actor.getUserId());
        entity.setCreatedAt(LocalDateTime.now());

        adjustmentMapper.insert(entity);
        return entity;
    }

    /**
     * 按多维过滤条件汇总查询租户在给定时间段内的用量与成本消耗。
     *
     * @param tenantId 租户 ID
     * @param applicationId 可选的应用 ID
     * @param modelKey 可选的模型标识
     * @param costCenter 可选的成本中心
     * @param from 起始时间
     * @param to 截止时间
     * @return 包含事件数、总用量、总成本与未知成本计数的 UsageSummary 对象
     */
    public UsageSummary aggregate(Long tenantId, Long applicationId, String modelKey, String costCenter,
                                  LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<SaasUsageEventEntity> query = new LambdaQueryWrapper<SaasUsageEventEntity>()
                .eq(SaasUsageEventEntity::getTenantId, tenantId)
                .eq(applicationId != null, SaasUsageEventEntity::getApplicationId, applicationId)
                .eq(modelKey != null && !modelKey.isBlank(), SaasUsageEventEntity::getModelKey, modelKey)
                .eq(costCenter != null && !costCenter.isBlank(), SaasUsageEventEntity::getCostCenter, costCenter)
                .ge(from != null, SaasUsageEventEntity::getOccurredAt, from)
                .lt(to != null, SaasUsageEventEntity::getOccurredAt, to);

        List<SaasUsageEventEntity> events = usageMapper.selectList(query);

        BigDecimal quantity = events.stream()
                .map(SaasUsageEventEntity::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cost = events.stream()
                .map(SaasUsageEventEntity::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unknown = events.stream()
                .filter(item -> CostStatus.UNKNOWN.name().equals(item.getCostStatus()))
                .count();

        return new UsageSummary(events.size(), quantity, cost, unknown);
    }

    /**
     * 根据用量数据和当前生效的价格版本推算成本证据链（UsageEvidence）。
     */
    private UsageEvidence resolveEvidence(UsageCommand command) {
        Integer input = command.inputTokens();
        Integer output = command.outputTokens();
        UsageSource source = command.providerUsageAvailable()
                ? UsageSource.PROVIDER
                : (input != null || output != null ? UsageSource.ESTIMATED : UsageSource.UNKNOWN);

        LocalDateTime occurTime = command.occurredAt() == null ? LocalDateTime.now() : command.occurredAt();

        PlatformPriceVersionEntity price = platformPriceMapper.selectOne(new LambdaQueryWrapper<PlatformPriceVersionEntity>()
                .eq(PlatformPriceVersionEntity::getTenantId, command.tenantId())
                .eq(PlatformPriceVersionEntity::getModelCode, command.modelKey())
                .eq(PlatformPriceVersionEntity::getStatus, ACTIVE)
                .le(PlatformPriceVersionEntity::getValidFrom, occurTime)
                .and(wrapper -> wrapper.isNull(PlatformPriceVersionEntity::getValidUntil).or().gt(PlatformPriceVersionEntity::getValidUntil, occurTime))
                .orderByDesc(PlatformPriceVersionEntity::getValidFrom)
                .last("LIMIT 1"));

        BigDecimal unitPrice = price == null ? null : price.getInputPrice();
        String currency = price == null ? null : price.getCurrency();
        Long priceId = price == null ? null : price.getId();

        if (price == null) {
            ModelPriceEntity fallback = modelPriceMapper.selectOne(new LambdaQueryWrapper<ModelPriceEntity>()
                    .eq(ModelPriceEntity::getModelKey, command.modelKey())
                    .eq(ModelPriceEntity::getStatus, ACTIVE)
                    .orderByDesc(ModelPriceEntity::getEffectiveFrom)
                    .last("LIMIT 1"));
            if (fallback != null) {
                unitPrice = fallback.getInputPricePer1k();
                priceId = fallback.getId();
                currency = "CNY";
            }
        }

        if (unitPrice == null || currency == null || source == UsageSource.UNKNOWN) {
            return new UsageEvidence(source, priceId, currency, unitPrice, null, CostStatus.UNKNOWN, "缺少可靠的供应商 usage 或价格版本信息。");
        }

        BigDecimal amount = command.quantity().multiply(unitPrice).setScale(10, RoundingMode.HALF_UP);
        return new UsageEvidence(
                source,
                priceId,
                currency,
                unitPrice,
                amount,
                source == UsageSource.ESTIMATED ? CostStatus.ESTIMATED : CostStatus.CALCULATED,
                null
        );
    }

    /** 用量提交 Command */
    public record UsageCommand(
            Long tenantId,
            Long applicationId,
            String releaseId,
            String runId,
            String modelKey,
            String featureCode,
            String costCenter,
            String idempotencyKey,
            BigDecimal quantity,
            String unit,
            boolean providerUsageAvailable,
            Integer inputTokens,
            Integer outputTokens,
            LocalDateTime occurredAt
    ) { }

    /** 用量调整 Command */
    public record AdjustmentCommand(
            Long usageEventId,
            UsageAdjustmentType type,
            BigDecimal quantity,
            BigDecimal amount,
            String currency,
            String reason,
            String idempotencyKey
    ) { }

    /** 内部成本证据链对象 */
    private record UsageEvidence(
            UsageSource source,
            Long priceVersionId,
            String currency,
            BigDecimal unitPrice,
            BigDecimal costAmount,
            CostStatus costStatus,
            String reason
    ) { }

    /** 用量汇总数据 Record */
    public record UsageSummary(
            long eventCount,
            BigDecimal quantity,
            BigDecimal cost,
            long unknownCostCount
    ) { }
}

