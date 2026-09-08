package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * ModelCallMetric 数据库持久化实体对象。
 * 对应数据库中 ModelCallMetric 数据表的字段结构映射。
 */
@Data
@TableName("model_call_metric")
/**
 * 数据库实体：映射表 `model_call_metric`，承载 ModelCallMetric 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * ModelCallMetric 数据表持久化实体类。
 * 映射数据库对应的 ModelCallMetric 表结构。
 */
public class ModelCallMetricEntity {
    /** 模型调用观测记录。estimatedCost 只有在真实 Token 和价格均可用时才有值。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** agent 主键 ID 标识 属性 */
    private Long agentId;
    /** 模型唯一标识 Key */
    private String modelKey;
    /** call 业务分类类型 属性 */
    private String callType;
    /** input Tokens 属性 */
    private Long inputTokens;
    /** output Tokens 属性 */
    private Long outputTokens;
    /** latency Ms 属性 */
    private Long latencyMs;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** error Message 属性 */
    private String errorMessage;
    /** estimated Cost 属性 */
    private BigDecimal estimatedCost;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
