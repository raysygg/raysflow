package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeTaskAttempt 数据库持久化实体对象。
 * 对应数据库中 RuntimeTaskAttempt 数据表的字段结构映射。
 */
@Data
@TableName("runtime_task_attempt")
/**
 * RuntimeTaskAttempt 数据表持久化实体类。
 * 映射数据库对应的 RuntimeTaskAttempt 表结构。
 */
public class RuntimeTaskAttemptEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long taskId; private String runId; private Integer attemptNo;
    private String leaseOwner; private String attemptStatus; private LocalDateTime leaseUntil;
    private LocalDateTime heartbeatAt; private LocalDateTime startedAt; private LocalDateTime finishedAt;
    private String errorCategory; private String errorSummary; private String retryDecision;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
