package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * SaasAdmissionDecision 数据库持久化实体对象。
 * 对应数据库中 SaasAdmissionDecision 数据表的字段结构映射。
 */
@Data @TableName("saas_admission_decision")
/**
 * SaasAdmissionDecision 数据表持久化实体类。
 * 映射数据库对应的 SaasAdmissionDecision 表结构。
 */
public class SaasAdmissionDecisionEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long applicationId;
    private String featureCode; private String decision; private Long currentUsage; private Long requestedQuantity;
    private Long usageLimit; private String overagePolicy; private String reasonCode; private String requestId;
    private Boolean shadowMode; private LocalDateTime resetAt; private LocalDateTime createdAt;
}
