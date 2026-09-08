package com.acme.agentstudio.domain.runtime.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台核心主链路跨服务与领域通用规范契约集中类（Canonical Runtime Contracts）。
 * 约定应用草稿 ApplicationDraft、不可变发布版本 ReleaseReference、会话句柄 ConversationReference、
 * 运行实例 RunReference、异步任务句柄 TaskReference、节点事件 NodeEvent、执行上下文 ExecutionContext 等核心模型。
 */
public final class CanonicalRuntimeContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private CanonicalRuntimeContracts() {
    }

    /** 应用草稿摘要契约 Record */
    public record ApplicationDraft(
            String applicationId,
            String applicationCode,
            String name,
            String status,
            Long currentRevisionId,
            LocalDateTime updatedAt
    ) {
        public ApplicationDraft {
            requireText(applicationId, "应用标识不能为空。");
            requireText(applicationCode, "应用编码不能为空。");
            requireText(name, "应用名称不能为空。");
            requireText(status, "应用状态不能为空。");
        }
    }

    /** 发布版本引用契约 Record（Run 与 Session 必须强绑定不可变版本） */
    public record ReleaseReference(
            String releaseId,
            String applicationId,
            String status,
            String environment,
            String promptVersionId,
            Map<String, Object> resolvedPolicySnapshot
    ) {
        public ReleaseReference {
            requireText(releaseId, "发布版本标识不能为空。");
            requireText(applicationId, "应用标识不能为空。");
            requireText(status, "发布版本状态不能为空。");
            requireText(environment, "发布环境不能为空。");
            resolvedPolicySnapshot = (resolvedPolicySnapshot == null)
                    ? Map.of() : Map.copyOf(resolvedPolicySnapshot);
        }
    }

    /** Runtime 会话摘要契约 Record */
    public record ConversationReference(
            String conversationId,
            String applicationId,
            String releaseId,
            String status,
            LocalDateTime updatedAt
    ) {
        public ConversationReference {
            requireText(conversationId, "会话标识不能为空。");
            requireText(applicationId, "应用标识不能为空。");
            requireText(releaseId, "会话发布版本不能为空。");
            requireText(status, "会话状态不能为空。");
        }
    }

    /** Run 任务实例句柄契约 Record */
    public record RunReference(
            String runId,
            String applicationId,
            String releaseId,
            RunStatus status,
            long eventCursor,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        public RunReference {
            requireText(runId, "Run 标识不能为空。");
            requireText(applicationId, "Run 应用标识不能为空。");
            requireText(releaseId, "Run 发布版本不能为空。");
            if (status == null) {
                throw new IllegalArgumentException("Run 状态不能为空。");
            }
            if (eventCursor < 0) {
                throw new IllegalArgumentException("Run 事件游标不能为负数。");
            }
        }
    }

    /** 异步任务统一摘要契约 Record */
    public record TaskReference(
            String taskId,
            String runId,
            String applicationId,
            String taskType,
            TaskStatus status,
            int attempt,
            boolean retryable,
            LocalDateTime updatedAt
    ) {
        public TaskReference {
            requireText(taskId, "任务标识不能为空。");
            requireText(runId, "任务运行标识不能为空。");
            requireText(applicationId, "任务应用标识不能为空。");
            requireText(taskType, "任务类型不能为空。");
            if (status == null) {
                throw new IllegalArgumentException("任务状态不能为空。");
            }
            if (attempt < 0) {
                throw new IllegalArgumentException("任务尝试次数不能为负数。");
            }
        }
    }

    /** 节点链路事件统一契约 Record */
    public record NodeEvent(
            String eventId,
            String runId,
            String taskId,
            String nodeId,
            String eventType,
            String status,
            String errorCode,
            String message,
            long sequence,
            LocalDateTime occurredAt,
            Map<String, Object> summary
    ) {
        public NodeEvent {
            requireText(eventId, "节点事件标识不能为空。");
            requireText(runId, "节点事件运行标识不能为空。");
            requireText(nodeId, "节点标识不能为空。");
            requireText(eventType, "节点事件类型不能为空。");
            if (sequence < 0) {
                throw new IllegalArgumentException("节点事件序号不能为负数。");
            }
            summary = (summary == null) ? Map.of() : Map.copyOf(summary);
        }
    }

    /** 运行期只读上下文不可变引用契约 Record */
    public record ExecutionContext(
            String runId,
            String applicationId,
            String releaseId,
            String conversationId,
            Map<String, Object> input,
            Map<String, Object> variables,
            Map<String, Object> memory,
            Map<String, Object> retrieval,
            Map<String, Object> policy
    ) {
        public ExecutionContext {
            requireText(runId, "执行上下文运行标识不能为空。");
            requireText(applicationId, "执行上下文应用标识不能为空。");
            requireText(releaseId, "执行上下文发布版本不能为空。");
            input = immutable(input);
            variables = immutable(variables);
            memory = immutable(memory);
            retrieval = immutable(retrieval);
            policy = immutable(policy);
        }
    }

    /** 应用统一生命周期状态枚举 */
    public enum ApplicationStatus {
        /** 草稿编写中 */
        DRAFT,

        /** 准备就绪，允许测试沙箱运行 */
        TESTABLE,

        /** 评测门禁通过，准备发布 */
        RELEASE_READY,

        /** 已发布上线 */
        PUBLISHED,

        /** 已经禁用下线 */
        DISABLED
    }

    /** 异步任务生命周期状态枚举 */
    public enum TaskStatus {
        /** 已入队等待分配 Worker */
        QUEUED,

        /** Worker 运行中 */
        RUNNING,

        /** 挂起等待回调 */
        WAITING,

        /** 执行成功完成 */
        SUCCEEDED,

        /** 执行失败 */
        FAILED,

        /** 已主动取消 */
        CANCELLED
    }

    /** 跨接口统一标准化异常结构描述符 Record */
    public record ErrorDescriptor(
            String code,
            String message,
            String correlationId,
            List<String> details,
            boolean retryable
    ) {
        public ErrorDescriptor {
            requireText(code, "错误编码不能为空。");
            requireText(message, "错误信息不能为空。");
            details = (details == null) ? List.of() : List.copyOf(details);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Map<String, Object> immutable(Map<String, Object> value) {
        return (value == null) ? Map.of() : Map.copyOf(value);
    }
}

