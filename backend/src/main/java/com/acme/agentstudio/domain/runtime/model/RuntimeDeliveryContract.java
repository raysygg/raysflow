package com.acme.agentstudio.domain.runtime.model;

/**
 * 统一 Agent 运行时交付协议（Runtime Delivery Contract）的字段名称与默认值常量定义类。
 * 集中管理 responseMode、runId、eventCursor、result 等字段 Key，避免 Controller、Service 与前端组件散落魔法字符串。
 */
public final class RuntimeDeliveryContract {

    /** 响应交付模式字段 Key："responseMode" */
    public static final String RESPONSE_MODE = "responseMode";

    /** 运行任务句柄 ID 字段 Key："runId" */
    public static final String RUN_ID = "runId";

    /** 事件自增游标字段 Key："eventCursor" */
    public static final String EVENT_CURSOR = "eventCursor";

    /** 结构化最终输出字段 Key："result" */
    public static final String RESULT = "result";

    /** 初始事件游标代号：0L */
    public static final long FIRST_EVENT_CURSOR = 0L;

    /** 私有构造函数，防止工具类被实例化 */
    private RuntimeDeliveryContract() {
    }
}

