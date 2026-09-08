package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeRecoveryRequest 数据库持久化实体对象。
 * 对应数据库中 RuntimeRecoveryRequest 数据表的字段结构映射。
 */
@Data
@TableName("runtime_recovery_request")
/**
 * RuntimeRecoveryRequest 数据表持久化实体类。
 * 映射数据库对应的 RuntimeRecoveryRequest 表结构。
 */
public class RuntimeRecoveryRequestEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long applicationId; private String releaseId; private String runId;
    private String actionType; private String requestStatus; private String reason; private Long requestedBy;
    private LocalDateTime createdAt; private LocalDateTime completedAt;
}
