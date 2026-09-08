package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasScimGroup 数据库持久化实体对象。
 * 对应数据库中 SaasScimGroup 数据表的字段结构映射。
 */
@Data @TableName("saas_scim_group")
/**
 * SaasScimGroup 数据表持久化实体类。
 * 映射数据库对应的 SaasScimGroup 表结构。
 */
public class SaasScimGroupEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private String sourceSystem;
    private String externalId; private String displayName; private String mappedRoleCode; private String syncStatus;
    private LocalDateTime lastSyncedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
