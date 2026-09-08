package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;
import com.acme.agentstudio.infrastructure.realtime.RealtimeEventMapper;
import com.acme.agentstudio.infrastructure.realtime.RealtimeWebSocketHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 工作流实时执行事件发布组件（Execution Event Publisher）。
 * 负责将已持久化的 PlatformExecutionEventEntity 转换为 RealtimeEvent 协议，并通过 Redis Pub/Sub 与 WebSocket 实时广播推送到前端页面。
 */
@Service
public class ExecutionEventPublisher {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(ExecutionEventPublisher.class);

    /** 实时 WebSocket Hub 推送组件 */
    private final RealtimeWebSocketHub webSocketHub;

    /**
     * 构造函数注入 WebSocket Hub 依赖。
     */
    public ExecutionEventPublisher(RealtimeWebSocketHub webSocketHub) {
        this.webSocketHub = webSocketHub;
    }

    /**
     * 将数据库中记录的平台执行事件实时广播推送至前端观察页面。
     *
     * @param event 已持久化的平台执行事件实体
     */
    public void publish(PlatformExecutionEventEntity event) {
        try {
            webSocketHub.publish(RealtimeEventMapper.execution(event));
        } catch (Exception exception) {
            log.warn("工作流执行事件实时广播失败，executionId=[{}], sequence=[{}], 原因：{}",
                    event.getExecutionId(), event.getSequenceNo(), exception.getMessage());
        }
    }
}

