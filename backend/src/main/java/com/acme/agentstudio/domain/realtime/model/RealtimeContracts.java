package com.acme.agentstudio.domain.realtime.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * WebSocket 与 Redis 实时长链接、广播事件与一次性 Ticket 消费强类型契约类（Realtime Contracts）。
 * 约定通道 RealtimeChannel、访问模式 RealtimeAccessMode、客户端消息 RealtimeClientMessageType，
 * 以及 Ticket 申请/响应/Claims 签名声明 Record 与事件信封包 RealtimeEventEnvelope。
 */
public final class RealtimeContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private RealtimeContracts() {
    }

    /** 实时长连接订阅频道枚举 */
    public enum RealtimeChannel {
        /** 知识库文档导入切块解析进度频道 */
        KNOWLEDGE_PROGRESS,

        /** Agent 运行任务事件与节点状态变化频道 */
        EXECUTION_EVENTS
    }

    /** 实时连接访问模式枚举 */
    public enum RealtimeAccessMode {
        /** 平台内部已登录用户访问 */
        INTERNAL,

        /** 外部应用公开调用端点（网页嵌入或 SDK 匿名访问） */
        PUBLIC
    }

    /** 客户端发送给服务端的消息类型枚举 */
    public enum RealtimeClientMessageType {
        /** PING 心跳消息 */
        PING
    }

    /** 申请一次性 WebSocket 访问 Ticket 请求 Record */
    public record RealtimeTicketRequest(
            RealtimeChannel channel,
            String resourceId,
            long afterSequence
    ) {
        /** 紧凑构造函数做输入属性断言 */
        public RealtimeTicketRequest {
            if (channel == null) {
                throw new IllegalArgumentException("实时订阅频道 channel 不能为空。");
            }
            if (resourceId == null || resourceId.isBlank()) {
                throw new IllegalArgumentException("订阅绑定的资源标识 resourceId 不能为空。");
            }
            if (afterSequence < 0) {
                throw new IllegalArgumentException("断线重放事件起始序号 afterSequence 不能小于零。");
            }
            resourceId = resourceId.trim();
        }
    }

    /** 光标偏移请求 Record */
    public record RealtimeCursorRequest(long afterSequence) {
        public RealtimeCursorRequest {
            if (afterSequence < 0) {
                throw new IllegalArgumentException("断线重放事件起始序号 afterSequence 不能小于零。");
            }
        }
    }

    /** 申请一次性 Ticket 响应 Record */
    public record RealtimeTicketResponse(
            String websocketPath,
            Instant expiresAt,
            int heartbeatIntervalSeconds
    ) {
    }

    /** 实时订阅入口描述符 Record */
    public record RealtimeSubscriptionDescriptor(
            RealtimeChannel channel,
            String resourceId,
            String ticketEndpoint
    ) {
    }

    /** WebSocket 握手时消费 Ticket 验签后的 Token 声明声明凭据 Record */
    public record RealtimeTicketClaims(
            RealtimeChannel channel,
            String resourceId,
            long afterSequence,
            Long tenantId,
            Long actorId,
            String username,
            String role,
            Set<String> roles,
            RealtimeAccessMode accessMode,
            Instant expiresAt
    ) {
        /** 紧凑构造函数防护角色集合复制 */
        public RealtimeTicketClaims {
            roles = copyRoles(roles);
        }

        private static Set<String> copyRoles(Set<String> source) {
            LinkedHashSet<String> copied = new LinkedHashSet<>();
            if (source != null) {
                source.stream()
                        .filter(item -> item != null && !item.isBlank())
                        .forEach(copied::add);
            }
            return Set.copyOf(copied);
        }
    }

    /** 平台统一推送到客户端的实时事件信封包 Record */
    public record RealtimeEventEnvelope(
            RealtimeChannel channel,
            String resourceId,
            String eventType,
            long sequence,
            Object payload,
            boolean terminal
    ) {
    }

    /** 客户端推送到服务端的 JSON 包装消息 Record */
    public record RealtimeClientMessage(RealtimeClientMessageType type) {
    }

    /** 实时连接状态消息 Record */
    public record RealtimeConnectionStatus(String message) {
    }

    /** 服务端内存 Hub 管理的单个 Session 订阅关系 Record */
    public record RealtimeSubscription(
            RealtimeChannel channel,
            String resourceId,
            String actorKey,
            long afterSequence
    ) {
    }
}

