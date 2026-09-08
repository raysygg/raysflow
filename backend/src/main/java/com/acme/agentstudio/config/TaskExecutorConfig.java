package com.acme.agentstudio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 全局 Spring 异步 TaskExecutor 线程池配置类。
 * 隔离配置评测任务线程池（`evaluationTaskExecutor`）与实时流程图运行线程池（`runtimeExecutionExecutor`），防止后台重型任务与实时 HTTP/WebSocket 竞争资源。
 */
@Configuration
public class TaskExecutorConfig {

    /**
     * 注册 Agent 与流程质量异步评测专用线程池 Bean。
     *
     * @return Executor 线程池
     */
    @Bean("evaluationTaskExecutor")
    public Executor evaluationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("evaluation-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 注册 Runtime 实时流程图执行与节点并发调度的专用线程池 Bean。
     *
     * @return Executor 线程池
     */
    @Bean("runtimeExecutionExecutor")
    public Executor runtimeExecutionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("runtime-realtime-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}

