package com.acme.agentstudio.interfaces.rest;

import com.acme.agentstudio.application.realtime.RealtimeTicketService;
import com.acme.agentstudio.common.response.ApiResponse;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实时通信 WebSocket 一次性鉴权 Ticket 申请 REST 控制器。
 * 负责为已登录的平台用户生成短时有效（如 30s）的凭证票据，用于替代 URLQuery 中的明文 JWT 令牌建立 WebSocket 安全连接。
 */
@Tag(name = "实时通信", description = "WebSocket 建立连接票据 Ticket 申请")
@RestController
@RequestMapping("/api/realtime")
public class RealtimeTicketController {

    /** 实时通信 Ticket 签发服务 */
    private final RealtimeTicketService ticketService;

    /**
     * 构造函数注入 Ticket 服务。
     */
    public RealtimeTicketController(RealtimeTicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 登录用户申请建立特定资源/会话 WebSocket 订阅的一次性鉴权 Ticket。
     *
     * @param user 当前登录用户
     * @param request 包含订阅资源编码与起始游标的请求
     * @return 包含临时 Ticket 的响应
     */
    @Operation(summary = "申请 WebSocket 建立连接 Ticket", description = "生成用于建立 `/ws/realtime` 安全连接的一次性临时凭证。")
    @PostMapping("/tickets")
    public ApiResponse<?> create(@AuthenticationPrincipal SecurityUser user,
                                 @RequestBody RealtimeTicketRequest request) {
        return ApiResponse.ok(ticketService.createInternal(user, request));
    }
}

