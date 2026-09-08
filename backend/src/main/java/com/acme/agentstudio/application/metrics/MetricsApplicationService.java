package com.acme.agentstudio.application.metrics;

import com.acme.agentstudio.domain.metrics.model.ModelUsageBreakdown;
import com.acme.agentstudio.domain.metrics.model.ModelUsageSummary;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelCallMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelCallMetricMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 运营控制台模型用量与成本大盘分析应用服务。
 * 汇总指定租户在设定期限内的总调用次数、成功率、输入/输出 Token 累计量以及按 Model 模型编码、Agent 智能体或 Date 日期多维度的统计下钻与成本预估。
 */
@Service
public class MetricsApplicationService {

    /** 大模型 API 调用指标 Persistence Mapper */
    private final ModelCallMetricMapper metricMapper;

    /**
     * 构造函数注入模型指标 Mapper 依赖。
     */
    public MetricsApplicationService(ModelCallMetricMapper metricMapper) {
        this.metricMapper = metricMapper;
    }

    /**
     * 查询指定租户在过去 N 天内的模型用量与预估消费汇总大盘。
     *
     * @param tenantId 租户 ID
     * @param days 天数范围（1 - 90 天）
     * @return 包含成功数、失败数、Token 累计数与总预估成本的聚合对象
     */
    public ModelUsageSummary summary(Long tenantId, int days) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户标识不能为空。");
        }
        int normalizedDays = Math.max(1, Math.min(days, 90));
        LocalDateTime from = LocalDateTime.now().minusDays(normalizedDays);

        List<ModelCallMetricEntity> metrics = metricMapper.selectList(new LambdaQueryWrapper<ModelCallMetricEntity>()
                .eq(ModelCallMetricEntity::getTenantId, tenantId)
                .ge(ModelCallMetricEntity::getCreatedAt, from)
                .orderByDesc(ModelCallMetricEntity::getCreatedAt));

        long successCalls = metrics.stream()
                .filter(item -> "SUCCESS".equals(item.getStatus()))
                .count();

        long failedCalls = metrics.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .count();

        long inputTokens = metrics.stream()
                .map(ModelCallMetricEntity::getInputTokens)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        long outputTokens = metrics.stream()
                .map(ModelCallMetricEntity::getOutputTokens)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        List<BigDecimal> costs = metrics.stream()
                .map(ModelCallMetricEntity::getEstimatedCost)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal totalCost = costs.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ModelUsageSummary(
                from,
                metrics.size(),
                successCalls,
                failedCalls,
                inputTokens,
                outputTokens,
                totalCost,
                !costs.isEmpty()
        );
    }

    /**
     * 按维度（MODEL 模型编码 / AGENT 智能体 / DATE 日期）多维下钻拆分用量与成本分布。
     *
     * @param tenantId 租户 ID
     * @param days 天数范围（1 - 90 天）
     * @param dimension 聚合下钻维度（MODEL / AGENT / DATE）
     * @return 维度下钻明细项列表
     */
    public List<ModelUsageBreakdown> breakdown(Long tenantId, int days, String dimension) {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户标识不能为空。");
        }
        if (!List.of("MODEL", "AGENT", "DATE").contains(dimension)) {
            throw new IllegalArgumentException("统计下钻维度只支持 MODEL、AGENT 或 DATE。");
        }
        int normalizedDays = Math.max(1, Math.min(days, 90));
        LocalDateTime from = LocalDateTime.now().minusDays(normalizedDays);

        List<ModelCallMetricEntity> metrics = metricMapper.selectList(new LambdaQueryWrapper<ModelCallMetricEntity>()
                .eq(ModelCallMetricEntity::getTenantId, tenantId)
                .ge(ModelCallMetricEntity::getCreatedAt, from)
                .orderByDesc(ModelCallMetricEntity::getCreatedAt));

        Map<String, List<ModelCallMetricEntity>> grouped = metrics.stream()
                .collect(Collectors.groupingBy(
                        item -> dimensionValue(item, dimension),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ModelUsageBreakdown> result = new ArrayList<>();
        grouped.forEach((key, items) -> {
            long success = items.stream()
                    .filter(item -> "SUCCESS".equals(item.getStatus()))
                    .count();

            long failed = items.stream()
                    .filter(item -> "FAILED".equals(item.getStatus()))
                    .count();

            long input = items.stream()
                    .map(ModelCallMetricEntity::getInputTokens)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();

            long output = items.stream()
                    .map(ModelCallMetricEntity::getOutputTokens)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();

            BigDecimal cost = items.stream()
                    .map(ModelCallMetricEntity::getEstimatedCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.add(new ModelUsageBreakdown(key, items.size(), success, failed, input, output, cost));
        });
        return result;
    }

    /**
     * 解析给定 ModelCallMetric 实体在特定维度下的 Key 值。
     */
    private String dimensionValue(ModelCallMetricEntity item, String dimension) {
        if ("MODEL".equals(dimension)) {
            return (item.getModelKey() == null || item.getModelKey().isBlank()) ? "未记录模型" : item.getModelKey();
        }
        if ("AGENT".equals(dimension)) {
            return item.getAgentId() == null ? "非 Agent 调用" : String.valueOf(item.getAgentId());
        }
        return item.getCreatedAt() == null ? "未知日期" : item.getCreatedAt().toLocalDate().toString();
    }
}

