package com.acme.agentstudio.application.ops;

import com.acme.agentstudio.domain.ops.model.OpsSnapshot;
import com.acme.agentstudio.infrastructure.persistence.entity.AuditLogEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelCallMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RagRetrievalMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AuditLogMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelCallMetricMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagRetrievalMetricMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 平台级 AI 运维监控大盘（Ops Console Snapshot）看板服务。
 * 负责实时统计租户当天的模型调用总数、平均延迟、失败率、RAG 向量检索命中率，并汇总风险安全审计日志。
 */
@Service
public class OpsApplicationService {

    /** 审计日志 Persistence Mapper */
    private final AuditLogMapper auditLogMapper;

    /** 大模型调用指标 Persistence Mapper */
    private final ModelCallMetricMapper modelCallMetricMapper;

    /** RAG 检索指标 Persistence Mapper */
    private final RagRetrievalMetricMapper ragRetrievalMetricMapper;

    /**
     * 构造函数注入审计日志 Mapper、模型指标 Mapper 与 RAG 检索 Mapper 依赖。
     */
    public OpsApplicationService(
            AuditLogMapper auditLogMapper,
            ModelCallMetricMapper modelCallMetricMapper,
            RagRetrievalMetricMapper ragRetrievalMetricMapper
    ) {
        this.auditLogMapper = auditLogMapper;
        this.modelCallMetricMapper = modelCallMetricMapper;
        this.ragRetrievalMetricMapper = ragRetrievalMetricMapper;
    }

    /**
     * 获取指定租户当天 00:00 至今的实时运维监控大盘快照数据。
     *
     * @param tenantId 租户 ID
     * @return 包含当日调用量、平均耗时、失败率、RAG 命中率与安全风险事件列表的 OpsSnapshot 实体
     */
    public OpsSnapshot getSnapshot(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户标识不能为空。");
        }
        LocalDateTime today = LocalDate.now().atStartOfDay();

        List<ModelCallMetricEntity> metrics = modelCallMetricMapper.selectList(new LambdaQueryWrapper<ModelCallMetricEntity>()
                .eq(ModelCallMetricEntity::getTenantId, tenantId)
                .ge(ModelCallMetricEntity::getCreatedAt, today));

        long successCount = metrics.stream()
                .filter(item -> "SUCCESS".equals(item.getStatus()))
                .count();

        long failedCount = metrics.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .count();

        long totalCount = successCount + failedCount;

        BigDecimal averageLatency = metrics.stream()
                .map(ModelCallMetricEntity::getLatencyMs)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String averageLatencyText = totalCount == 0
                ? "暂无数据"
                : averageLatency.divide(BigDecimal.valueOf(totalCount), 0, RoundingMode.HALF_UP) + " ms";

        String failureRateText = totalCount == 0
                ? "暂无数据"
                : BigDecimal.valueOf(failedCount * 100L)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) + "%";

        List<RagRetrievalMetricEntity> ragMetrics = ragRetrievalMetricMapper.selectList(new LambdaQueryWrapper<RagRetrievalMetricEntity>()
                .eq(RagRetrievalMetricEntity::getTenantId, tenantId)
                .ge(RagRetrievalMetricEntity::getCreatedAt, today));

        String ragHitRateText;
        if (ragMetrics.isEmpty()) {
            ragHitRateText = "暂无数据";
        } else {
            long hitCount = ragMetrics.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getHit()))
                    .count();
            ragHitRateText = BigDecimal.valueOf(hitCount * 100L)
                    .divide(BigDecimal.valueOf(ragMetrics.size()), 2, RoundingMode.HALF_UP) + "%";
        }

        List<AuditLogEntity> auditLogs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getTenantId, tenantId)
                .ge(AuditLogEntity::getCreatedAt, today));

        List<String> auditEvents = auditLogs.stream()
                .map(item -> item.getRiskLevel() + " " + item.getActionType() + " " + item.getTargetType() + ":" + item.getTargetId())
                .toList();

        return new OpsSnapshot(
                String.valueOf(metrics.size()),
                averageLatencyText,
                ragHitRateText,
                failureRateText,
                auditEvents
        );
    }
}

