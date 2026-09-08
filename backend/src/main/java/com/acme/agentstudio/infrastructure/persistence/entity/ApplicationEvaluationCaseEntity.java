package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ApplicationEvaluationCase 数据库持久化实体对象。
 * 对应数据库中 ApplicationEvaluationCase 数据表的字段结构映射。
 */
@Data
@TableName("application_evaluation_case")
/**
 * ApplicationEvaluationCase 数据表持久化实体类。
 * 映射数据库对应的 ApplicationEvaluationCase 表结构。
 */
public class ApplicationEvaluationCaseEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** suite 运行版本标识 主键 ID 标识 属性 */
    private Long suiteVersionId;
    /** case 业务编码 属性 */
    private String caseCode;
    /** input Json 属性 */
    private String inputJson;
    /** expected Rule Json 属性 */
    private String expectedRuleJson;
    /** metric Applicability Json 属性 */
    private String metricApplicabilityJson;
    /** sort Order 属性 */
    private Integer sortOrder;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
