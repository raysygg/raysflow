package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasLegalHold 数据库持久化实体对象。
 * 对应数据库中 SaasLegalHold 数据表的字段结构映射。
 */
@Data @TableName("saas_legal_hold")
/**
 * SaasLegalHold 数据表持久化实体类。
 * 映射数据库对应的 SaasLegalHold 表结构。
 */
public class SaasLegalHoldEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private String holdCode;
    private String scopeJson; private String reasonSummary; private String holdStatus; private Long authorizedBy;
    private Long releasedBy; private LocalDateTime startsAt; private LocalDateTime releasedAt; private LocalDateTime createdAt;
}
