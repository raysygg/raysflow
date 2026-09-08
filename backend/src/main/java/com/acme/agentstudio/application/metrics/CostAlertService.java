package com.acme.agentstudio.application.metrics;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.CostAlertEventEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelCallMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.TenantCostAlertRuleEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.CostAlertEventMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelCallMetricMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.TenantCostAlertRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户大模型（LLM）API 消费成本月度预算告警服务。
 * 负责通过后台定时扫描，汇总租户当月大模型调用的真实预估花费（Estimated Cost），超过规则设定的阈值百分比时自动触发并落库生成状态为 OPEN 的 CostAlertEvent 告警事件。
 */
@Service
public class CostAlertService {

    /** 租户成本告警规则 Persistence Mapper */
    private final TenantCostAlertRuleMapper ruleMapper;

    /** 模型调用指标 Persistence Mapper */
    private final ModelCallMetricMapper metricMapper;

    /** 成本告警事件 Persistence Mapper */
    private final CostAlertEventMapper eventMapper;

    /**
     * 构造函数注入规则、指标与事件 Persistence 组件。
     */
    public CostAlertService(TenantCostAlertRuleMapper ruleMapper, ModelCallMetricMapper metricMapper, CostAlertEventMapper eventMapper) {
        this.ruleMapper = ruleMapper;
        this.metricMapper = metricMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * 定时任务：扫描处于活动状态的租户成本告警规则，判断当月累计花销是否突破预警阈值。
     */
    @Scheduled(fixedDelayString = "${app.cost-alert.scan-delay-ms:60000}")
    public void scan() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        for (TenantCostAlertRuleEntity rule : ruleMapper.selectList(new LambdaQueryWrapper<TenantCostAlertRuleEntity>()
                .eq(TenantCostAlertRuleEntity::getStatus, BusinessStatus.ACTIVE))) {

            List<ModelCallMetricEntity> metrics = metricMapper.selectList(new LambdaQueryWrapper<ModelCallMetricEntity>()
                    .eq(ModelCallMetricEntity::getTenantId, rule.getTenantId())
                    .ge(ModelCallMetricEntity::getCreatedAt, monthStart)
                    .isNotNull(ModelCallMetricEntity::getEstimatedCost));

            BigDecimal current = metrics.stream()
                    .map(ModelCallMetricEntity::getEstimatedCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal threshold = rule.getMonthlyCostLimit()
                    .multiply(rule.getAlertPercent())
                    .divide(BigDecimal.valueOf(100));

            if (current.compareTo(threshold) >= 0 && eventMapper.selectCount(new LambdaQueryWrapper<CostAlertEventEntity>()
                    .eq(CostAlertEventEntity::getTenantId, rule.getTenantId())
                    .eq(CostAlertEventEntity::getAlertType, "MONTHLY_COST")
                    .ge(CostAlertEventEntity::getCreatedAt, monthStart)) == 0) {

                CostAlertEventEntity event = new CostAlertEventEntity();
                event.setTenantId(rule.getTenantId());
                event.setAlertType("MONTHLY_COST");
                event.setCurrentCost(current);
                event.setLimitCost(rule.getMonthlyCostLimit());
                event.setStatus("OPEN");
                event.setCreatedAt(LocalDateTime.now());
                eventMapper.insert(event);
            }
        }
    }
}

