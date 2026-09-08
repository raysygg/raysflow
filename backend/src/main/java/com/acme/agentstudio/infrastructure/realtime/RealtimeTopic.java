package com.acme.agentstudio.infrastructure.realtime;

import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;

/**
 * 实时 Pub/Sub 广播消息 PubSub Redis Topic 主题命名映射工具类（Realtime Topic）。
 * 统一基于 `agent-studio:realtime:` 前缀与 RealtimeChannel 频道及资源标识生成 Redis 订阅 Topic 键名。
 */
public final class RealtimeTopic {

    /** Redis 订阅主题 Key 前缀 */
    private static final String PREFIX = "agent-studio:realtime:";

    /** 私有构造函数，防止实例化 */
    private RealtimeTopic() {
    }

    /**
     * 拼接并生成标准的 Redis Topic 主题名称（例如 agent-studio:realtime:RUN:1001）。
     *
     * @param channel 频道类型 RealtimeChannel
     * @param resourceId 绑定的资源/实体 ID
     * @return 格式化的 Redis Topic 字符串
     */
    public static String of(RealtimeChannel channel, String resourceId) {
        if (channel == null || resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("生成 Redis 实时 Topic 时，Channel 和 ResourceId 均不能为空。");
        }
        return PREFIX + channel.name() + ":" + resourceId;
    }
}

