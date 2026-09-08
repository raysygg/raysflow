package com.acme.agentstudio.interfaces.rest.dto;

/**
 * OrchestrationPublish 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 OrchestrationPublish 操作参数。
 */
/**
 * 发布工作流/Agent 编排草稿到指定运行环境请求对象。
 *
 * @param environmentCode 目标发布环境编码（如 PROD, STAGING 等）
 * @param changeSummary 发布日志与版本变更日志摘要
 * @param expectedRevisionNo 期待的期望草稿修订号（防并发覆盖）
 * @param candidateId 发布候选版本记录 ID
 * @param candidateFingerprint 候选版本哈希指纹（防串改）
 * @param evaluationRunId 关联的自动化评测运行 ID（可选）
 */
/**
 * OrchestrationPublish 业务请求数据传输对象 (DTO)。
 */
public record OrchestrationPublishRequest(
        String environmentCode,
        String changeSummary,
        Integer expectedRevisionNo,
        Long candidateId,
        String candidateFingerprint,
        Long evaluationRunId
) {
}
