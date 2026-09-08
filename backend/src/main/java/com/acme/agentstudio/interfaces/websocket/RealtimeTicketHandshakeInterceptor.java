package com.acme.agentstudio.interfaces.websocket;

import com.acme.agentstudio.application.realtime.RealtimeTicketService;
import com.acme.agentstudio.common.exception.RealtimeUnavailableException;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 连接建立 HTTP 握手票据验证拦截器（Realtime Ticket Handshake Interceptor）。
 * 在 WebSocket 握手建立前，从 URL 查询参数 `?ticket=xxx` 提取一次性访问票据 Ticket，
 * 调用 RealtimeTicketService 原子消费并换取身份与资源声明 RealtimeTicketClaims，将验证通过的主体注入 Session Attributes。
 */
@Component
public class RealtimeTicketHandshakeInterceptor implements HandshakeInterceptor {
    public static final String TICKET_CLAIMS_ATTRIBUTE = "realtimeTicketClaims";
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeTicketHandshakeInterceptor.class);
    private static final String TICKET_PARAMETER = "ticket";

    private final RealtimeTicketService ticketService;

    public RealtimeTicketHandshakeInterceptor(RealtimeTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String ticket = queryParameters(request).getFirst(TICKET_PARAMETER);
            RealtimeTicketClaims claims = ticketService.consume(ticket);
            attributes.put(TICKET_CLAIMS_ATTRIBUTE, claims);
            return true;
        } catch (RealtimeUnavailableException exception) {
            LOG.warn("实时连接握手暂不可用，原因={}", exception.getMessage());
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        } catch (Exception exception) {
            LOG.warn("实时连接握手被拒绝，原因={}", exception.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后不保留额外状态，票据已经在 Redis 中原子消费。
    }

    private MultiValueMap<String, String> queryParameters(ServerHttpRequest request) {
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
    }
}
