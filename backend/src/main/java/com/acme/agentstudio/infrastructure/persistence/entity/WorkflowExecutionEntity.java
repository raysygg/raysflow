package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WorkflowExecution 数据库持久化实体对象。
 * 对应数据库中 WorkflowExecution 数据表的字段结构映射。
 */
@Data
@TableName("workflow_execution")
/**
 * 数据库实体：映射表 `workflow_execution`，承载 WorkflowExecution 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * WorkflowExecution 数据表持久化实体类。
 * 映射数据库对应的 WorkflowExecution 表结构。
 */
public class WorkflowExecutionEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** session 主键 ID 标识 属性 */
    private Long sessionId;
    /** 工作流编码 */
    private String workflowCode;
    /** input Message 属性 */
    private String inputMessage;
    /** execution Key 属性 */
    private String executionKey;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** error Message 属性 */
    private String errorMessage;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** heartbeat At 属性 */
    private LocalDateTime heartbeatAt;
    /** finished At 属性 */
    private LocalDateTime finishedAt;
}
