package com.acme.agentstudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 运行中心任务断点恢复与 Worker 租约配置属性映射类。
 * 绑定配置文件中前缀为 `app.runtime-recovery` 的控制参数，包含 Worker 租约时长、续约心跳间隔、最大重试次数与指数避退延迟线。
 */
@Data
@ConfigurationProperties(prefix = "app.runtime-recovery")
public class RuntimeRecoveryProperties {

    /** Worker 节点认领任务的有效租约时长（单位：秒，默认 120s） */
    private int leaseSeconds = 120;

    /** Worker 节点租约续约心跳时间间隔（单位：秒，默认 30s） */
    private int heartbeatSeconds = 30;

    /** 失败任务允许自动重试的最大次数（默认 3 次） */
    private int maxAttempts = 3;

    /** 第一次重试的初始退避延迟时间（单位：秒，默认 30s） */
    private long initialBackoffSeconds = 30;

    /** 指数退避的最大延迟上限（单位：秒，默认 1800s/30min） */
    private long maxBackoffSeconds = 1800;

    /** 单次可恢复任务调度的批次上限数量（默认 50） */
    private int maxBatchSize = 50;

    /** 告警观察的时间窗口跨度（单位：分钟，默认 15min） */
    private int alertWindowMinutes = 15;

    /** 任务快照与事件日志的硬保留天数（默认 30 天） */
    private int retentionDays = 30;
}

