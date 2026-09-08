package com.acme.agentstudio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.List;

/**
 * JWT 令牌签名与解析工具组件。
 * 基于 HMAC-SHA256 算法实现无状态身份令牌（AccessToken）与长效刷新令牌（RefreshToken）的生成、防篡改签名校验与 Claim 载荷解析。
 */
@Component
public class JwtUtils {

    /** 密钥，必须在配置文件中指定，至少 32 字符 */
    private final String secret;

    /** Jackson JSON 映射解析器 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数：校验并注入 JWT 加密密钥。
     *
     * @param secret 应用安全配置中的 JWT 签名密钥
     */
    public JwtUtils(@Value("${app.security.jwt-secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("未配置 JWT_SECRET，应用无法启动。");
        }
        this.secret = secret;
    }

    /**
     * 生成短效访问令牌（AccessToken，默认有效期 15 分钟）。
     *
     * @param userId   用户唯一标识 ID
     * @param username 用户登录账号名
     * @param tenantId 归属租户 ID
     * @param role     主角色标识
     * @return 签名完成的 JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, Long tenantId, String role) {
        return generateToken(userId, username, tenantId, role, 15 * 60 * 1000L);
    }

    /**
     * 生成包含多角色的短效访问令牌（AccessToken，默认有效期 15 分钟）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param role     主角色
     * @param roles    用户持有的全量角色集合
     * @return 签名完成的 JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, Long tenantId, String role, Collection<String> roles) {
        return generateToken(userId, username, tenantId, role, roles, 15 * 60 * 1000L);
    }

    /**
     * 生成长效刷新令牌（RefreshToken，默认有效期 7 天）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param role     主角色
     * @return 签名完成的 JWT 字符串
     */
    public String generateRefreshToken(Long userId, String username, Long tenantId, String role) {
        return generateToken(userId, username, tenantId, role, 7 * 24 * 60 * 60 * 1000L);
    }

    /**
     * 生成包含多角色的长效刷新令牌（RefreshToken，默认有效期 7 天）。
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param tenantId 租户 ID
     * @param role     主角色
     * @param roles    角色集合
     * @return 签名完成的 JWT 字符串
     */
    public String generateRefreshToken(Long userId, String username, Long tenantId, String role, Collection<String> roles) {
        return generateToken(userId, username, tenantId, role, roles, 7 * 24 * 60 * 60 * 1000L);
    }

    /** 辅助方法：生成单角色 Token */
    private String generateToken(Long userId, String username, Long tenantId, String role, long expirationMs) {
        return generateToken(userId, username, tenantId, role, List.of(role), expirationMs);
    }

    /** 核心底层方法：组装 Header 与 Claims 载荷，计算 HMAC-SHA256 签名 */
    private String generateToken(Long userId, String username, Long tenantId, String role,
                                 Collection<String> roles, long expirationMs) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("username", username);
            payload.put("tenantId", tenantId);
            payload.put("role", role);
            payload.put("roles", roles == null ? List.of(role) : roles.stream().filter(item -> item != null && !item.isBlank()).distinct().toList());
            payload.put("exp", System.currentTimeMillis() + expirationMs);

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String headerBase64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadBase64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signatureInput = headerBase64 + "." + payloadBase64;
            String signature = sign(signatureInput, secret);

            return signatureInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("生成安全令牌失败。", e);
        }
    }

    /**
     * 校验 JWT 令牌签名合法性与时效性，并解析出 Claims 键值对。
     *
     * @param token 待校验的 JWT 字符串
     * @return 解析得到的 Claims 字典；若令牌伪造、篡改或超时则返回 null
     */
    public Map<String, Object> validateAndParseToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String headerBase64 = parts[0];
            String payloadBase64 = parts[1];
            String signature = parts[2];

            // 1. 重新计算签名比对，防篡改
            String signatureInput = headerBase64 + "." + payloadBase64;
            String expectedSignature = sign(signatureInput, secret);

            if (!expectedSignature.equals(signature)) {
                return null;
            }

            // 2. 解码 JSON 载荷
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadBase64);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            // 3. 校验 exp 过期时间
            Number exp = (Number) claims.get("exp");
            if (exp != null && exp.longValue() < System.currentTimeMillis()) {
                return null;
            }

            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    /** Base64-URL 编码 */
    private String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }

    /** HMAC-SHA256 签名计算 */
    private String sign(String input, String secretKey) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKeySpec);
        byte[] hash = sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hash);
    }
}

