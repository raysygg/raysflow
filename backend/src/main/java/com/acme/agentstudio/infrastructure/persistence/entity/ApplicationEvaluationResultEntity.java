package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ApplicationEvaluationResult 数据库持久化实体对象。
 * 对应数据库中 ApplicationEvaluationResult 数据表的字段结构映射。
 */
@Data
@TableName("application_evaluation_result")
/**
 * ApplicationEvaluationResult 数据表持久化实体类。
 * 映射数据库对应的 ApplicationEvaluationResult 表结构。
 */
public class ApplicationEvaluationResultEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** evaluation Run 主键 ID 标识 属性 */
    private Long evaluationRunId;
    /** case 主键 ID 标识 属性 */
    private Long caseId;
    /** result 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String resultStatus;
    /** CANDIDATE 或 BASELINE，用于同一 Suite 的版本对比。 */
    private String evaluationVariant;
    /** input Digest 属性 */
    private String inputDigest;
    /** output Digest 属性 */
    private String outputDigest;
    /** evidence Json 属性 */
    private String evidenceJson;
    /** task Success Score 属性 */
    private BigDecimal taskSuccessScore;
    /** correctness Score 属性 */
    private BigDecimal correctnessScore;
    /** groundedness Score 属性 */
    private BigDecimal groundednessScore;
    /** citation Score 属性 */
    private BigDecimal citationScore;
    /** latency Ms 属性 */
    private Long latencyMs;
    /** input Tokens 属性 */
    private Long inputTokens;
    /** output Tokens 属性 */
    private Long outputTokens;
    /** estimated Cost 属性 */
    private BigDecimal estimatedCost;
    /** failure 所属维度分类 属性 */
    private String failureCategory;
    /** failure Message 属性 */
    private String failureMessage;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
