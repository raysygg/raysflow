package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * PlatformCredentialRef 数据库持久化实体对象。
 * 对应数据库中 PlatformCredentialRef 数据表的字段结构映射。
 */
@Data @TableName("platform_credential_ref")
/**
 * 数据库实体：映射表 `platform_credential_ref`，承载 PlatformCredentialRef 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformCredentialRef 数据表持久化实体类。
 * 映射数据库对应的 PlatformCredentialRef 表结构。
 */
public class PlatformCredentialRefEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private String credentialName; private String credentialType;
    private String ciphertext; private String keyVersion; private String status; private LocalDateTime rotatedAt; private Long createdBy; private LocalDateTime createdAt;
}
