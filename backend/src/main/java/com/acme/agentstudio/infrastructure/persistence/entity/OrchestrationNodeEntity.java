package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OrchestrationNode 数据库持久化实体对象。
 * 对应数据库中 OrchestrationNode 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_node")
/**
 * 数据库实体：映射表 `orchestration_node`，承载 OrchestrationNode 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationNode 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationNode 表结构。
 */
public class OrchestrationNodeEntity {
    @TableId(type = IdType.AUTO)
    /** 节点记录主键。 */
    private Long id;
    /** 所属租户主键。 */
    private Long tenantId;
    /** 所属编排应用主键。 */
    private Long appId;
    /** 所属草稿修订主键。 */
    private Long revisionId;
    /** 图内稳定节点 ID，边和变量引用使用该值。 */
    private String nodeId;
    /** 节点类型编码，对应节点类型注册表。 */
    private String nodeType;
    /** 画布上的节点标题。 */
    private String title;
    /** 节点配置 JSON，按节点类型 schema 解释。 */
    private String configJson;
    /** 节点输入 schema JSON。 */
    private String inputSchemaJson;
    /** 节点输出 schema JSON。 */
    private String outputSchemaJson;
}
