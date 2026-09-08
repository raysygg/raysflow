package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * EnterpriseIdentityConfig 数据库持久化实体对象。
 * 对应数据库中 EnterpriseIdentityConfig 数据表的字段结构映射。
 */
@Data
@TableName("enterprise_identity_config")
/**
 * EnterpriseIdentityConfig 数据表持久化实体类。
 * 映射数据库对应的 EnterpriseIdentityConfig 表结构。
 */
public class EnterpriseIdentityConfigEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Integer versionNo; private String protocol; private String status;
    private String issuer; private String entityId; private String organizationClaim; private String callbackUrl;
    private String claimMappingJson; private Boolean scimEnabled; private String mfaPolicyJson; private String secretRef;
    private Long createdBy; private LocalDateTime validatedAt; private LocalDateTime activatedAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
