package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasScimIdentity 数据库持久化实体对象。
 * 对应数据库中 SaasScimIdentity 数据表的字段结构映射。
 */
@Data @TableName("saas_scim_identity")
/**
 * SaasScimIdentity 数据表持久化实体类。
 * 映射数据库对应的 SaasScimIdentity 表结构。
 */
public class SaasScimIdentityEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long userId;
    private String externalId; private String externalUserName; private String sourceSystem; private String syncStatus;
    private LocalDateTime lastSyncedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
