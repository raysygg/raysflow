package com.acme.agentstudio.application.task;

/**
 * 业务待办任务状态常量定义。
 * 用于统一标识人工审批、补充材料提交、异常恢复工单以及人工干预任务的处理流程状态。
 */
public final class BusinessTaskStatus {

    /** 状态：待处理（开启） */
    public static final String OPEN = "OPEN";

    /** 状态：处理中 */
    public static final String IN_PROGRESS = "IN_PROGRESS";

    /** 状态：已完成/审批通过 */
    public static final String COMPLETED = "COMPLETED";

    /** 状态：已驳回/已拒绝 */
    public static final String REJECTED = "REJECTED";

    /** 状态：已取消/已撤销 */
    public static final String CANCELLED = "CANCELLED";

    /**
     * 私有构造函数，防止常量工具类实例化。
     */
    private BusinessTaskStatus() {
    }
}

