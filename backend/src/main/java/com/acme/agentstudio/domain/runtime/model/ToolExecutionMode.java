package com.acme.agentstudio.domain.runtime.model;

/**
 * 外部工具与 API 调用执行模式枚举（Tool Execution Mode）。
 * 区分真实线上执行 LIVE 与预检试运行 DRY_RUN（测试和发布门禁评估前默认强制 DRY_RUN）。
 */
public enum ToolExecutionMode {

    /** 真实线上环境调用 */
    LIVE,

    /** 试运行/沙箱 Mock 预检运行 */
    DRY_RUN
}

