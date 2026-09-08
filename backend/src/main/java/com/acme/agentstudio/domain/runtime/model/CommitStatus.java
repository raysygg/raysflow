package com.acme.agentstudio.domain.runtime.model;

/**
 * 外部动作/副作用工具调用的提交事务状态枚举（Commit Status）。
 * 用于在网络超时或网络断开恢复时精确判断外部系统动作的执行与补偿状态。
 */
public enum CommitStatus {

    /** 未开始提交 */
    NOT_STARTED,

    /** 提交请求已发出（飞奔中/进行中） */
    IN_FLIGHT,

    /** 已经成功提交（固化成真） */
    COMMITTED,

    /** 提交失败并已执行冲销/逆向补偿 */
    COMPENSATED,

    /** 状态未知（需要人工介入复核） */
    UNKNOWN
}

