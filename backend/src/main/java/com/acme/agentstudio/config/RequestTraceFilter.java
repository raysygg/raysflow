package com.acme.agentstudio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全局 HTTP 请求全链路追踪过滤器。
 * 从请求头读取或自动生成 `X-Request-ID` 与 `X-Trace-ID` 标识，并注入 SLF4J MDC 日志上下文，同时回写至 HTTP 响应头，方便前端与网关全链路排查诊断。
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    /**
     * 核心过滤方法：注入 requestId, traceId 与 spanId 到 MDC 日志上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 尝试从请求头提取上游网关透传的 RequestID 与 TraceID
        String requestId = RequestTraceContext.normalize(request.getHeader(RequestTraceContext.REQUEST_ID_HEADER));
        String traceId = RequestTraceContext.normalize(request.getHeader(RequestTraceContext.TRACE_ID_HEADER));

        // 2. 若上游未透传，则自动生成全局唯一的 UUID
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        if (traceId == null) {
            traceId = requestId;
        }

        // 3. 将链路标识注入 SLF4J MDC 日志上下文
        MDC.put(RequestTraceContext.REQUEST_ID_KEY, requestId);
        MDC.put(RequestTraceContext.TRACE_ID_KEY, traceId);
        MDC.put(RequestTraceContext.SPAN_ID_KEY, UUID.randomUUID().toString());

        // 4. 将 RequestID 和 TraceID 回写到 HTTP 响应头
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);
        response.setHeader(RequestTraceContext.TRACE_ID_HEADER, traceId);

        try {
            // 5. 放行请求
            filterChain.doFilter(request, response);
        } finally {
            // 6. 请求处理结束时必须移除 MDC 中的链路标识，防止线程池复用导致日志上下文污染
            MDC.remove(RequestTraceContext.REQUEST_ID_KEY);
            MDC.remove(RequestTraceContext.TRACE_ID_KEY);
            MDC.remove(RequestTraceContext.SPAN_ID_KEY);
        }
    }
}

