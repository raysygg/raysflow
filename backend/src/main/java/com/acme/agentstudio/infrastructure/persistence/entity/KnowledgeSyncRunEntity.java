package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeSyncRun 数据库持久化实体对象。
 * 对应数据库中 KnowledgeSyncRun 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_sync_run")
/**
 * KnowledgeSyncRun 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeSyncRun 表结构。
 */
public class KnowledgeSyncRunEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** source 主键 ID 标识 属性 */
    private Long sourceId;
    /** run 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String runStatus;
    /** cursor Before 属性 */
    private String cursorBefore;
    /** cursor After 属性 */
    private String cursorAfter;
    /** discovered Count 属性 */
    private Integer discoveredCount;
    /** changed Count 属性 */
    private Integer changedCount;
    /** 逻辑删除标记（0:未删 1:已删） Count 属性 */
    private Integer deletedCount;
    /** skipped Count 属性 */
    private Integer skippedCount;
    /** failed Count 属性 */
    private Integer failedCount;
    /** error 业务编码 属性 */
    private String errorCode;
    /** error Message 属性 */
    private String errorMessage;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** completed At 属性 */
    private LocalDateTime completedAt;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
