package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeSloPolicy 数据库持久化实体对象。
 * 对应数据库中 RuntimeSloPolicy 数据表的字段结构映射。
 */
@Data
@TableName("runtime_slo_policy")
/**
 * RuntimeSloPolicy 数据表持久化实体类。
 * 映射数据库对应的 RuntimeSloPolicy 表结构。
 */
public class RuntimeSloPolicyEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long applicationId; private String releaseId; private String policyCode;
    private String targetJson; private Integer windowMinutes; private Boolean enabled;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
