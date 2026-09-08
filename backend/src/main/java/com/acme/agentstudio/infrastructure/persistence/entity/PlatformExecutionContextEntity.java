package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * PlatformExecutionContext 数据库持久化实体对象。
 * 对应数据库中 PlatformExecutionContext 数据表的字段结构映射。
 */
@Data
@TableName("platform_execution_context")
/**
 * 数据库实体：映射表 `platform_execution_context`，承载 PlatformExecutionContext 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformExecutionContext 数据表持久化实体类。
 * 映射数据库对应的 PlatformExecutionContext 表结构。
 */
public class PlatformExecutionContextEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** execution 主键 ID 标识 属性 */
    private String executionId;
    /** 请求链路 ID，贯穿一次用户请求与其异步 Run。 */
    private String requestId;
    /** 分布式 Trace ID，贯穿同一条业务链路。 */
    private String traceId;
    /** Run 级 Span ID，用于定位 Run 主执行段。 */
    private String spanId;
    /** 当前异步任务 ID，用于把 Run 与 Worker 任务关联起来。 */
    private Long taskId;
    /** 本次重试来源 Run ID；首次运行为空。 */
    private String retryOfExecutionId;
    /** Replay 创建的新 Run 指向原 Run；与同 Run 内重试语义严格区分。 */
    private String replayOfExecutionId;
    /** 当前 Run 的尝试序号，从 1 开始。 */
    private Integer retryAttempt;
    /** 应用全局唯一 ID */
    private Long appId;
    /** Run 创建时冻结的入口来源，重试和人工恢复必须原样继承。 */
    private Long entrypointId;
    /** entrypoint 业务分类类型 属性 */
    private String entrypointType;
    /** delivery Mode 属性 */
    private String deliveryMode;
    /** trigger Source 属性 */
    private String triggerSource;
    /** scheduled Fire Time 属性 */
    private LocalDateTime scheduledFireTime;
    /** 运行版本标识 主键 ID 标识 属性 */
    private Long versionId;
    /** 草稿测试绑定的草稿修订主键；正式运行不填写。 */
    private Long draftRevisionId;
    /** 草稿测试使用的图快照，保证测试期间不受后续编辑影响。 */
    private String draftGraphJson;
    /** 草稿测试使用的修订号，便于运行详情明确显示测试来源。 */
    private Integer draftRevisionNo;
    /** 服务端判定的运行来源：生产、草稿测试或节点调试。 */
    private String runType;
    /** Run 启动时复制的发布包，重试和人工恢复不得重新读取可变配置。 */
    private String runtimeSnapshotJson;
    /** runtime Snapshot Hash 属性 */
    private String runtimeSnapshotHash;
    /** execution 业务分类类型 属性 */
    private String executionType;
    /** idempotency Key 属性 */
    private String idempotencyKey;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** input Json 属性 */
    private String inputJson;
    /** output Json 属性 */
    private String outputJson;
    /** error 业务编码 属性 */
    private String errorCode;
    /** error Message 属性 */
    private String errorMessage;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** finished At 属性 */
    private LocalDateTime finishedAt;
    /** lease Owner 属性 */
    private String leaseOwner;
    /** lease Until 属性 */
    private LocalDateTime leaseUntil;
    /** heartbeat At 属性 */
    private LocalDateTime heartbeatAt;
    /** cancel Requested 属性 */
    private Boolean cancelRequested;
    /** pause Requested 属性 */
    private Boolean pauseRequested;
    /** retry Count 属性 */
    private Integer retryCount;
    /** max Attempts 属性 */
    private Integer maxAttempts;
    /** current Node 主键 ID 标识 属性 */
    private String currentNodeId;
    /** state Json 属性 */
    private String stateJson;
    /** approval Decision 属性 */
    private String approvalDecision;
    /** compensation Json 属性 */
    private String compensationJson;
}
