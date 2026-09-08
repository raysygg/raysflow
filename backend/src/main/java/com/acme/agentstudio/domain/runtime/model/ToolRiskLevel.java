package com.acme.agentstudio.domain.runtime.model;

/**
 * 外部工具与 API 调用安全风险等级枚举（Tool Risk Level）。
 * 决定工具执行时是否触发人工审核二次确认单及受控隔离策略。
 */
public enum ToolRiskLevel {

    /** 低风险（如只读查询 API） */
    LOW,

    /** 中风险（如常规数据更新写入） */
    MEDIUM,

    /** 高风险（如批量修改、关键系统写接口，需人工确认） */
    HIGH,

    /** 极高风险/危险等级（如账号删除、转账资金操作，必须超级管理员二次审批） */
    CRITICAL
}

