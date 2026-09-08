package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用生命周期与发布门禁配置属性映射类。
 * 绑定配置文件中前缀为 `app.lifecycle` 的控制参数，包含评测有效期、最小任务成功率、幻觉率阈值与延迟成本告警线。
 */
@ConfigurationProperties(prefix = "app.lifecycle")
public class ApplicationLifecycleProperties {

    /** 评估有效天数（默认 7 天） */
    private int evaluationValidityDays = 7;

    /** 门禁要求的最低任务成功率（默认 80%） */
    private double minimumTaskSuccessRate = 0.80D;

    /** 门禁要求的最低 Groundedness 事实依据度（默认 75%） */
    private double minimumGroundedness = 0.75D;

    /** 平均延迟告警线（单位：毫秒，默认 5000ms） */
    private long warningAverageLatencyMs = 5000L;

    /** 平均单次调用成本告警线（默认 1.0 元） */
    private double warningAverageCost = 1.0D;

    /** 获取评估有效天数 */
    public int getEvaluationValidityDays() {
        return evaluationValidityDays;
    }

    /** 设置评估有效天数 */
    public void setEvaluationValidityDays(int value) {
        this.evaluationValidityDays = value;
    }

    /** 获取门禁最低任务成功率 */
    public double getMinimumTaskSuccessRate() {
        return minimumTaskSuccessRate;
    }

    /** 设置门禁最低任务成功率 */
    public void setMinimumTaskSuccessRate(double value) {
        this.minimumTaskSuccessRate = value;
    }

    /** 获取门禁最低事实依据度 */
    public double getMinimumGroundedness() {
        return minimumGroundedness;
    }

    /** 设置门禁最低事实依据度 */
    public void setMinimumGroundedness(double value) {
        this.minimumGroundedness = value;
    }

    /** 获取平均延迟告警线（毫秒） */
    public long getWarningAverageLatencyMs() {
        return warningAverageLatencyMs;
    }

    /** 设置平均延迟告警线（毫秒） */
    public void setWarningAverageLatencyMs(long value) {
        this.warningAverageLatencyMs = value;
    }

    /** 获取平均单次调用成本告警线 */
    public double getWarningAverageCost() {
        return warningAverageCost;
    }

    /** 设置平均单次调用成本告警线 */
    public void setWarningAverageCost(double value) {
        this.warningAverageCost = value;
    }
}

