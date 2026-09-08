package com.acme.agentstudio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库运维与质量分析配置属性映射类。
 * 绑定配置文件中前缀为 `agent.knowledge.operations` 的管理参数，包含同步失败重试次数、文档陈旧失效时长、未命中告警比例等。
 */
@Data
@ConfigurationProperties(prefix = "agent.knowledge.operations")
public class KnowledgeOperationsProperties {

    /** 文档索引同步最大重试次数（默认 3 次） */
    private int maxSyncRetryCount = 3;

    /** 文档认定为陈旧的过期小时数（默认 720 小时/30 天） */
    private int staleAfterHours = 24 * 30;

    /** 彻底清理已删除文档的保留天数（默认 30 天） */
    private int documentRetentionDays = 30;

    /** 检索测试未命中率触发告警的百分比阈值（默认 20%） */
    private double evaluationNoHitWarningRate = 0.20D;

    /** 检索评测时默认的召回 TopK 数量（默认 5） */
    private int evaluationDefaultTopK = 5;
}

