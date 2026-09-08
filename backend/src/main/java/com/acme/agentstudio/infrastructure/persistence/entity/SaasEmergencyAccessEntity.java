package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasEmergencyAccess 数据库持久化实体对象。
 * 对应数据库中 SaasEmergencyAccess 数据表的字段结构映射。
 */
@Data @TableName("saas_emergency_access")
/**
 * SaasEmergencyAccess 数据表持久化实体类。
 * 映射数据库对应的 SaasEmergencyAccess 表结构。
 */
public class SaasEmergencyAccessEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long userId;
    private String reasonSummary; private String accessStatus; private LocalDateTime expiresAt;
    private LocalDateTime usedAt; private LocalDateTime createdAt;
}
