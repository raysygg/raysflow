package com.acme.agentstudio.application.realtime;

import com.acme.agentstudio.application.knowledge.KnowledgeApplicationService;
import com.acme.agentstudio.application.runtime.ExternalEntrypointAuthenticationService;
import com.acme.agentstudio.application.runtime.RuntimeRunApplicationService;
import com.acme.agentstudio.common.exception.RealtimeUnavailableException;
import com.acme.agentstudio.config.RealtimeProperties;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeAccessMode;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketClaims;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketRequest;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeTicketResponse;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * 实时通讯（WebSocket SSE）一次性安全票据（Ticket）服务。
 * 负责为长连接通信生成短寿命（Short-lived）、原子供单次消费（One-time Consume）的加密随机 Ticket，避免在 URL 参数或日志中泄漏 JWT Token 或敏感入口 API Key。
 */
@Service
public class RealtimeTicketService {

    /** Redis 票据缓存 Key 前缀 */
    private static final String TICKET_KEY_PREFIX = "agent-studio:realtime:ticket:";

    /** WebSocket 资源连接 Relative Path 前缀 */
    private static final String WEBSOCKET_PATH_PREFIX = "/ws/realtime?ticket=";

    /** 随机 Ticket 字节数 */
    private static final int TICKET_RANDOM_BYTES = 32;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Redis Lua 脚本：原子获取并立即删除票据（实现 One-time Consumption 消费保证） */
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
                    "if value then redis.call('DEL', KEYS[1]); end; return value;", String.class);

    /** Redis 操作模板 */
    private final StringRedisTemplate redis;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 实时通信属性配置 */
    private final RealtimeProperties properties;

    /** 知识库应用服务 */
    private final KnowledgeApplicationService knowledgeService;

    /** Runtime 运行服务 */
    private final RuntimeRunApplicationService runService;

    /** 外部入口鉴权服务 */
    private final ExternalEntrypointAuthenticationService externalAuthenticationService;

    /**
     * 构造函数注入票据服务所需依赖组件。
     */
    public RealtimeTicketService(StringRedisTemplate redis, ObjectMapper objectMapper,
                                 RealtimeProperties properties, KnowledgeApplicationService knowledgeService,
                                 RuntimeRunApplicationService runService,
                                 ExternalEntrypointAuthenticationService externalAuthenticationService) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.knowledgeService = knowledgeService;
        this.runService = runService;
        this.externalAuthenticationService = externalAuthenticationService;
    }

    /**
     * 为登录用户创建内部控制台通道（如知识库同步进度或应用运行追踪）的 WebSocket 票据。
     *
     * @param user 当前登录用户
     * @param request 票据申请请求（包含频道类型与目标资源 ID）
     * @return 包含 WebSocket 连接 Path 与过期时间的票据响应
     */
    public RealtimeTicketResponse createInternal(SecurityUser user, RealtimeTicketRequest request) {
        requireUser(user);
        validateInternalAccess(user, request);
        return createClaims(request, user, RealtimeAccessMode.INTERNAL);
    }

    /**
     * 为外部 API / 公开调用方的应用运行记录创建实时 WebSocket 追踪票据。
     *
     * @param runId 运行任务 ID
     * @param authorization 外部 Bearer / ApiKey 鉴权 Header 字符串
     * @param afterSequence 续连的起始 Event 序号
     * @return 包含 WebSocket 连接 Path 与过期时间的票据响应
     */
    public RealtimeTicketResponse createPublicRun(String runId, String authorization, long afterSequence) {
        ApplicationEntrypointEntity entrypoint = externalAuthenticationService.authenticateApiRun(runId, authorization);
        SecurityUser user = new SecurityUser(0L, entrypoint.getTenantId(), "应用外部入口", "SYSTEM");
        RealtimeTicketRequest request = new RealtimeTicketRequest(
                RealtimeChannel.EXECUTION_EVENTS, runId, afterSequence);
        runService.detail(user, runId);
        return createClaims(request, user, RealtimeAccessMode.PUBLIC);
    }

    /**
     * 原子消费校验 WebSocket 连接凭证票据（读出后自动从 Redis 中物理销毁）。
     *
     * @param ticket 客户端提交的 Ticket 字符串
     * @return 票据内包含的用户与通道 Claims 载荷
     */
    public RealtimeTicketClaims consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("实时连接票据不能为空。");
        }
        try {
            String value = redis.execute(CONSUME_SCRIPT, List.of(ticketKey(ticket.trim())));
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("实时连接票据无效、已过期或已被其他连接消费。");
            }
            RealtimeTicketClaims claims = objectMapper.readValue(value, RealtimeTicketClaims.class);
            if (claims.expiresAt() == null || claims.expiresAt().isBefore(Instant.now())) {
                throw new IllegalArgumentException("实时连接票据已超过有效期，请重新申请。");
            }
            return claims;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RealtimeUnavailableException("实时连接票据服务暂时不可用，请稍后重试。", exception);
        }
    }

    /**
     * 校验内部渠道的资源访问权限。
     */
    private void validateInternalAccess(SecurityUser user, RealtimeTicketRequest request) {
        if (request.channel() == RealtimeChannel.KNOWLEDGE_PROGRESS) {
            knowledgeService.requireDocumentAccess(user.getTenantId(), parseDocumentId(request.resourceId()));
            return;
        }
        runService.detail(user, request.resourceId());
    }

    /**
     * 构造并生成票据载荷。
     */
    private RealtimeTicketResponse createClaims(RealtimeTicketRequest request, SecurityUser user,
                                                RealtimeAccessMode accessMode) {
        Instant expiresAt = Instant.now().plusSeconds(Math.max(5, properties.getTicketTtlSeconds()));
        RealtimeTicketClaims claims = new RealtimeTicketClaims(request.channel(), request.resourceId(),
                request.afterSequence(), user.getTenantId(), user.getUserId(), user.getUsername(),
                user.getRole(), user.getRoles(), accessMode, expiresAt);
        String ticket = newTicket();
        save(ticket, claims, expiresAt);
        return new RealtimeTicketResponse(WEBSOCKET_PATH_PREFIX + ticket, expiresAt,
                properties.getHeartbeatIntervalSeconds());
    }

    /**
     * 将 Ticket 及其载荷序列化保存至 Redis 并设置 TTL。
     */
    private void save(String ticket, RealtimeTicketClaims claims, Instant expiresAt) {
        try {
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            redis.opsForValue().set(ticketKey(ticket), objectMapper.writeValueAsString(claims), ttl);
        } catch (Exception exception) {
            throw new RealtimeUnavailableException("实时连接票据创建落库失败，请检查 Redis 服务状态。", exception);
        }
    }

    /**
     * 随机生成安全 URL 编码 Ticket。
     */
    private String newTicket() {
        byte[] random = new byte[TICKET_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    /**
     * 格式化 Redis Ticket Key。
     */
    private String ticketKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }

    /**
     * 解析文档 ID。
     */
    private Long parseDocumentId(String resourceId) {
        try {
            return Long.parseLong(resourceId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("知识库文档 ID 格式不正确。", exception);
        }
    }

    /**
     * 校验身份。
     */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份状态无效，请重新登录。");
        }
    }
}

