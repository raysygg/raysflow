package com.acme.agentstudio.config;

import com.acme.agentstudio.interfaces.websocket.RealtimeTicketHandshakeInterceptor;
import com.acme.agentstudio.interfaces.websocket.RealtimeWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

/**
 * 实时通信 WebSocket 端点注册与握手拦截配置类。
 * 注册 `/ws/realtime` 端点，装配单次 Ticket 鉴权 HandshakeInterceptor 握手拦截器与 RealtimeWebSocketHandler 处理器。
 */
@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    /** 实时通信 WebSocket 访问端点相对路径 */
    public static final String REALTIME_WEBSOCKET_PATH = "/ws/realtime";

    /** 实时 WebSocket 核心消息处理器 */
    private final RealtimeWebSocketHandler handler;

    /** 握手鉴权 Ticket 拦截器 */
    private final RealtimeTicketHandshakeInterceptor ticketInterceptor;

    /** 允许跨域建立 WebSocket 连接的前端域名数组 */
    private final String[] allowedOrigins;

    /**
     * 构造函数注入依赖与跨域域名配置。
     */
    public RealtimeWebSocketConfig(RealtimeWebSocketHandler handler,
                                   RealtimeTicketHandshakeInterceptor ticketInterceptor,
                                   @Value("${app.security.allowed-origins:http://127.0.0.1:5176}") String origins) {
        this.handler = handler;
        this.ticketInterceptor = ticketInterceptor;
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toArray(String[]::new);
    }

    /**
     * 注册 WebSocket 处理器、握手拦截器与 AllowedOrigins 跨域策略。
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, REALTIME_WEBSOCKET_PATH)
                .addInterceptors(ticketInterceptor)
                .setAllowedOrigins(allowedOrigins);
    }
}

