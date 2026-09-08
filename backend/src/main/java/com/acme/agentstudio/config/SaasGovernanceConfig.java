package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SaaS 多租户治理 Spring 容器配置类。
 * 激活 `SaasGovernanceProperties` 绑定，为配额审计、对账导出与采用度分析提供配置 Bean。
 */
@Configuration
@EnableConfigurationProperties(SaasGovernanceProperties.class)
public class SaasGovernanceConfig {
}

