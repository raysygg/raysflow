package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EvaluationResult 数据库持久化实体对象。
 * 对应数据库中 EvaluationResult 数据表的字段结构映射。
 */
@Data
@TableName("evaluation_result")
/**
 * 数据库实体：映射表 `evaluation_result`，承载 EvaluationResult 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * EvaluationResult 数据表持久化实体类。
 * 映射数据库对应的 EvaluationResult 表结构。
 */
public class EvaluationResultEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** task 主键 ID 标识 属性 */
    private Long taskId;
    /** sample 主键 ID 标识 属性 */
    private Long sampleId;
    /** question 属性 */
    private String question;
    /** expected Answer 属性 */
    private String expectedAnswer;
    /** actual Answer 属性 */
    private String actualAnswer;
    /** score 属性 */
    private BigDecimal score;
    /** latency Ms 属性 */
    private Long latencyMs;
    /** result 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String resultStatus;
    /** error Message 属性 */
    private String errorMessage;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
