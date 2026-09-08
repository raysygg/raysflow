package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用生命周期 Spring 容器配置类。
 * 激活 `ApplicationLifecycleProperties` 绑定，为发布门禁与发布策略校验提供配置对象 Bean。
 */
@Configuration
@EnableConfigurationProperties(ApplicationLifecycleProperties.class)
public class ApplicationLifecycleConfig {
}

