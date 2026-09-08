package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeSideEffectCheckpoint 数据库持久化实体对象。
 * 对应数据库中 RuntimeSideEffectCheckpoint 数据表的字段结构映射。
 */
@Data
@TableName("runtime_side_effect_checkpoint")
/**
 * RuntimeSideEffectCheckpoint 数据表持久化实体类。
 * 映射数据库对应的 RuntimeSideEffectCheckpoint 表结构。
 */
public class RuntimeSideEffectCheckpointEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private String runId; private Long taskId; private String operationKey;
    private String operationType; private String sideEffectStatus; private String requestSummary;
    private String providerReceipt; private String resultSummary; private LocalDateTime updatedAt;
}
