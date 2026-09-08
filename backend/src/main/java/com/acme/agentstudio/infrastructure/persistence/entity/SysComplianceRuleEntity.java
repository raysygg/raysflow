package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysComplianceRule 数据库持久化实体对象。
 * 对应数据库中 SysComplianceRule 数据表的字段结构映射。
 */
@Data
@TableName("sys_compliance_rule")
/**
 * 数据库实体：映射表 `sys_compliance_rule`，承载 SysComplianceRule 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * SysComplianceRule 数据表持久化实体类。
 * 映射数据库对应的 SysComplianceRule 表结构。
 */
public class SysComplianceRuleEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** rule 展示名称 属性 */
    private String ruleName;
    /** sensitive Word 属性 */
    private String sensitiveWord;
    /** action 业务分类类型 属性 */
    private String actionType;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
