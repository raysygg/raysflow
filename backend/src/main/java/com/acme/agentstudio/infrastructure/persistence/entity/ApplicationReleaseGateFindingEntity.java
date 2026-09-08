package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ApplicationReleaseGateFinding 数据库持久化实体对象。
 * 对应数据库中 ApplicationReleaseGateFinding 数据表的字段结构映射。
 */
@Data
@TableName("application_release_gate_finding")
/**
 * ApplicationReleaseGateFinding 数据表持久化实体类。
 * 映射数据库对应的 ApplicationReleaseGateFinding 表结构。
 */
public class ApplicationReleaseGateFindingEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** gate Report 主键 ID 标识 属性 */
    private Long gateReportId;
    /** finding 业务编码 属性 */
    private String findingCode;
    /** 所属维度分类 */
    private String category;
    /** finding Level 属性 */
    private String findingLevel;
    /** 标题名称 */
    private String title;
    /** reason 属性 */
    private String reason;
    /** evidence Summary 属性 */
    private String evidenceSummary;
    /** remediation Target 属性 */
    private String remediationTarget;
    /** overridable 属性 */
    private Boolean overridable;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
