package com.acme.agentstudio.interfaces.websocket;

import com.acme.agentstudio.application.knowledge.KnowledgeApplicationService;
import com.acme.agentstudio.application.runtime.RuntimeRunApplicationService;
import com.acme.agentstudio.common.exception.RealtimeUnavailableException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeAccessMode;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeClientMessage;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeClientMessageType;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeEventEnvelope;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketClaims;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeSubscription;
import com.acme.agentstudio.infrastructure.rag.KnowledgeProgressBroadcaster;
import com.acme.agentstudio.infrastructure.realtime.RealtimeEventMapper;
import com.acme.agentstudio.infrastructure.realtime.RealtimeWebSocketHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台统一 WebSocket 实时长连接端点处理器（Realtime WebSocket Handler）。
 * 基于 Spring TextWebSocketHandler 框架，处理客户端长连接建立（afterConnectionEstablished）、
 * 鉴权校验、订阅绑定（webSocketHub.subscribe）、事件重放（replay）、客户端 PING/PONG 心跳维护（handleTextMessage）
 * 以及连接异常关闭（afterConnectionClosed）全生命周期。
 */
@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RealtimeWebSocketHandler.class);

    /** 4008 权限不足/未授权拒绝关闭状态码 */
    private static final CloseStatus SUBSCRIPTION_REJECTED = new CloseStatus(1008, "实时订阅无权访问");

    /** 1013 实时服务不可用关闭状态码 */
    private static final CloseStatus SERVICE_UNAVAILABLE = new CloseStatus(1013, "实时服务暂不可用");

    /** 1003 无效客户端数据包关闭状态码 */
    private static final CloseStatus INVALID_MESSAGE = new CloseStatus(1003, "仅支持心跳消息");

    /** 运行生命周期管理服务 */
    private final RuntimeRunApplicationService runService;

    /** 知识库应用服务 */
    private final KnowledgeApplicationService knowledgeService;

    /** 知识库解析切块进度广播服务 */
    private final KnowledgeProgressBroadcaster knowledgeProgressBroadcaster;

    /** 实时 WebSocket 连接 Hub 枢纽 */
    private final RealtimeWebSocketHub webSocketHub;

    /** JSON 序列化映射组件 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖服务。
     */
    public RealtimeWebSocketHandler(
            RuntimeRunApplicationService runService,
            KnowledgeApplicationService knowledgeService,
            KnowledgeProgressBroadcaster knowledgeProgressBroadcaster,
            RealtimeWebSocketHub webSocketHub,
            ObjectMapper objectMapper
    ) {
        this.runService = runService;
        this.knowledgeService = knowledgeService;
        this.knowledgeProgressBroadcaster = knowledgeProgressBroadcaster;
        this.webSocketHub = webSocketHub;
        this.objectMapper = objectMapper;
    }

    /**
     * 连接建立成功后触发：解析凭证、绑定资源订阅并自动重放断线后遗漏的事件。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        RealtimeTicketClaims claims = claims(session);
        try {
            SecurityUser user = securityUser(claims);
            RealtimeSubscription subscription = new RealtimeSubscription(
                    claims.channel(),
                    claims.resourceId(),
                    actorKey(claims),
                    claims.afterSequence()
            );
            webSocketHub.subscribe(session, subscription, () -> replay(user, claims));
        } catch (RealtimeUnavailableException | IllegalStateException exception) {
            LOG.warn("实时订阅暂时不可用，channel={}, resourceId={}, 原因={}",
                    claims.channel(), claims.resourceId(), exception.getMessage());
            webSocketHub.unsubscribe(session);
            session.close(SERVICE_UNAVAILABLE);
        } catch (Exception exception) {
            LOG.warn("实时订阅建立失败，channel={}, resourceId={}, 原因={}",
                    claims.channel(), claims.resourceId(), exception.getMessage());
            webSocketHub.unsubscribe(session);
            session.close(SUBSCRIPTION_REJECTED);
        }
    }

    /**
     * 接收并处理客户端发起的文本消息（仅响应 PING 心跳，回复 PONG）。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            RealtimeClientMessage clientMessage = objectMapper.readValue(message.getPayload(), RealtimeClientMessage.class);
            if (clientMessage.type() != RealtimeClientMessageType.PING) {
                session.close(INVALID_MESSAGE);
                return;
            }
            webSocketHub.sendPong(session);
        } catch (Exception exception) {
            LOG.debug("实时连接收到无法识别的客户端消息，sessionId={}", session.getId());
            session.close(INVALID_MESSAGE);
        }
    }

    /**
     * 连接正常或异常关闭后回调：取消 Session 绑定的所有资源订阅。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketHub.unsubscribe(session);
    }

    /**
     * 传输发生底层网络异常回调：清理订阅并尝试关闭 Session。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOG.debug("实时连接传输异常，sessionId={}, 原因={}", session.getId(), exception.getMessage());
        webSocketHub.unsubscribe(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /** 断线重连后补发在断线窗口期间丢失的历史事件列表 */
    private List<RealtimeEventEnvelope> replay(SecurityUser user, RealtimeTicketClaims claims) {
        if (claims.channel() == RealtimeChannel.KNOWLEDGE_PROGRESS) {
            Long documentId = parseDocumentId(claims.resourceId());
            knowledgeService.requireDocumentAccess(user.getTenantId(), documentId);
            return knowledgeProgressBroadcaster.replay(documentId, claims.afterSequence());
        }

        runService.detail(user, claims.resourceId());
        List<RealtimeEventEnvelope> replay = new ArrayList<>(runService.events(
                user, claims.resourceId(), claims.afterSequence()).stream()
                .map(RealtimeEventMapper::execution)
                .toList());

        long cursor = replay.stream()
                .mapToLong(RealtimeEventEnvelope::sequence)
                .max()
                .orElse(claims.afterSequence());

        replay.addAll(webSocketHub.replayExecutionSnapshot(claims.resourceId(), cursor));
        return List.copyOf(replay);
    }

    /** 从 Session Attributes 解析 TicketClaims */
    private RealtimeTicketClaims claims(WebSocketSession session) {
        Object value = session.getAttributes().get(RealtimeTicketHandshakeInterceptor.TICKET_CLAIMS_ATTRIBUTE);
        if (value instanceof RealtimeTicketClaims claims) {
            return claims;
        }
        throw new IllegalArgumentException("实时连接 Session 中缺少有效票据声明。");
    }

    /** 构建 SecurityUser 安全主体 */
    private SecurityUser securityUser(RealtimeTicketClaims claims) {
        return new SecurityUser(claims.actorId(), claims.tenantId(), claims.username(), claims.role(), claims.roles());
    }

    /** 构建 Actor 缓存辨识 Key */
    private String actorKey(RealtimeTicketClaims claims) {
        if (claims.accessMode() == null) {
            throw new IllegalArgumentException("实时连接访问模式 AccessMode 不能为空。");
        }
        if (claims.accessMode() == RealtimeAccessMode.PUBLIC) {
            return claims.tenantId() + ":PUBLIC:" + claims.resourceId();
        }
        return claims.tenantId() + ":USER:" + claims.actorId();
    }

    /** 解析文档 ID 数字 */
    private Long parseDocumentId(String resourceId) {
        try {
            return Long.parseLong(resourceId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("知识文档标识 resourceId [" + resourceId + "] 必须为合法的数字 ID。", exception);
        }
    }
}

