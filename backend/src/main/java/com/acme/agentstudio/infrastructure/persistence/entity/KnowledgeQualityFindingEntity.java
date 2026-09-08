package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeQualityFinding 数据库持久化实体对象。
 * 对应数据库中 KnowledgeQualityFinding 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_quality_finding")
/**
 * KnowledgeQualityFinding 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeQualityFinding 表结构。
 */
public class KnowledgeQualityFindingEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** knowledge Base 主键 ID 标识 属性 */
    private Long knowledgeBaseId;
    /** target 业务分类类型 属性 */
    private String targetType;
    /** target 主键 ID 标识 属性 */
    private Long targetId;
    /** finding 业务编码 属性 */
    private String findingCode;
    /** severity 属性 */
    private String severity;
    /** finding 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String findingStatus;
    /** evidence Summary 属性 */
    private String evidenceSummary;
    /** remediation 属性 */
    private String remediation;
    /** assignee 属性 */
    private Long assignee;
    /** audit Note 属性 */
    private String auditNote;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
