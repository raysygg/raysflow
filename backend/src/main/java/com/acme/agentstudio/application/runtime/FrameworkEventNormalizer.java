package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RunStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 原生框架 Worker 事件标准归一化组件（Framework Event Normalizer）。
 * 将不同开源框架 Worker 抛出的原始事件（如 started, completed, failed, checkpoint）统一归一化映射为平台通用的 RuntimeEvent。
 */
@Component
public class FrameworkEventNormalizer {

    /** 启动事件常量 */
    private static final String EVENT_STARTED = "started";

    /** 完成事件常量 */
    private static final String EVENT_COMPLETED = "completed";

    /** 失败事件常量 */
    private static final String EVENT_FAILED = "failed";

    /** 检查点保存事件常量 */
    private static final String EVENT_CHECKPOINT = "checkpoint";

    /**
     * 将 Worker 框架层原始事件归一化转换为平台标准 RuntimeEvent。
     *
     * @param framework 来源原生框架 NativeFramework
     * @param source Worker 抛出的原始事件对象 FrameworkWorkerEvent
     * @return 归一化后的平台事件对象 RuntimeEvent
     */
    public RuntimeEvent normalize(NativeFramework framework, FrameworkWorkerEvent source) {
        if (framework == null || source == null) {
            throw new IllegalArgumentException("框架 NativeFramework 与 Worker 事件 FrameworkWorkerEvent 均不能为空。");
        }

        RuntimeEventType type = switch (source.eventName().toLowerCase()) {
            case EVENT_STARTED -> RuntimeEventType.AGENT_STARTED;
            case EVENT_COMPLETED -> RuntimeEventType.RUN_COMPLETED;
            case EVENT_CHECKPOINT -> RuntimeEventType.CONTEXT_BUILT;
            case EVENT_FAILED -> RuntimeEventType.RUN_FAILED;
            default -> throw new IllegalArgumentException("暂不支持归一化的框架事件类型：" + source.eventName());
        };

        RunStatus status = (type == RuntimeEventType.RUN_COMPLETED)
                ? RunStatus.SUCCEEDED
                : (type == RuntimeEventType.RUN_FAILED)
                ? RunStatus.FAILED
                : RunStatus.RUNNING;

        Map<String, Object> payload = Map.of("framework", framework.name(), "data", source.data());
        return new RuntimeEvent(
                UUID.randomUUID().toString(),
                source.runId(),
                source.sequence(),
                type,
                status,
                payload,
                source.occurredAt()
        );
    }

    /**
     * 框架 Worker 抛出的原始事件传输 Record。
     *
     * @param runId 运行执行 ID
     * @param sequence 事件逻辑序号
     * @param eventName 原始事件名称
     * @param data 事件携带负载数据 Map
     * @param occurredAt 事件发生时间点
     */
    public record FrameworkWorkerEvent(
            String runId,
            long sequence,
            String eventName,
            Map<String, Object> data,
            Instant occurredAt
    ) {
        public FrameworkWorkerEvent {
            if (runId == null || runId.isBlank() || sequence < 0 || eventName == null || eventName.isBlank()) {
                throw new IllegalArgumentException("框架事件的 runId、sequence 序号和 eventName 事件名称不能为空。");
            }
            data = (data == null) ? Map.of() : Map.copyOf(data);
            occurredAt = (occurredAt == null) ? Instant.now() : occurredAt;
        }
    }
}

