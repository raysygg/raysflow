package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * EvaluationTask 数据库持久化实体对象。
 * 对应数据库中 EvaluationTask 数据表的字段结构映射。
 */
@Data
@TableName("evaluation_task")
/**
 * 数据库实体：映射表 `evaluation_task`，承载 EvaluationTask 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * EvaluationTask 数据表持久化实体类。
 * 映射数据库对应的 EvaluationTask 表结构。
 */
public class EvaluationTaskEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** agent 主键 ID 标识 属性 */
    private Long agentId;
    /** task 展示名称 属性 */
    private String taskName;
    /** sample Count 属性 */
    private Integer sampleCount;
    /** scoring Rule 属性 */
    private String scoringRule;
    /** score 属性 */
    private BigDecimal score;
    /** evaluation 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String evaluationStatus;
    /** report Json 属性 */
    private String reportJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
