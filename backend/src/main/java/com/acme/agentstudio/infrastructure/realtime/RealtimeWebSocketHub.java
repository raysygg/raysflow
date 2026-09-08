package com.acme.agentstudio.infrastructure.realtime;

import com.acme.agentstudio.config.RealtimeProperties;
import com.acme.agentstudio.common.exception.RealtimeUnavailableException;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeConnectionStatus;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeEventEnvelope;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 本机 WebSocket 线程安全连接 hub 集中管理与消息分发组件（Realtime WebSocket Hub）。
 * 本机只保存有上限的活跃连接，实时事件通过 Redis 按资源主题跨实例分发。
 * 事件历史不进入 JVM：执行事件从数据库补发，知识进度只读取 Redis 最新快照。
 */
@Component
public class RealtimeWebSocketHub {
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeWebSocketHub.class);
    private static final String EVENT_CONNECTED = "CONNECTED";
    private static final String EVENT_PONG = "PONG";
    private static final String EXECUTION_SNAPSHOT_KEY_PREFIX = "agent-studio:realtime:execution:snapshot:";
    private static final CloseStatus OVERLOADED = new CloseStatus(1013, "服务繁忙，请稍后重连");

    private final RedisMessageListenerContainer listenerContainer;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RealtimeProperties properties;
    private final RealtimeDispatchExecutor dispatcher;
    private final Map<String, ChannelState> channels = new ConcurrentHashMap<>();
    private final Map<String, String> sessionTopics = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> actorConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger();

    public RealtimeWebSocketHub(RedisMessageListenerContainer listenerContainer, StringRedisTemplate redis,
                                ObjectMapper objectMapper, RealtimeProperties properties,
                                RealtimeDispatchExecutor dispatcher) {
        this.listenerContainer = listenerContainer;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.dispatcher = dispatcher;
    }

    public void subscribe(WebSocketSession rawSession, RealtimeSubscription subscription,
                          Supplier<List<RealtimeEventEnvelope>> replaySupplier) {
        String topicName = RealtimeTopic.of(subscription.channel(), subscription.resourceId());
        while (true) {
            ChannelState state = channels.computeIfAbsent(topicName, this::newChannelState);
            synchronized (state.monitor) {
                // 状态被最后一个连接移除后，等待中的握手必须重新获取有效状态。
                if (channels.get(topicName) != state) continue;
                subscribeToState(rawSession, subscription, replaySupplier, topicName, state);
                return;
            }
        }
    }

    private void subscribeToState(WebSocketSession rawSession, RealtimeSubscription subscription,
                                  Supplier<List<RealtimeEventEnvelope>> replaySupplier,
                                  String topicName, ChannelState state) {
        enforceResourceLimit(state);
        reserveCapacity(subscription.actorKey());
        Subscriber subscriber = null;
        try {
            subscriber = register(rawSession, subscription, state, topicName);
            List<RealtimeEventEnvelope> replay = replaySupplier == null ? List.of() : replaySupplier.get();
            sendInitialEvents(subscriber, subscription, replay);
        } catch (RuntimeException exception) {
            rollbackSubscription(rawSession, subscription, topicName, state, subscriber);
            throw exception;
        }
    }

    private void rollbackSubscription(WebSocketSession rawSession, RealtimeSubscription subscription,
                                      String topicName, ChannelState state, Subscriber subscriber) {
        if (subscriber == null) {
            releaseCapacity(subscription.actorKey());
            cleanupEmptyState(topicName, state);
            return;
        }
        remove(rawSession.getId());
    }

        /**
         * 发布publish 业务逻辑处理。
         *
         * @param envelope envelope 参数
         */
    public void publish(RealtimeEventEnvelope envelope) {
        try {
            String topic = RealtimeTopic.of(envelope.channel(), envelope.resourceId());
            String json = objectMapper.writeValueAsString(envelope);
            saveExecutionSnapshot(envelope, json);
            redis.convertAndSend(topic, json);
        } catch (Exception exception) {
            LOG.warn("实时事件发布失败，channel={}, resourceId={}, eventType={}, 原因={}",
                    envelope.channel(), envelope.resourceId(), envelope.eventType(), exception.getMessage());
            closeTopic(RealtimeTopic.of(envelope.channel(), envelope.resourceId()), OVERLOADED);
        }
    }

        /**
         * closeAllForRedisFailure 方法。
         */
    public void closeAllForRedisFailure() {
        for (String topicName : List.copyOf(channels.keySet())) closeTopic(topicName, OVERLOADED);
    }

        /**
         * sendPong 方法。
         *
         * @param rawSession rawSession 参数
         */
    public void sendPong(WebSocketSession rawSession) {
        String topic = sessionTopics.get(rawSession.getId());
        ChannelState state = topic == null ? null : channels.get(topic);
        Subscriber subscriber = state == null ? null : state.subscribers.get(rawSession.getId());
        if (subscriber == null) return;
        RealtimeEventEnvelope pong = new RealtimeEventEnvelope(subscriber.subscription.channel(),
                subscriber.subscription.resourceId(), EVENT_PONG, 0L,
                new RealtimeConnectionStatus("实时连接正常"), false);
        send(subscriber, pong);
    }

        /**
         * replayExecutionSnapshot 方法。
         *
         * @param executionId executionId 参数
         * @param afterSequence afterSequence 参数
         * @return List<RealtimeEventEnvelope> 返回对象
         */
    public List<RealtimeEventEnvelope> replayExecutionSnapshot(String executionId, long afterSequence) {
        try {
            String json = redis.opsForValue().get(executionSnapshotKey(executionId));
            if (json == null || json.isBlank()) return List.of();
            RealtimeEventEnvelope envelope = objectMapper.readValue(json, RealtimeEventEnvelope.class);
            return envelope.sequence() > afterSequence ? List.of(envelope) : List.of();
        } catch (Exception exception) {
            throw new RealtimeUnavailableException("执行事件实时快照暂时不可用，请稍后重连", exception);
        }
    }

        /**
         * unsubscribe 方法。
         *
         * @param rawSession rawSession 参数
         */
    public void unsubscribe(WebSocketSession rawSession) {
        remove(rawSession.getId());
    }

    private ChannelState newChannelState(String topicName) {
        ChannelTopic topic = new ChannelTopic(topicName);
        ChannelState state = new ChannelState(topic);
        state.listener = (message, pattern) -> onRedisMessage(topicName,
                new String(message.getBody(), StandardCharsets.UTF_8));
        return state;
    }

    private Subscriber register(WebSocketSession rawSession, RealtimeSubscription subscription,
                                ChannelState state, String topicName) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(rawSession,
                properties.getSendTimeLimitMillis(), properties.getSendBufferSizeBytes());
        Subscriber subscriber = new Subscriber(decorated, subscription,
                new AtomicLong(subscription.afterSequence()));
        boolean firstSubscriber = state.subscribers.isEmpty();
        if (firstSubscriber) listenerContainer.addMessageListener(state.listener, state.topic);
        state.subscribers.put(rawSession.getId(), subscriber);
        sessionTopics.put(rawSession.getId(), topicName);
        LOG.info("实时订阅已建立，channel={}, resourceId={}, connections={}",
                subscription.channel(), subscription.resourceId(), totalConnections.get());
        return subscriber;
    }

    private void sendInitialEvents(Subscriber subscriber, RealtimeSubscription subscription,
                                   List<RealtimeEventEnvelope> replayEvents) {
        send(subscriber, new RealtimeEventEnvelope(subscription.channel(), subscription.resourceId(),
                EVENT_CONNECTED, 0L, new RealtimeConnectionStatus("实时连接已建立"), false));
        if (replayEvents == null) return;
        for (RealtimeEventEnvelope event : replayEvents) {
            if (!subscriber.session.isOpen()) break;
            send(subscriber, event);
        }
    }

    private void onRedisMessage(String topicName, String json) {
        try {
            RealtimeEventEnvelope envelope = objectMapper.readValue(json, RealtimeEventEnvelope.class);
            ChannelState state = channels.get(topicName);
            if (state == null) return;
            synchronized (state.monitor) {
                dispatcher.execute(topicName, () -> dispatch(topicName, envelope));
            }
        } catch (Exception exception) {
            LOG.error("实时事件分发失败，topic={}, 原因={}", topicName, exception.getMessage(), exception);
            if (isQueueFull(exception)) closeTopic(topicName, OVERLOADED);
        }
    }

    private void dispatch(String topicName, RealtimeEventEnvelope envelope) {
        ChannelState state = channels.get(topicName);
        if (state == null) return;
        Collection<Subscriber> subscribers = List.copyOf(state.subscribers.values());
        for (Subscriber subscriber : subscribers) send(subscriber, envelope);
    }

    private void send(Subscriber subscriber, RealtimeEventEnvelope envelope) {
        if (envelope.sequence() > 0 && envelope.sequence() <= subscriber.cursor.get()) return;
        try {
            subscriber.session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
            if (envelope.sequence() > 0) subscriber.cursor.set(envelope.sequence());
            if (envelope.terminal()) closeAndRemove(subscriber, CloseStatus.NORMAL);
        } catch (Exception exception) {
            LOG.warn("实时事件发送失败，channel={}, resourceId={}, 原因={}",
                    subscriber.subscription.channel(), subscriber.subscription.resourceId(), exception.getMessage());
            closeAndRemove(subscriber, OVERLOADED);
        }
    }

    private void enforceResourceLimit(ChannelState state) {
        if (state.subscribers.size() >= properties.getMaxSubscribersPerResource()) {
            throw new IllegalStateException("该资源的实时订阅数已达到上限，请稍后重试");
        }
    }

    private void reserveCapacity(String actorKey) {
        int total = totalConnections.incrementAndGet();
        if (total > properties.getMaxConnectionsPerInstance()) {
            totalConnections.decrementAndGet();
            throw new IllegalStateException("当前节点实时连接数已达到上限，请稍后重试");
        }
        AtomicInteger actorCounter = actorConnections.computeIfAbsent(actorKey, ignored -> new AtomicInteger());
        if (actorCounter.incrementAndGet() > properties.getMaxConnectionsPerUser()) {
            totalConnections.decrementAndGet();
            if (actorCounter.decrementAndGet() <= 0) actorConnections.remove(actorKey, actorCounter);
            throw new IllegalStateException("当前用户的实时连接数已达到上限，请关闭无用页面后重试");
        }
    }

    private void releaseCapacity(String actorKey) {
        totalConnections.decrementAndGet();
        decrementActor(actorKey);
    }

    private void closeTopic(String topicName, CloseStatus status) {
        ChannelState state = channels.get(topicName);
        if (state == null) return;
        for (Subscriber subscriber : List.copyOf(state.subscribers.values())) closeAndRemove(subscriber, status);
    }

    private void closeAndRemove(Subscriber subscriber, CloseStatus status) {
        try {
            if (subscriber.session.isOpen()) subscriber.session.close(status);
        } catch (Exception ignored) {
            // 连接已经断开时只需要清理注册信息。
        } finally {
            remove(subscriber.session.getId());
        }
    }

    private void remove(String sessionId) {
        String topicName = sessionTopics.remove(sessionId);
        ChannelState state = topicName == null ? null : channels.get(topicName);
        if (state == null) return;
        synchronized (state.monitor) {
            Subscriber removed = state.subscribers.remove(sessionId);
            if (removed == null) return;
            releaseCapacity(removed.subscription.actorKey());
            if (state.subscribers.isEmpty()) {
                listenerContainer.removeMessageListener(state.listener, state.topic);
                channels.remove(topicName, state);
            }
            LOG.info("实时订阅已关闭，channel={}, resourceId={}, connections={}",
                    removed.subscription.channel(), removed.subscription.resourceId(), totalConnections.get());
        }
    }

    private void decrementActor(String actorKey) {
        AtomicInteger counter = actorConnections.get(actorKey);
        if (counter == null) return;
        if (counter.decrementAndGet() <= 0) actorConnections.remove(actorKey, counter);
    }

    private boolean isQueueFull(Exception exception) {
        return exception instanceof IllegalStateException
                && exception.getMessage() != null
                && exception.getMessage().contains("发送队列已满");
    }

    private void saveExecutionSnapshot(RealtimeEventEnvelope envelope, String json) {
        if (envelope.channel() != RealtimeChannel.EXECUTION_EVENTS) {
            return;
        }
        Duration ttl = Duration.ofHours(Math.max(1, properties.getExecutionSnapshotTtlHours()));
        redis.opsForValue().set(executionSnapshotKey(envelope.resourceId()), json, ttl);
    }

    private String executionSnapshotKey(String executionId) {
        return EXECUTION_SNAPSHOT_KEY_PREFIX + executionId;
    }

    private void cleanupEmptyState(String topicName, ChannelState state) {
        if (!state.subscribers.isEmpty()) return;
        listenerContainer.removeMessageListener(state.listener, state.topic);
        channels.remove(topicName, state);
    }

    private static final class ChannelState {
        private final Object monitor = new Object();
        private final ChannelTopic topic;
        private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();
        /** listener 属性 */
        private MessageListener listener;

        private ChannelState(ChannelTopic topic) {
            this.topic = topic;
        }
    }

    private record Subscriber(WebSocketSession session, RealtimeSubscription subscription, AtomicLong cursor) {
    }
}
