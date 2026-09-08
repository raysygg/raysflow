package com.acme.agentstudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * SaaS 多租户治理与商业化配额配置属性映射类。
 * 绑定配置文件中前缀为 `app.saas-governance` 的控制参数，包含默认账期天数、导出链接有效期、审计日志硬保留年限与预算告警线。
 */
@Data
@ConfigurationProperties(prefix = "app.saas-governance")
public class SaasGovernanceProperties {

    /** 默认结算账期天数（默认 30 天） */
    private int defaultBillingPeriodDays = 30;

    /** 租户采用度事件 Schema 版本号（默认 1） */
    private int adoptionEventSchemaVersion = 1;

    /** 跨租户采用基准允许展示的最小租户样本数 */
    private int adoptionMinimumBenchmarkTenants = 5;

    /** 采用事件允许的最大入库延迟小时数 */
    private int adoptionMaximumEventDelayHours = 24;

    /** 单租户每日采用事件异常流量阈值 */
    private int adoptionAbnormalDailyEventLimit = 10000;

    /** 导出的对账单/分析数据下载链接的有效存活分钟数（默认 30min） */
    private int exportLinkExpiryMinutes = 30;

    /** 单次治理任务批处理上限数量（默认 100） */
    private int governanceBatchLimit = 100;

    /** 用量数据日志的硬保留天数（默认 730 天 / 2 年） */
    private int usageRetentionDays = 730;

    /** 商业审计日志的硬保留天数（默认 2555 天 / 7 年） */
    private int auditRetentionDays = 2555;

    /** 预算额度使用比例触发预警的阈值（默认 80%） */
    private double budgetWarningPercent = 0.8D;

    /** 默认结算货币单位（默认 "CNY"） */
    private String defaultCurrency = "CNY";

    /** 权益读取的本地短缓存秒数 */
    private int admissionCacheTtlSeconds = 15;

    /** 已启用真实阻断的功能，未列入的功能只记录影子决策 */
    private Set<String> enforcedAdmissionFeatures = Set.of("WORKFLOW_RUN", "KNOWLEDGE_STORAGE");

    /** 准入基础设施异常时允许告警放行的计费类功能 */
    private Set<String> admissionFailOpenFeatures = Set.of("MODEL_TOKEN", "WORKFLOW_RUN", "CONNECTOR_CALL");
}

