package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * OrchestrationEdge 数据库持久化实体对象。
 * 对应数据库中 OrchestrationEdge 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_edge")
/**
 * 数据库实体：映射表 `orchestration_edge`，承载 OrchestrationEdge 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationEdge 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationEdge 表结构。
 */
public class OrchestrationEdgeEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用全局唯一 ID */
    private Long appId;
    /** revision 主键 ID 标识 属性 */
    private Long revisionId;
    /** edge 主键 ID 标识 属性 */
    private String edgeId;
    /** source Node 主键 ID 标识 属性 */
    private String sourceNodeId;
    /** source Port 属性 */
    private String sourcePort;
    /** target Node 主键 ID 标识 属性 */
    private String targetNodeId;
    /** target Port 属性 */
    private String targetPort;
}
