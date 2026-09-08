package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 运行中心任务断点恢复 Spring 容器配置类。
 * 激活 `RuntimeRecoveryProperties` 绑定，为死信队列重试、Worker 租约清理提供配置 Bean。
 */
@Configuration
@EnableConfigurationProperties(RuntimeRecoveryProperties.class)
public class RuntimeRecoveryConfig {
}

