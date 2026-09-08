package com.acme.agentstudio.config;

import com.acme.agentstudio.common.response.ApiErrorCode;
import com.acme.agentstudio.infrastructure.persistence.entity.SysUserEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * JWT 身份认证 HTTP 过滤器。
 * 从请求头 `Authorization: Bearer <token>` 解析身份凭证，校验用户在数据库中的状态，并将安全主体绑定到 Spring SecurityContext 与 MDC 日志上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 用户处于正常启用状态的编码 */
    private static final String ACTIVE_STATUS = "ACTIVE";

    /** Authorization 请求头前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 签名与解析工具类 */
    private final JwtUtils jwtUtils;

    /** 系统用户数据库 Mapper */
    private final SysUserMapper userMapper;

    /** 安全错误响应写入器。 */
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    /**
     * 构造函数注入依赖服务。
     *
     * @param jwtUtils   JWT 工具
     * @param userMapper 用户 Mapper
     */
    public JwtAuthenticationFilter(JwtUtils jwtUtils, SysUserMapper userMapper,
                                   SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    /**
     * 核心过滤逻辑：从 HTTP 请求头解析 Bearer 令牌并初始化安全上下文。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 尝试解析并认证请求
            try {
                authenticate(request, response);
            } catch (IllegalArgumentException exception) {
                securityErrorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                        ApiErrorCode.AUTHENTICATION_EXPIRED, "登录凭证无效，请重新登录。");
            }

            // 2. 若响应未被阻断（如未直接返回 401 错误），放行请求传递
            if (!response.isCommitted()) {
                filterChain.doFilter(request, response);
            }
        } finally {
            // 3. 必须在请求结束时清理 MDC 中的租户 ID，防止线程池复用污染
            MDC.remove("tenantId");
        }
    }

    /**
     * 执行实际的 Token 解析、数据库活动状态校验与 SecurityContext 填充。
     */
    private void authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        if (token == null) {
            return;
        }

        Map<String, Object> claims = jwtUtils.validateAndParseToken(token);
        if (claims == null) {
            return;
        }

        Long userId = numberClaim(claims, "userId");
        Long tenantId = numberClaim(claims, "tenantId");

        // 绑定当前租户 ID 至日志 MDC 上下文
        MDC.put("tenantId", String.valueOf(tenantId));

        // 实时校验用户在数据库中的活动状态
        SysUserEntity currentUser = findActiveUser(userId, tenantId);
        if (currentUser == null) {
            securityErrorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    ApiErrorCode.TENANT_CONTEXT_INVALID, "账号或租户上下文已失效，请重新登录。");
            return;
        }

        // 组装安全主体并注册到 SecurityContextHolder
        SecurityUser user = securityUser(claims, userId, tenantId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 查询特定租户下处于活动状态的用户记录 */
    private SysUserEntity findActiveUser(Long userId, Long tenantId) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getStatus, ACTIVE_STATUS));
    }

    /** 根据 Claims 构造安全上下文 SecurityUser 对象 */
    private SecurityUser securityUser(Map<String, Object> claims, Long userId, Long tenantId) {
        String username = stringClaim(claims, "username");
        String role = stringClaim(claims, "role");
        Set<String> roles = rolesClaim(claims.get("roles"), role);
        return new SecurityUser(userId, tenantId, username, role, roles);
    }

    /** 解析并格式化角色集合 */
    private Set<String> rolesClaim(Object claim, String primaryRole) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (claim instanceof Iterable<?> values) {
            for (Object value : values) {
                if (value != null) {
                    roles.add(String.valueOf(value));
                }
            }
        }
        if (primaryRole != null && !primaryRole.isBlank()) {
            roles.add(primaryRole);
        }
        return roles;
    }

    /** 提取数值类型的 Claim 字段 */
    private Long numberClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("登录令牌缺少有效的" + name + "字段");
    }

    /** 提取字符串类型的 Claim 字段 */
    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : String.valueOf(value);
    }

    /** 从 Authorization 请求头解析 Bearer 字符串 */
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}

