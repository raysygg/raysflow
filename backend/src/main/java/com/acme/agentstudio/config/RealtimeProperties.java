package com.acme.agentstudio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 实时 WebSocket 与 SSE 通信配置属性映射类。
 * 绑定配置文件中前缀为 `app.realtime` 的控制参数，包含单实例最大连接数、心跳间隔、临时 Ticket 有效期、分发队列容量等。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.realtime")
public class RealtimeProperties {

    /** 单个服务节点实例允许的最大并发连接数（默认 2000） */
    private int maxConnectionsPerInstance = 2000;

    /** 单个用户允许建立的最大并发连接数（默认 10） */
    private int maxConnectionsPerUser = 10;

    /** 单个资源主题允许的最大订阅数（默认 100） */
    private int maxSubscribersPerResource = 100;

    /** WebSocket 消息发送超时限制（单位：毫秒，默认 10,000ms） */
    private int sendTimeLimitMillis = 10_000;

    /** WebSocket 发送缓冲区字节大小（默认 256KB） */
    private int sendBufferSizeBytes = 256 * 1024;

    /** 文本消息最大接收缓冲区字节大小（默认 64KB） */
    private int maxTextMessageBufferSize = 64 * 1024;

    /** WebSocket 会话空闲超时时间（单位：秒，默认 90s） */
    private int sessionIdleTimeoutSeconds = 90;

    /** 心跳 Ping/Pong 检测间隔（单位：秒，默认 25s） */
    private int heartbeatIntervalSeconds = 25;

    /** WebSocket 一次性鉴权 Ticket 存活时间（单位：秒，默认 30s） */
    private int ticketTtlSeconds = 30;

    /** 知识库实时索引快照缓存 TTL（单位：分钟，默认 120min） */
    private int knowledgeSnapshotTtlMinutes = 120;

    /** 最终终止状态快照缓存 TTL（单位：分钟，默认 15min） */
    private int terminalSnapshotTtlMinutes = 15;

    /** 执行轨迹快照缓存 TTL（单位：小时，默认 24h） */
    private int executionSnapshotTtlHours = 24;

    /** 异步事件分发队列缓冲容量（默认 2000） */
    private int dispatchQueueCapacity = 2000;

    /** Redis 状态健康检查延迟间隔（单位：毫秒，默认 5000ms） */
    private int redisHealthCheckDelayMillis = 5000;
}

