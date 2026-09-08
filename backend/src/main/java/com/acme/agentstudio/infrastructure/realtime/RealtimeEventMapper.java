package com.acme.agentstudio.infrastructure.realtime;

import com.acme.agentstudio.application.workflow.ExecutionEventType;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeChannel;
import com.acme.agentstudio.domain.realtime.model.RealtimeContracts.RealtimeEventEnvelope;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunEvent;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionEventEntity;

/**
 * RealtimeEvent 数据访问 Mapper 接口。
 * 提供基于 MyBatis-Plus 的数据库 CRUD 与自定义 SQL 操作。
 */
/** 将持久化执行事件转换为不暴露原始载荷的实时事件。 */
public final class RealtimeEventMapper {
    private RealtimeEventMapper() {
    }

        /**
         * execution 方法。
         *
         * @param event event 参数
         * @return static RealtimeEventEnvelope 返回对象
         */
    public static RealtimeEventEnvelope execution(PlatformExecutionEventEntity event) {
        return execution(toRunEvent(event));
    }

        /**
         * execution 方法。
         *
         * @param event event 参数
         * @return static RealtimeEventEnvelope 返回对象
         */
    public static RealtimeEventEnvelope execution(RunEvent event) {
        long sequence = event.sequence() == null ? 0L : event.sequence();
        return new RealtimeEventEnvelope(RealtimeChannel.EXECUTION_EVENTS, event.runId(),
                event.eventType(), sequence, event, ExecutionEventType.isTerminal(event.eventType()));
    }

    private static RunEvent toRunEvent(PlatformExecutionEventEntity event) {
        return new RunEvent(event.getId(), event.getExecutionId(), event.getSequenceNo(), event.getNodeId(),
                event.getEventType(), event.getStatus(), event.getSummaryJson(), event.getErrorCode(),
                event.getErrorMessage(), event.getCreatedAt());
    }
}
