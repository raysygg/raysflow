package com.acme.agentstudio.config;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全局请求与工作流执行链路上下文工具。
 * 维护 MDC 中 `requestId`, `traceId`, `runId`, `taskId`, `nodeId`, `spanId` 等属性，
 * 并提供 `TraceScope` AutoCloseable 资源句柄，方便在异步 Task 或 Worker 线程中透传与恢复链路上下文。
 */
public final class RequestTraceContext {

    /** Request-ID HTTP 请求头编码 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Trace-ID 全局追踪 HTTP 请求头编码 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC 中 Request-ID 的 Key 属性 */
    public static final String REQUEST_ID_KEY = "requestId";

    /** MDC 中 Trace-ID 的 Key 属性 */
    public static final String TRACE_ID_KEY = "traceId";

    /** MDC 中 Run-ID 运行任务的 Key 属性 */
    public static final String RUN_ID_KEY = "runId";

    /** MDC 中 Task-ID 异步子任务的 Key 属性 */
    public static final String TASK_ID_KEY = "taskId";

    /** MDC 中 Node-ID 节点算法执行的 Key 属性 */
    public static final String NODE_ID_KEY = "nodeId";

    /** MDC 中 Span-ID 跟踪跨度的 Key 属性 */
    public static final String SPAN_ID_KEY = "spanId";

    /** 标识符最大允许长度限制（防止注入极端超大字符串） */
    private static final int MAX_IDENTIFIER_LENGTH = 128;

    /** 私有构造函数，防止工具类实例化 */
    private RequestTraceContext() {
    }

    /**
     * 获取当前 MDC 中的 RequestID，若为空则自动补全生成 UUID。
     *
     * @return 有效的 RequestID 字符串
     */
    public static String currentRequestId() {
        return valueOrGenerate(MDC.get(REQUEST_ID_KEY));
    }

    /**
     * 获取当前 MDC 中的 TraceID，若为空则自动补全生成 UUID。
     *
     * @return 有效的 TraceID 字符串
     */
    public static String currentTraceId() {
        return valueOrGenerate(MDC.get(TRACE_ID_KEY));
    }

    /**
     * 获取当前 MDC 中的 SpanID，若为空则自动补全生成 UUID。
     *
     * @return 有效的 SpanID 字符串
     */
    public static String currentSpanId() {
        return valueOrGenerate(MDC.get(SPAN_ID_KEY));
    }

    /**
     * 打开一个新的 TraceScope 作用域，常用于 Worker 异步线程绑定任务上下文。
     *
     * @param requestId 请求 ID
     * @param traceId 链路 ID
     * @param runId 运行任务 ID
     * @param taskId 异步 Task ID
     * @param nodeId 当前节点 ID
     * @param spanId 当前跨度 ID
     * @return 可用 try-with-resources 自动关闭并还原上下文的 TraceScope
     */
    public static TraceScope open(String requestId, String traceId, String runId, String taskId,
                                  String nodeId, String spanId) {
        return new TraceScope(requestId, traceId, runId, taskId, nodeId, spanId);
    }

    /**
     * 动态设置或清理当前线程的 NodeID 节点标识。
     *
     * @param nodeId 节点 ID
     */
    public static void setNodeId(String nodeId) {
        String normalized = normalize(nodeId);
        if (normalized == null) {
            MDC.remove(NODE_ID_KEY);
        } else {
            MDC.put(NODE_ID_KEY, normalized);
        }
    }

    /**
     * 自动资源释放的链路上下文作用域。
     * 退出 try 代码块时恢复关闭前的旧上下文，防止线程复用污染。
     */
    public static final class TraceScope implements AutoCloseable {

        /** 保存进入作用域前的原 MDC 缓存 */
        private final Map<String, String> previous = new HashMap<>();

        private TraceScope(String requestId, String traceId, String runId, String taskId,
                           String nodeId, String spanId) {
            put(REQUEST_ID_KEY, requestId);
            put(TRACE_ID_KEY, traceId);
            put(RUN_ID_KEY, runId);
            put(TASK_ID_KEY, taskId);
            put(NODE_ID_KEY, nodeId);
            put(SPAN_ID_KEY, spanId == null || spanId.isBlank() ? UUID.randomUUID().toString() : spanId);
        }

        private void put(String key, String value) {
            previous.put(key, MDC.get(key));
            String normalized = normalize(value);
            if (normalized == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, normalized);
            }
        }

        /**
         * 还原进入作用域前的原 ThreadLocal MDC 上下文。
         */
        @Override
        public void close() {
            previous.forEach((key, value) -> {
                if (value == null) {
                    MDC.remove(key);
                } else {
                    MDC.put(key, value);
                }
            });
        }
    }

    /**
     * 规范化并截断字符串标识。
     *
     * @param value 原始字符串
     * @return 截断规范化后的有效字符串，若为空则返回 null
     */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_IDENTIFIER_LENGTH) {
            return normalized.substring(0, MAX_IDENTIFIER_LENGTH);
        }
        return normalized;
    }

    private static String valueOrGenerate(String value) {
        String normalized = normalize(value);
        return normalized == null ? UUID.randomUUID().toString() : normalized;
    }
}

