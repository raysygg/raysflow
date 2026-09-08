package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeDeadLetter 数据库持久化实体对象。
 * 对应数据库中 RuntimeDeadLetter 数据表的字段结构映射。
 */
@Data
@TableName("runtime_dead_letter")
/**
 * RuntimeDeadLetter 数据表持久化实体类。
 * 映射数据库对应的 RuntimeDeadLetter 表结构。
 */
public class RuntimeDeadLetterEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long applicationId; private String releaseId; private Long taskId;
    private String runId; private String failedNodeId; private String errorCategory; private String errorSummary;
    private String snapshotJson; private String deadLetterStatus; private Long assignedTo;
    /** replay Run 主键 ID 标识 属性 */
    private String replayRunId;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
