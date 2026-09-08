package com.acme.agentstudio.application.audit;

import com.acme.agentstudio.domain.common.AuditConstants;
import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.infrastructure.persistence.entity.AuditLogEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 平台统一合规审计日志应用服务。
 * 负责记录多租户环境下工作流编排、Agent 应用发布、角色权限变更以及敏感资源擦除与导出等关键操作痕迹，写入不可篡改审计流。
 */
@Service
public class AuditApplicationService {

    /** 审计日志持久化 Mapper */
    private final AuditLogMapper auditLogMapper;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入审计持久化依赖。
     */
    public AuditApplicationService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一条工作流/应用相关的关键业务操作审计事件。
     *
     * @param tenantId 租户 ID
     * @param operatorId 操作人用户标识/账号
     * @param actionType 操作动作类型编码（如 MEMBER_STATUS_CHANGE / RUNTIME_EVENT_RAW_READ）
     * @param workflowId 目标工作流或资源 ID
     * @param detail 操作上下文明细 Map
     */
    public void recordWorkflowAction(Long tenantId, String operatorId, String actionType, Long workflowId, Map<String, Object> detail) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setOperatorId(operatorId);
        entity.setActionType(actionType);
        entity.setTargetType(AuditConstants.TARGET_WORKFLOW);
        entity.setTargetId(String.valueOf(workflowId));
        entity.setRiskLevel(AuditConstants.RISK_LEVEL_NORMAL);
        entity.setDetailJson(toJson(detail));
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    /**
     * 将操作详情 Detail Map 序列化为 JSON 字符串。
     */
    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            throw new IllegalStateException(ApplicationMessages.AUDIT_DETAIL_SERIALIZE_FAILED, ex);
        }
    }
}

