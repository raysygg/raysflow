package com.acme.agentstudio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.concurrent.Executor;

/**
 * 实时通信 Redis Pub/Sub 与 WebSocket 容器配置类。
 * 配置分布式 Pub/Sub 异步监听线程池、RedisMessageListenerContainer 容器以及底层 Servlet WebSocket 的缓冲区与超时参数。
 */
@Configuration
@EnableConfigurationProperties(RealtimeProperties.class)
public class RealtimeRedisConfig {

    /**
     * 创建 Redis 实时 Pub/Sub 事件监听专用的异步 TaskExecutor 线程池。
     *
     * @return Executor 线程池对象
     */
    @Bean("realtimeRedisListenerExecutor")
    public Executor realtimeRedisListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("realtime-redis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 注册 Redis 消息监听器容器 Bean。
     *
     * @param connectionFactory Redis 连接工厂
     * @param realtimeRedisListenerExecutor 专用监听线程池
     * @return RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer realtimeMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("realtimeRedisListenerExecutor") Executor realtimeRedisListenerExecutor) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(realtimeRedisListenerExecutor);
        return container;
    }

    /**
     * 配置 Servlet 层的底层 WebSocket 容器属性（如最大空闲超时时间与缓冲区上限）。
     *
     * @param properties 实时通信属性配置对象
     * @return ServletServerContainerFactoryBean 工厂 Bean
     */
    @Bean
    public ServletServerContainerFactoryBean webSocketContainer(RealtimeProperties properties) {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxSessionIdleTimeout(properties.getSessionIdleTimeoutSeconds() * 1000L);
        container.setMaxTextMessageBufferSize(properties.getMaxTextMessageBufferSize());
        return container;
    }
}

