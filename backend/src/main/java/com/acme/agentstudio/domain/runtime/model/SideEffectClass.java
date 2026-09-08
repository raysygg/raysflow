package com.acme.agentstudio.domain.runtime.model;

/**
 * 外部工具与 API 调用的副作用受控等级枚举（Side Effect Class）。
 * 明确动作对外部数据库/系统产生的影响：无副作用 (READ_ONLY)、可逆可冲销 (REVERSIBLE) 与不可逆 (IRREVERSIBLE)。
 */
public enum SideEffectClass {

    /** 只读访问（无任何持久化写副作用） */
    READ_ONLY,

    /** 可逆写操作（提供对应的 Rollback / Undo 补偿 API） */
    REVERSIBLE,

    /** 不可逆操作（如真实扣费、发送短信、广播通知，需要严格审批防重推） */
    IRREVERSIBLE
}

