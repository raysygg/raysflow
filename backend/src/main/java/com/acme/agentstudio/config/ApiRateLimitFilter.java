package com.acme.agentstudio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * API 入口请求限流过滤器。
 * 基于 Spring OncePerRequestFilter 保证每个 HTTP 请求仅过滤一次，
 * 结合 RedisRateLimitService 在分布式多实例部署下共享滑动窗口计数，防止接口滥用与过载击穿。
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    /** Redis 滑动窗口限流分布式服务 */
    private final RedisRateLimitService redisRateLimitService;

    /** 时间窗口内允许的最大请求次数阈值 */
    private final int maxRequests;

    /** 滑动窗口的时间跨度（单位：秒） */
    private final int windowSeconds;

    /**
     * 构造函数：注入限流配置与 Redis 限流组件
     *
     * @param redisRateLimitService Redis 分布式限流服务
     * @param maxRequests           从配置文件读取的单窗口最大请求数限制
     * @param windowSeconds         从配置文件读取的时间窗口秒数
     */
    public ApiRateLimitFilter(
            RedisRateLimitService redisRateLimitService,
            @Value("${app.rate-limit.max-requests:120}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisRateLimitService = redisRateLimitService;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    /**
     * 核心过滤逻辑：拦截并检查 HTTP 请求频率
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param chain    Filter 过滤链条
     * @throws ServletException Servlet 异常
     * @throws IOException      I/O 读写异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 1. 分支判断：如果不属于 /api/ 接口或属于 /api/auth/ 登录认证类接口，则跳过通用限流
        // 说明：登录类接口有专门的 LoginRateLimiter 防爆破机制，不需要叠加通用 API 限流
        if (!uri.startsWith("/api/") || uri.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 从 SecurityContext 提取当前已登录用户的安全上下文主体
        SecurityUser user = request.getUserPrincipal() instanceof org.springframework.security.core.Authentication authentication
                && authentication.getPrincipal() instanceof SecurityUser securityUser ? securityUser : null;

        // 3. 构建请求标识：未登录用户使用客户端远程 IP 地址，已登录用户使用 "租户ID:用户ID" 格式
        String identity = (user == null) ? request.getRemoteAddr() : (user.getTenantId() + ":" + user.getUserId());

        // 4. 组合最终 Redis 限流 Key：格式为 "身份标识:请求路径"
        String key = identity + ":" + uri;

        try {
            // 5. 调用 RedisRateLimitService 判断在滑动窗口内是否超额
            boolean allowed = redisRateLimitService.isAllowed(key, maxRequests, windowSeconds);

            if (!allowed) {
                // 设置 HTTP 429 Too Many Requests 响应状态码
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"请求过于频繁，请稍后再试。\"}");
                return;
            }
        } catch (RuntimeException exception) {
            // 6. 异常分支：Redis 出现网络超时或服务不可用故障时的降级熔断处理
            response.setStatus(503);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"限流服务暂时不可用，请稍后再试。\"}");
            return;
        }

        // 7. 检验通过，将请求放行传递给后续 Filter 或 Controller 处理
        chain.doFilter(request, response);
    }
}

