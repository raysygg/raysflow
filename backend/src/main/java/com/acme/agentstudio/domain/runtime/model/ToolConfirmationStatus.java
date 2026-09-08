package com.acme.agentstudio.domain.runtime.model;

/**
 * 高风险工具执行人工二次确认单（Tool Confirmation）生命周期状态枚举。
 */
public enum ToolConfirmationStatus {

    /** 确认单等待审核中 */
    PENDING,

    /** 已同意放行执行 */
    APPROVED,

    /** 驳回拒绝执行 */
    DENIED,

    /** 超时未审批自动失效 */
    EXPIRED
}

