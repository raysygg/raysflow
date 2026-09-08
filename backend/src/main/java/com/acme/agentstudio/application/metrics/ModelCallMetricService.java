package com.acme.agentstudio.application.metrics;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelCallMetricEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.ModelPriceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelCallMetricMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.ModelPriceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 大模型 API 调用指标明细监控与计费计算服务。
 * 负责记录每次 LLM 调用的成功/失败状态、耗时毫秒数、提取并记录输入/输出 Token 数量，并依据生效的价格规则实时折算预估消费成本。
 */
@Service
public class ModelCallMetricService {

    /** 模型调用指标 Persistence Mapper */
    private final ModelCallMetricMapper metricMapper;

    /** 模型单价配置 Persistence Mapper */
    private final ModelPriceMapper priceMapper;

    /**
     * 构造函数注入指标 Mapper 与单价 Mapper 依赖。
     */
    public ModelCallMetricService(ModelCallMetricMapper metricMapper, ModelPriceMapper priceMapper) {
        this.metricMapper = metricMapper;
        this.priceMapper = priceMapper;
    }

    /**
     * 记录一次成功的 LLM 大模型调用指标（提取 Token 统计并折算消费金额）。
     *
     * @param tenantId 租户 ID
     * @param agentId Agent ID
     * @param modelKey 模型编码
     * @param callType 调用场景类型（如 CHAT_PREVIEW / WORKFLOW_NODE）
     * @param latencyMs 调用耗时毫秒
     * @param response LLM 响应结果对象（兼容 LangChain4j Response）
     */
    public void recordSuccess(Long tenantId, Long agentId, String modelKey, String callType, long latencyMs, Object response) {
        ModelCallMetricEntity metric = base(tenantId, agentId, modelKey, callType, latencyMs);
        metric.setStatus("SUCCESS");
        metric.setInputTokens(readToken(response, "inputTokenCount"));
        metric.setOutputTokens(readToken(response, "outputTokenCount"));
        metric.setEstimatedCost(calculateCost(metric));
        metricMapper.insert(metric);
    }

    /**
     * 记录一次失败的 LLM 大模型调用指标（保存捕获的错误异常提示）。
     *
     * @param tenantId 租户 ID
     * @param agentId Agent ID
     * @param modelKey 模型编码
     * @param callType 调用场景类型
     * @param latencyMs 调用耗时毫秒
     * @param error 捕获到的异常对象
     */
    public void recordFailure(Long tenantId, Long agentId, String modelKey, String callType, long latencyMs, Exception error) {
        ModelCallMetricEntity metric = base(tenantId, agentId, modelKey, callType, latencyMs);
        metric.setStatus("FAILED");
        metric.setErrorMessage(error != null ? error.getMessage() : "未知异常");
        metricMapper.insert(metric);
    }

    /**
     * 构建基础模型调用实体。
     */
    private ModelCallMetricEntity base(Long tenantId, Long agentId, String modelKey, String callType, long latencyMs) {
        ModelCallMetricEntity metric = new ModelCallMetricEntity();
        metric.setTenantId(tenantId);
        metric.setAgentId(agentId);
        metric.setModelKey(modelKey);
        metric.setCallType(callType);
        metric.setLatencyMs(latencyMs);
        metric.setCreatedAt(LocalDateTime.now());
        return metric;
    }

    /**
     * 反射读取 LangChain4j 或通用 Response Metadata 中的 Token 统计数值。
     */
    private Long readToken(Object response, String methodName) {
        if (response == null) {
            return null;
        }
        try {
            Method metadataMethod = response.getClass().getMethod("metadata");
            Object metadata = metadataMethod.invoke(response);
            if (metadata == null) {
                return null;
            }
            Method tokenMethod = metadata.getClass().getMethod(methodName);
            Object value = tokenMethod.invoke(metadata);
            return value instanceof Number number ? number.longValue() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 根据生效中的模型定价规则，按每 1k Token 计算预估消费成本。
     */
    private BigDecimal calculateCost(ModelCallMetricEntity metric) {
        if (metric.getInputTokens() == null || metric.getOutputTokens() == null) {
            return null;
        }
        ModelPriceEntity price = priceMapper.selectOne(new LambdaQueryWrapper<ModelPriceEntity>()
                .eq(ModelPriceEntity::getModelKey, metric.getModelKey())
                .eq(ModelPriceEntity::getStatus, BusinessStatus.ACTIVE)
                .le(ModelPriceEntity::getEffectiveFrom, LocalDateTime.now())
                .orderByDesc(ModelPriceEntity::getEffectiveFrom)
                .last("LIMIT 1"));
        if (price == null) {
            return null;
        }
        return price.getInputPricePer1k().multiply(BigDecimal.valueOf(metric.getInputTokens()))
                .add(price.getOutputPricePer1k().multiply(BigDecimal.valueOf(metric.getOutputTokens())))
                .divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
    }
}

