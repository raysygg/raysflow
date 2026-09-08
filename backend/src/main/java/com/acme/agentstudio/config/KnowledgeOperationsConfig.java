package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库运维与质量分析 Spring 容器配置类。
 * 激活 `KnowledgeOperationsProperties` 绑定，为索引质量分析、文档同步任务调度提供配置对象 Bean。
 */
@Configuration
@EnableConfigurationProperties(KnowledgeOperationsProperties.class)
public class KnowledgeOperationsConfig {
}

