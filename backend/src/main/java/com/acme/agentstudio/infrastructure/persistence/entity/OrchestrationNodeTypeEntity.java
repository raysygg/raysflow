package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OrchestrationNodeType 数据库持久化实体对象。
 * 对应数据库中 OrchestrationNodeType 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_node_type")
/**
 * 数据库实体：映射表 `orchestration_node_type`，承载 OrchestrationNodeType 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationNodeType 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationNodeType 表结构。
 */
public class OrchestrationNodeTypeEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 节点功能类型 */
    private String nodeType;
    /** 运行版本标识 */
    private String version;
    /** label 属性 */
    private String label;
    /** 详细业务功能描述 */
    private String description;
    /** symbol 属性 */
    private String symbol;
    /** supported Graph Types Json 属性 */
    private String supportedGraphTypesJson;
    /** capabilities Json 属性 */
    private String capabilitiesJson;
    /** permissions Json 属性 */
    private String permissionsJson;
    /** required Config Fields Json 属性 */
    private String requiredConfigFieldsJson;
    /** input Ports Json 属性 */
    private String inputPortsJson;
    /** output Ports Json 属性 */
    private String outputPortsJson;
    /** config Schema Json 属性 */
    private String configSchemaJson;
    /** input Schema Json 属性 */
    private String inputSchemaJson;
    /** output Schema Json 属性 */
    private String outputSchemaJson;
    /** sort Order 属性 */
    private Integer sortOrder;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** executor Key 属性 */
    private String executorKey;
}
