package com.acme.agentstudio.infrastructure.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 实时广播通道健康监控器与连接断开断路保护器（Realtime Redis Health Monitor）。
 * Redis 中断时主动关闭本机实时连接，让客户端通过重连和持久化事件补发恢复。
 */
@Component
public class RealtimeRedisHealthMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeRedisHealthMonitor.class);
    private static final String PONG = "PONG";

    private final StringRedisTemplate redis;
    private final RealtimeWebSocketHub webSocketHub;
    private final AtomicBoolean available = new AtomicBoolean(true);

    public RealtimeRedisHealthMonitor(StringRedisTemplate redis, RealtimeWebSocketHub webSocketHub) {
        this.redis = redis;
        this.webSocketHub = webSocketHub;
    }

        /**
         * 校验check 业务逻辑处理。
         */
    @Scheduled(fixedDelayString = "${app.realtime.redis-health-check-delay-millis:5000}")
    public void check() {
        RedisConnection connection = null;
        try {
            connection = redis.getConnectionFactory().getConnection();
            String response = connection.ping();
            if (!PONG.equalsIgnoreCase(response)) throw new IllegalStateException("Redis 未返回 PONG");
            if (available.compareAndSet(false, true)) LOG.info("Redis 实时分发连接已恢复");
        } catch (Exception exception) {
            if (available.compareAndSet(true, false)) {
                LOG.error("Redis 实时分发连接中断，正在关闭本机 WebSocket 连接", exception);
                webSocketHub.closeAllForRedisFailure();
            }
        } finally {
            if (connection != null) connection.close();
        }
    }
}
