package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 检索增强生成 Spring 容器配置类。
 * 激活 `RagProperties` 绑定，为切片处理、Qdrant 客户端与向量检索服务提供配置 Bean。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {
}

