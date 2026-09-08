package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ApplicationEvaluationRun 数据库持久化实体对象。
 * 对应数据库中 ApplicationEvaluationRun 数据表的字段结构映射。
 */
@Data
@TableName("application_evaluation_run")
/**
 * ApplicationEvaluationRun 数据表持久化实体类。
 * 映射数据库对应的 ApplicationEvaluationRun 表结构。
 */
public class ApplicationEvaluationRunEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** candidate 主键 ID 标识 属性 */
    private Long candidateId;
    /** candidate Fingerprint 属性 */
    private String candidateFingerprint;
    /** suite 运行版本标识 主键 ID 标识 属性 */
    private Long suiteVersionId;
    /** baseline Release 主键 ID 标识 属性 */
    private String baselineReleaseId;
    /** dependency Fingerprint 属性 */
    private String dependencyFingerprint;
    /** evaluation 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String evaluationStatus;
    /** aggregate Report Json 属性 */
    private String aggregateReportJson;
    /** failure 所属维度分类 属性 */
    private String failureCategory;
    /** failure Message 属性 */
    private String failureMessage;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** completed At 属性 */
    private LocalDateTime completedAt;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
