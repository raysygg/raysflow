package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AsyncTask 数据库持久化实体对象。
 * 对应数据库中 AsyncTask 数据表的字段结构映射。
 */
@Data
@TableName("async_task")
/**
 * 数据库实体：映射表 `async_task`，承载 AsyncTask 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * AsyncTask 数据表持久化实体类。
 * 映射数据库对应的 AsyncTask 表结构。
 */
public class AsyncTaskEntity {
    /** 异步任务主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务所属租户，用于数据隔离。 */
    private Long tenantId;
    /** 请求链路 ID，任务脱离 HTTP 线程后仍可追踪。 */
    private String requestId;
    /** 分布式 Trace ID。 */
    private String traceId;
    /** 关联的业务 Run ID。 */
    private String runId;
    /** 任务处理器类型，例如 EVALUATION、KNOWLEDGE_REINDEX。 */
    private String taskType;
    /** 同一租户和任务类型下的业务幂等键；普通任务允许为空。 */
    private String idempotencyKey;
    /** 处理器所需的业务参数。 */
    private String payloadJson;
    /** QUEUED、RUNNING、COMPLETED 或 FAILED。 */
    private String status;
    /** 已经执行的失败重试次数。 */
    private Integer retryCount;
    /** 允许的最大重试次数。 */
    private Integer maxRetries;
    /** 当前认领任务的工作节点标识。 */
    private String workerId;
    /** 任务允许开始执行的时间。 */
    private LocalDateTime availableAt;
    /** 工作节点最近一次续租时间。 */
    private LocalDateTime heartbeatAt;
    /** 任务入队时间。 */
    private LocalDateTime createdAt;
    /** 任务完成或最终失败时间。 */
    private LocalDateTime finishedAt;
    /** 最近一次失败或接管原因。 */
    private String errorMessage;
}
