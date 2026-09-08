package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasGovernanceEvidence 数据库持久化实体对象。
 * 对应数据库中 SaasGovernanceEvidence 数据表的字段结构映射。
 */
@Data @TableName("saas_governance_evidence")
/**
 * SaasGovernanceEvidence 数据表持久化实体类。
 * 映射数据库对应的 SaasGovernanceEvidence 表结构。
 */
public class SaasGovernanceEvidenceEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long requestId;
    private String storeType; private String executionStatus; private Long objectCount; private String checksum;
    private String safeSummary; private Integer retryCount; private LocalDateTime completedAt; private LocalDateTime createdAt;
}
