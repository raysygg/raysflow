package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ModelPrice 数据库持久化实体对象。
 * 对应数据库中 ModelPrice 数据表的字段结构映射。
 */
@Data
@TableName("model_price")
/**
 * 数据库实体：映射表 `model_price`，承载 ModelPrice 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * ModelPrice 数据表持久化实体类。
 * 映射数据库对应的 ModelPrice 表结构。
 */
public class ModelPriceEntity {
    /** 价格规则主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模型供应商。 */
    private String provider;
    /** 真实调用使用的模型名称。 */
    private String modelKey;
    /** 每一千个输入 Token 的价格。 */
    @TableField("input_price_per_1k")
    private BigDecimal inputPricePer1k;
    /** 每一千个输出 Token 的价格。 */
    @TableField("output_price_per_1k")
    private BigDecimal outputPricePer1k;
    /** 价格开始生效的时间，用于支持历史价格。 */
    private LocalDateTime effectiveFrom;
    /** ACTIVE 表示可用于成本计算。 */
    private String status;
}
