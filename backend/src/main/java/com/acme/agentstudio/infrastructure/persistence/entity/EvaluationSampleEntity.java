package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * EvaluationSample 数据库持久化实体对象。
 * 对应数据库中 EvaluationSample 数据表的字段结构映射。
 */
@Data
@TableName("evaluation_sample")
/**
 * 数据库实体：映射表 `evaluation_sample`，承载 EvaluationSample 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * EvaluationSample 数据表持久化实体类。
 * 映射数据库对应的 EvaluationSample 表结构。
 */
public class EvaluationSampleEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** question 属性 */
    private String question;
    /** expected Answer 属性 */
    private String expectedAnswer;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
