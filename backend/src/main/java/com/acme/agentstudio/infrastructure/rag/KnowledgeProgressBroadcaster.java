package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.config.RealtimeProperties;
import com.acme.agentstudio.common.exception.RealtimeUnavailableException;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeIndexingProgressEvent;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeEventEnvelope;
import com.acme.agentstudio.infrastructure.realtime.RealtimeWebSocketHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 知识库切片与索引生成进度 WebSocket / Redis 实时广播组件（Knowledge Progress Broadcaster）。
 * 知识索引进度通过 Redis 保存最新快照，并交给统一 WebSocket 链路实时分发。
 */
@Component
public class KnowledgeProgressBroadcaster {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeProgressBroadcaster.class);
    private static final String SNAPSHOT_KEY_PREFIX = "agent-studio:realtime:knowledge:snapshot:";
    private static final String SEQUENCE_KEY_PREFIX = "agent-studio:realtime:knowledge:sequence:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RealtimeProperties properties;
    private final RealtimeWebSocketHub webSocketHub;

    public KnowledgeProgressBroadcaster(StringRedisTemplate redis, ObjectMapper objectMapper,
                                        RealtimeProperties properties, RealtimeWebSocketHub webSocketHub) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webSocketHub = webSocketHub;
    }

        /**
         * broadcast 方法。
         *
         * @param event event 参数
         */
    public void broadcast(KnowledgeIndexingProgressEvent event) {
        if (event == null || event.documentId() == null) return;
        try {
            KnowledgeIndexingProgressEvent sequenced = event.withSequence(nextSequence(event.documentId()));
            RealtimeEventEnvelope envelope = new RealtimeEventEnvelope(RealtimeChannel.KNOWLEDGE_PROGRESS,
                    String.valueOf(event.documentId()), sequenced.phase(), sequenced.sequence(), sequenced,
                    sequenced.terminal());
            saveSnapshot(envelope, sequenced.terminal());
            webSocketHub.publish(envelope);
        } catch (Exception exception) {
            // 索引主流程不能因实时进度中间件短暂不可用而失败，最终状态仍由文档和任务表保存。
            LOG.warn("知识索引进度发布失败，documentId={}, phase={}, 原因={}",
                    event.documentId(), event.phase(), exception.getMessage());
        }
    }

        /**
         * replay 方法。
         *
         * @param documentId documentId 参数
         * @param afterSequence afterSequence 参数
         * @return List<RealtimeEventEnvelope> 返回对象
         */
    public List<RealtimeEventEnvelope> replay(Long documentId, long afterSequence) {
        try {
            String json = redis.opsForValue().get(snapshotKey(documentId));
            if (json == null || json.isBlank()) return List.of();
            RealtimeEventEnvelope envelope = objectMapper.readValue(json, RealtimeEventEnvelope.class);
            return envelope.sequence() > afterSequence ? List.of(envelope) : List.of();
        } catch (Exception exception) {
            throw new RealtimeUnavailableException("知识索引实时进度暂时不可用，请稍后重连", exception);
        }
    }

    private long nextSequence(Long documentId) {
        String key = sequenceKey(documentId);
        Long sequence = redis.opsForValue().increment(key);
        if (sequence == null) throw new IllegalStateException("无法生成知识索引进度序号");
        redis.expire(key, snapshotTtl(false));
        return sequence;
    }

    private void saveSnapshot(RealtimeEventEnvelope envelope, boolean terminal) throws Exception {
        Long documentId = Long.parseLong(envelope.resourceId());
        Duration ttl = snapshotTtl(terminal);
        redis.opsForValue().set(snapshotKey(documentId), objectMapper.writeValueAsString(envelope), ttl);
        redis.expire(sequenceKey(documentId), ttl);
    }

    private Duration snapshotTtl(boolean terminal) {
        int minutes = terminal ? properties.getTerminalSnapshotTtlMinutes()
                : properties.getKnowledgeSnapshotTtlMinutes();
        return Duration.ofMinutes(Math.max(1, minutes));
    }

    private String snapshotKey(Long documentId) {
        return SNAPSHOT_KEY_PREFIX + documentId;
    }

    private String sequenceKey(Long documentId) {
        return SEQUENCE_KEY_PREFIX + documentId;
    }
}
