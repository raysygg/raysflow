package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SaasGovernanceRequest 数据库持久化实体对象。
 * 对应数据库中 SaasGovernanceRequest 数据表的字段结构映射。
 */
@Data
@TableName("saas_governance_request")
/**
 * SaasGovernanceRequest 数据表持久化实体类。
 * 映射数据库对应的 SaasGovernanceRequest 表结构。
 */
public class SaasGovernanceRequestEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private String requestType; private String requestStatus; private String scopeJson;
    private Long requestedBy; private Long approvedBy; private String progressJson; private String evidenceHash;
    /** protected Reference 属性 */
    private String protectedReference;
    private LocalDateTime expiresAt; private LocalDateTime approvedAt; private LocalDateTime completedAt;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
