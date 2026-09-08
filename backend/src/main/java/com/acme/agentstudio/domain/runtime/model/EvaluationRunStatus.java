package com.acme.agentstudio.domain.runtime.model;

/**
 * 测试集离线评测任务运行状态枚举（Evaluation Run Status）。
 */
public enum EvaluationRunStatus {

    /** 已入队排队 */
    QUEUED,

    /** 评测执行中 */
    RUNNING,

    /** 评测全部完成 */
    COMPLETED,

    /** 评测异常失败 */
    FAILED,

    /** 存在得分争议，需人工复核 */
    REVIEW_REQUIRED
}

