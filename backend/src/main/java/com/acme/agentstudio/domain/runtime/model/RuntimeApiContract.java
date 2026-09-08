package com.acme.agentstudio.domain.runtime.model;

/**
 * Agent Runtime 运行时对外 REST/WebSocket 接口约定的常量契约类（Runtime API Contract）。
 * 集中管理默认执行类型 DEFAULT_EXECUTION_TYPE、默认触发入口 DEFAULT_TRIGGER_TYPE、首游标 FIRST_EVENT_CURSOR 及核心错误编码。
 */
public final class RuntimeApiContract {

    /** 默认后端执行类型：应用工作流 */
    public static final String DEFAULT_EXECUTION_TYPE = "APPLICATION_WORKFLOW";

    /** 默认入口类型：表单提交 */
    public static final String DEFAULT_TRIGGER_TYPE = "FORM";

    /** 首次事件游标起始号：0L */
    public static final long FIRST_EVENT_CURSOR = 0L;

    /** 错误码：无效的租户/用户身份凭证 */
    public static final String ERROR_INVALID_IDENTITY = "RUNTIME_INVALID_IDENTITY";

    /** 错误码：应用未指明或无法定位 */
    public static final String ERROR_APPLICATION_REQUIRED = "RUNTIME_APPLICATION_REQUIRED";

    /** 私有构造函数，防止工具类被实例化 */
    private RuntimeApiContract() {
    }
}

