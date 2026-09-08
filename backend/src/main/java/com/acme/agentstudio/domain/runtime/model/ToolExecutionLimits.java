package com.acme.agentstudio.domain.runtime.model;

import java.util.Set;

/**
 * 外部工具/MCP 沙箱环境下的资源物理边界限额配置 Record（Tool Execution Limits）。
 * 限制最大请求字节数 maxRequestBytes、最大响应字节数 maxResponseBytes、最大执行时长毫秒数 maxDurationMillis、
 * 每分钟最大调用次数上限 maxCallsPerMinute 以及允许读写的根目录路径白名单集合 allowedFileRoots。
 *
 * @param maxRequestBytes 最大允许发出的 HTTP/RPC 请求字节数
 * @param maxResponseBytes 最大允许接收的响应字节数
 * @param maxDurationMillis 单次工具调用的超时时间（毫秒）
 * @param maxCallsPerMinute 频率控制：每分钟最大调用次数上限
 * @param allowedFileRoots 允许本地工具访问的磁盘根路径目录白名单集合
 */
public record ToolExecutionLimits(
        long maxRequestBytes,
        long maxResponseBytes,
        long maxDurationMillis,
        int maxCallsPerMinute,
        Set<String> allowedFileRoots
) {
    /** 默认最大请求大小：1 MB */
    public static final long DEFAULT_MAX_REQUEST_BYTES = 1_048_576L;

    /** 默认最大响应大小：10 MB */
    public static final long DEFAULT_MAX_RESPONSE_BYTES = 10_485_760L;

    /** 默认超时时间：2 分钟（120,000 毫秒） */
    public static final long DEFAULT_MAX_DURATION_MILLIS = 120_000L;

    /** 默认每分钟最高调用次数：60 次 */
    public static final int DEFAULT_MAX_CALLS_PER_MINUTE = 60;

    /** 紧凑构造函数做输入校验 */
    public ToolExecutionLimits {
        if (maxRequestBytes < 1 || maxResponseBytes < 1 || maxDurationMillis < 1 || maxCallsPerMinute < 1) {
            throw new IllegalArgumentException("工具资源限制必须大于零。");
        }
        allowedFileRoots = (allowedFileRoots == null) ? Set.of() : Set.copyOf(allowedFileRoots);
    }

    /**
     * 构建默认工具执行限额策略。
     *
     * @return 默认的 ToolExecutionLimits 实例
     */
    public static ToolExecutionLimits defaults() {
        return new ToolExecutionLimits(DEFAULT_MAX_REQUEST_BYTES, DEFAULT_MAX_RESPONSE_BYTES,
                DEFAULT_MAX_DURATION_MILLIS, DEFAULT_MAX_CALLS_PER_MINUTE, Set.of());
    }
}

