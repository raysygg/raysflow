package com.acme.agentstudio.config;

import com.acme.agentstudio.common.response.ApiErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全策略配置类。
 * 负责配置无状态（STATELESS） Session 策略、CORS 跨域资源共享、放行白名单路径、密码 BCrypt 强加密强度的 Bean 注册与 Filter 顺序装配。
 */
@Configuration
public class SecurityConfig {

    /** JWT 认证过滤器 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** API 分布式限流过滤器 */
    private final ApiRateLimitFilter apiRateLimitFilter;

    /** 认证和授权失败 JSON 响应写入器。 */
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    /** 允许跨域调用的前端源地址（以逗号分隔） */
    private final String allowedOrigins;

    /**
     * 构造函数注入过滤器与跨域白名单配置。
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ApiRateLimitFilter apiRateLimitFilter,
                          SecurityErrorResponseWriter securityErrorResponseWriter,
                          @Value("${app.security.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * 配置 HTTP 安全过滤链（SecurityFilterChain）。
     *
     * @param http HttpSecurity 链构建器
     * @return 编译构建的 SecurityFilterChain
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用传统 Session CSRF 保护（架构采用无状态 JWT 方案）
                .csrf(csrf -> csrf.disable())
                // 2. 配置 CORS 跨域源规则
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 严格使用无状态 Session 策略
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                securityErrorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        ApiErrorCode.AUTHENTICATION_EXPIRED,
                                        "登录状态已失效，请重新登录。"))
                        .accessDeniedHandler((request, response, exception) ->
                                securityErrorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                                        ApiErrorCode.FORBIDDEN,
                                        "当前账号没有访问该功能的权限。")))
                // 4. 路由拦截规则定义
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册、刷新 Token 和登出接口放行
                        .requestMatchers("/api/auth/login", "/api/auth/register-tenant", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        // OpenApi 与 Swagger 页面放行
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 外部 Webhook 和 Open API 放行（由底层控制层自行做 AppId/Secret 校验）
                        .requestMatchers("/api/public/v1/**").permitAll()
                        // WebSocket 通信端点放行
                        .requestMatchers(RealtimeWebSocketConfig.REALTIME_WEBSOCKET_PATH).permitAll()
                        // 所有以 /api/ 开头的内部业务接口必须通过身份认证
                        .requestMatchers("/api/**").authenticated()
                        // 其他前端静态资源等放行
                        .anyRequest().permitAll()
                )
                // 5. 注册 JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 6. 注册 API 限流过滤器在 JWT 认证完成之后
        http.addFilterAfter(apiRateLimitFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 注册密码哈希强加密器（BCrypt，强度因子 12）。
     *
     * @return PasswordEncoder 密码加密与比对工具
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** 构造 CorsConfigurationSource 对象 */
    private UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException("app.security.allowed-origins 不能为空。");
        }

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

