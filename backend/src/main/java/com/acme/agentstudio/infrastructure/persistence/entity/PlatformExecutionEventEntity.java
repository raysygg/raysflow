package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * PlatformExecutionEvent 数据库持久化实体对象。
 * 对应数据库中 PlatformExecutionEvent 数据表的字段结构映射。
 */
@Data
@TableName("platform_execution_event")
/**
 * 数据库实体：映射表 `platform_execution_event`，承载 PlatformExecutionEvent 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformExecutionEvent 数据表持久化实体类。
 * 映射数据库对应的 PlatformExecutionEvent 表结构。
 */
public class PlatformExecutionEventEntity {
    /** 执行事件主键。 */
    @TableId(type = IdType.AUTO) private Long id;
    /** 事件所属租户主键。 */
    private Long tenantId;
    /** 一次编排执行的业务 ID。 */
    private String executionId;
    /** 请求链路 ID。 */
    private String requestId;
    /** 分布式 Trace ID。 */
    private String traceId;
    /** 事件级 Span ID。 */
    private String spanId;
    /** 关联的异步任务 ID。 */
    private Long taskId;
    /** 事件所属应用和发布版本，避免查询时必须二次解析运行上下文。 */
    private Long applicationId;
    /** 运行版本标识 主键 ID 标识 属性 */
    private Long versionId;
    /** 事件发生时的运行状态、耗时和稳定错误分类。 */
    private String status;
    /** duration Ms 属性 */
    private Long durationMs;
    /** error 业务编码 属性 */
    private String errorCode;
    /** error Message 属性 */
    private String errorMessage;
    /** 默认保存脱敏后的事件摘要，原始内容由后续审计权限控制。 */
    private String summaryJson;
    /** 产生事件的节点 ID，可为空。 */
    private String nodeId;
    /** 事件类型，例如 NODE_STARTED、NODE_FAILED。 */
    private String eventType;
    /** 同一执行内的递增序号，用于还原事件顺序。 */
    private Long sequenceNo;
    /** 事件载荷 JSON，仅供服务端审计和恢复使用，普通 API 禁止序列化原文。 */
    @JsonIgnore
    private String payloadJson;
    /** 事件写入时间。 */
    private LocalDateTime createdAt;
}
