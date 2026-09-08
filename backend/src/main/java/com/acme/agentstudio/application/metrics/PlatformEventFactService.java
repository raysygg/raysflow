package com.acme.agentstudio.application.metrics;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformEventFactEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformPriceVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformEventFactMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformPriceVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台运行事件事实表（Platform Event Fact）记录与模型价格体系匹配服务。
 * 用于统一审计落库工作流执行、Agent 对话、模型调用的 Token 消耗、响应延迟与计费事实，并根据租户定制或平台公共策略查询模型生效的价格版本。
 */
@Service
public class PlatformEventFactService {

    /** 平台事件事实表 Persistence Mapper */
    private final PlatformEventFactMapper factMapper;

    /** 平台模型价格版本 Persistence Mapper */
    private final PlatformPriceVersionMapper priceMapper;

    /** JSON 序列化映射工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入事件事实表 Mapper、价格版本 Mapper 与 JSON 序列化工具。
     */
    public PlatformEventFactService(PlatformEventFactMapper factMapper, PlatformPriceVersionMapper priceMapper, ObjectMapper objectMapper) {
        this.factMapper = factMapper;
        this.priceMapper = priceMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一条平台运行事件事实数据（包含耗时、Token 消耗及折算扣费金额）。
     *
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param eventType 事件类型编码
     * @param resultStatus 执行结果状态（如 SUCCESS / FAILED）
     * @param tokenCount 消耗的 Token 数量
     * @param latencyMs 调用响应耗时（毫秒）
     * @param modelCode 调用的模型编码
     * @param cost 本次调用的扣费金额
     * @param payload 事件上下文 Payload 属性映射字典
     */
    public void record(Long tenantId, Long userId, String eventType, String resultStatus, Long tokenCount,
                       Long latencyMs, String modelCode, BigDecimal cost, Map<String, Object> payload) {
        PlatformEventFactEntity fact = new PlatformEventFactEntity();
        fact.setTenantId(tenantId);
        fact.setUserId(userId);
        fact.setEventType(eventType);
        fact.setResultStatus(resultStatus);
        fact.setTokenCount(tokenCount == null ? 0 : tokenCount);
        fact.setLatencyMs(latencyMs == null ? 0 : latencyMs);
        fact.setCostAmount(cost);
        fact.setOccurredAt(LocalDateTime.now());
        try {
            fact.setPayloadJson(objectMapper.writeValueAsString(payload == null ? Map.of() : payload));
        } catch (Exception ex) {
            throw new IllegalArgumentException("事件 Payload 参数格式无效，无法序列化 JSON。", ex);
        }
        factMapper.insert(fact);
    }

    /**
     * 查询指定租户最近 N 天内的事件事实日志列表（按发生时间倒序排列）。
     *
     * @param tenantId 租户 ID
     * @param days 查询时间跨度天数
     * @return 平台事件事实实体列表
     */
    public List<PlatformEventFactEntity> list(Long tenantId, int days) {
        return factMapper.selectList(new LambdaQueryWrapper<PlatformEventFactEntity>()
                .eq(PlatformEventFactEntity::getTenantId, tenantId)
                .ge(PlatformEventFactEntity::getOccurredAt, LocalDateTime.now().minusDays(Math.max(days, 1)))
                .orderByDesc(PlatformEventFactEntity::getOccurredAt));
    }

    /**
     * 查询适合指定租户且当前处于有效时间范围内的模型计费价格版本规则（优先匹配租户专属单价）。
     *
     * @param tenantId 租户 ID
     * @param modelCode 大模型编码标识
     * @return 匹配的的计费价格版本实体
     */
    public PlatformPriceVersionEntity price(Long tenantId, String modelCode) {
        LocalDateTime now = LocalDateTime.now();
        return priceMapper.selectOne(new LambdaQueryWrapper<PlatformPriceVersionEntity>()
                .and(query -> query.isNull(PlatformPriceVersionEntity::getTenantId)
                        .or().eq(PlatformPriceVersionEntity::getTenantId, tenantId))
                .eq(PlatformPriceVersionEntity::getModelCode, modelCode)
                .eq(PlatformPriceVersionEntity::getStatus, BusinessStatus.ACTIVE)
                .le(PlatformPriceVersionEntity::getValidFrom, now)
                .and(query -> query.isNull(PlatformPriceVersionEntity::getValidUntil)
                        .or().ge(PlatformPriceVersionEntity::getValidUntil, now))
                .orderByDesc(PlatformPriceVersionEntity::getTenantId)
                .orderByDesc(PlatformPriceVersionEntity::getValidFrom)
                .last("LIMIT 1"));
    }
}

