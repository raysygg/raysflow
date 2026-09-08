package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WorkflowExecutionNode 数据库持久化实体对象。
 * 对应数据库中 WorkflowExecutionNode 数据表的字段结构映射。
 */
/** 工作流节点执行明细，记录每个节点的尝试次数、状态和失败原因。 */
@Data
@TableName("workflow_execution_node")
/**
 * 数据库实体：映射表 `workflow_execution_node`，承载 WorkflowExecutionNode 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * WorkflowExecutionNode 数据表持久化实体类。
 * 映射数据库对应的 WorkflowExecutionNode 表结构。
 */
public class WorkflowExecutionNodeEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** execution 主键 ID 标识 属性 */
    private Long executionId;
    /** 工作流节点 ID */
    private String nodeId;
    /** 节点功能类型 */
    private String nodeType;
    /** attempt 属性 */
    private Integer attempt;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** error Message 属性 */
    private String errorMessage;
    /** input Summary 属性 */
    private String inputSummary;
    /** output Summary 属性 */
    private String outputSummary;
    /** trace Json 属性 */
    private String traceJson;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** finished At 属性 */
    private LocalDateTime finishedAt;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
