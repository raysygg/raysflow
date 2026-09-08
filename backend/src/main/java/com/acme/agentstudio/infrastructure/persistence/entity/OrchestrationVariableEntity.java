package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OrchestrationVariable 数据库持久化实体对象。
 * 对应数据库中 OrchestrationVariable 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_variable")
/**
 * 数据库实体：映射表 `orchestration_variable`，承载 OrchestrationVariable 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationVariable 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationVariable 表结构。
 */
public class OrchestrationVariableEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用全局唯一 ID */
    private Long appId;
    /** revision 主键 ID 标识 属性 */
    private Long revisionId;
    /** source Node 主键 ID 标识 属性 */
    private String sourceNodeId;
    /** output Path 属性 */
    private String outputPath;
    /** data 业务分类类型 属性 */
    private String dataType;
    /** 数据库列使用非保留字 sensitive_flag；Java 属性明确表达布尔标记语义。 */
    @TableField("sensitive_flag")
    private Boolean sensitiveFlag;
}
