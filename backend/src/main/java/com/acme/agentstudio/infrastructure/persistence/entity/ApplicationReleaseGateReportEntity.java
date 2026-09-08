package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ApplicationReleaseGateReport 数据库持久化实体对象。
 * 对应数据库中 ApplicationReleaseGateReport 数据表的字段结构映射。
 */
@Data
@TableName("application_release_gate_report")
/**
 * ApplicationReleaseGateReport 数据表持久化实体类。
 * 映射数据库对应的 ApplicationReleaseGateReport 表结构。
 */
public class ApplicationReleaseGateReportEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** candidate 主键 ID 标识 属性 */
    private Long candidateId;
    /** candidate Fingerprint 属性 */
    private String candidateFingerprint;
    /** evaluation Run 主键 ID 标识 属性 */
    private Long evaluationRunId;
    /** overall Level 属性 */
    private String overallLevel;
    /** override Granted 属性 */
    private Boolean overrideGranted;
    /** override Reason 属性 */
    private String overrideReason;
    /** override Scope Json 属性 */
    private String overrideScopeJson;
    /** override By 属性 */
    private Long overrideBy;
    /** override Expires At 属性 */
    private LocalDateTime overrideExpiresAt;
    /** evaluated By 属性 */
    private Long evaluatedBy;
    /** evaluated At 属性 */
    private LocalDateTime evaluatedAt;
}
