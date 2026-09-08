package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasMfaPolicy 数据库持久化实体对象。
 * 对应数据库中 SaasMfaPolicy 数据表的字段结构映射。
 */
@Data @TableName("saas_mfa_policy")
/**
 * SaasMfaPolicy 数据表持久化实体类。
 * 映射数据库对应的 SaasMfaPolicy 表结构。
 */
public class SaasMfaPolicyEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private String roleCode;
    private Boolean required; private Integer stepUpMinutes; private String highRiskActionsJson;
    private Integer versionNo; private String status; private Long updatedBy; private LocalDateTime updatedAt;
}
