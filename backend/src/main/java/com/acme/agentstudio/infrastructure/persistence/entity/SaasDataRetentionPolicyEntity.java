package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasDataRetentionPolicy 数据库持久化实体对象。
 * 对应数据库中 SaasDataRetentionPolicy 数据表的字段结构映射。
 */
@Data @TableName("saas_data_retention_policy")
/**
 * SaasDataRetentionPolicy 数据表持久化实体类。
 * 映射数据库对应的 SaasDataRetentionPolicy 表结构。
 */
public class SaasDataRetentionPolicyEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private String dataCategory;
    private Integer versionNo; private Integer retentionDays; private String legalBasis; private String status;
    private LocalDateTime effectiveAt; private Long createdBy; private LocalDateTime createdAt;
}
