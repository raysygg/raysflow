package com.acme.agentstudio.interfaces.rest.dto;

import com.acme.agentstudio.common.util.SystemIdentifierGenerator;

/**
 * OrchestrationDraft 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 OrchestrationDraft 操作参数。
 */
/**
 * 保存工作流/Agent 编排草稿请求对象。
 *
 * @param appId 应用/Agent 唯一 ID
 * @param appCode 应用/Agent 编码（若为空将根据 appName 自动生成）
 * @param appName 应用/Agent 展示名称
 * @param graphType 编排图类型（如 WORKFLOW, AGENT_SINGLE 等）
 * @param graphJson 画布节点与连线 JSON
 * @param expectedRevisionNo 期待的期望修订版本号（并发控制）
 * @param changeSummary 本次修改变更摘要说明
 */
/**
 * OrchestrationDraft 业务请求数据传输对象 (DTO)。
 */
public record OrchestrationDraftRequest(
        Long appId,
        String appCode,
        String appName,
        String graphType,
        String graphJson,
        Integer expectedRevisionNo,
        String changeSummary
) {
    public OrchestrationDraftRequest {
        if (appCode == null || appCode.isBlank()) {
            appCode = SystemIdentifierGenerator.fromName(appName, "workflow");
        } else {
            appCode = appCode.trim();
        }
    }
}

