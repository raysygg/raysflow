package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.common.exception.ExternalInvocationException;
import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformExecutionContextEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.WebhookNonceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformExecutionContextMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.WebhookNonceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 外部应用入口安全认证与防重放边界服务（External Entrypoint Authentication Service）。
 * 校验 API Key 凭证散列（Bearer Token Hash）、HMAC-SHA256 签名（Webhook Signature）、
 * 时间戳有效期窗口（5分钟）及数据库级别的 Nonce 随机数唯一性防重放攻击。
 */
@Service
public class ExternalEntrypointAuthenticationService {

    /** Webhook 签名有效防重放滑动时间窗口（5分钟） */
    private static final Duration SIGNATURE_WINDOW = Duration.ofMinutes(5);

    /** HMAC 散列加密算法 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** 应用入口元数据服务 */
    private final ApplicationEntrypointService entrypointService;

    /** Webhook Nonce 随机串防重放 Mapper */
    private final WebhookNonceMapper nonceMapper;

    /** 运行上下文 Mapper */
    private final PlatformExecutionContextMapper contextMapper;

    /**
     * 构造函数注入所需依赖服务。
     */
    public ExternalEntrypointAuthenticationService(
            ApplicationEntrypointService entrypointService,
            WebhookNonceMapper nonceMapper,
            PlatformExecutionContextMapper contextMapper
    ) {
        this.entrypointService = entrypointService;
        this.nonceMapper = nonceMapper;
        this.contextMapper = contextMapper;
    }

    /**
     * 对凭 runId 访问历史运行结果的外部 API 进行安全鉴权。
     *
     * @param runId 运行执行 ID
     * @param authorization 请求头 Authorization (Bearer Token)
     * @return 对应的应用入口实体 ApplicationEntrypointEntity
     */
    public ApplicationEntrypointEntity authenticateApiRun(String runId, String authorization) {
        PlatformExecutionContextEntity context = contextMapper.selectOne(
                new LambdaQueryWrapper<PlatformExecutionContextEntity>()
                        .eq(PlatformExecutionContextEntity::getExecutionId, runId)
        );
        if (context == null || context.getEntrypointId() == null || !"EXTERNAL_API".equals(context.getTriggerSource())) {
            throw unauthorized("RUN_ACCESS_DENIED", "指定的运行实例不存在或不允许通过外部入口跨域访问。");
        }
        ApplicationEntrypointEntity entrypoint = entrypointService.requireById(context.getTenantId(), context.getEntrypointId());

        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring(7).trim() : "";
        if (!constantTimeEquals(ApplicationEntrypointService.hash(token), entrypoint.getApiCredentialHash())) {
            throw unauthorized("API_CREDENTIAL_INVALID", "外部 API 鉴权失败，提供了无效的 Bearer 凭证 Token。");
        }
        return entrypoint;
    }

    /**
     * 校验外部 API 入口的通用请求 Bearer Token 凭证。
     *
     * @param invokeCode 入口全局调用 Code
     * @param authorization 请求头 Authorization
     * @return 应用入口实体 ApplicationEntrypointEntity
     */
    public ApplicationEntrypointEntity authenticateApi(String invokeCode, String authorization) {
        ApplicationEntrypointEntity entrypoint = requireExternalEntrypoint(invokeCode, ApplicationEntrypointType.API);
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring(7).trim() : "";

        if (token.isBlank() || !constantTimeEquals(ApplicationEntrypointService.hash(token), entrypoint.getApiCredentialHash())) {
            throw unauthorized("API_CREDENTIAL_INVALID", "应用 API 入口调用凭证无效或已被重置。");
        }
        return entrypoint;
    }

    /**
     * 校验 Webhook 回调签名的合法性、时间窗口与 Nonce 随机串防重放。
     *
     * @param invokeCode Webhook 入口调用 Code
     * @param timestamp 触发时间戳（秒级）
     * @param nonce 随机唯一串
     * @param signature HMAC 签名结果
     * @param rawBody 原始请求体 HTTP Body 字符串
     * @return 应用入口实体 ApplicationEntrypointEntity
     */
    public ApplicationEntrypointEntity authenticateWebhook(
            String invokeCode,
            String timestamp,
            String nonce,
            String signature,
            String rawBody
    ) {
        ApplicationEntrypointEntity entrypoint = requireExternalEntrypoint(invokeCode, ApplicationEntrypointType.WEBHOOK);
        Instant requestTime = parseTimestamp(timestamp);

        if (Duration.between(requestTime, Instant.now()).abs().compareTo(SIGNATURE_WINDOW) > 0) {
            throw unauthorized("WEBHOOK_REQUEST_EXPIRED", "Webhook 请求的时间戳超出 5 分钟安全滑动窗口，拒绝响应。");
        }
        if (nonce == null || nonce.isBlank()) {
            throw unauthorized("WEBHOOK_NONCE_REQUIRED", "Webhook 请求标头必须包含唯一随机数 X-Webhook-Nonce。");
        }

        String secret = entrypointService.decryptWebhookSecret(entrypoint);
        String expected = sign(secret, timestamp + "\n" + nonce + "\n" + rawBody);

        if (!constantTimeEquals(expected, signature)) {
            throw unauthorized("WEBHOOK_SIGNATURE_INVALID", "Webhook 请求 HMAC-SHA256 签名计算结果不匹配。");
        }
        saveNonce(entrypoint.getId(), nonce);
        return entrypoint;
    }

    /** 保存防重放 Nonce 至数据库并利用唯一约束防御重放 */
    private void saveNonce(Long entrypointId, String nonce) {
        WebhookNonceEntity entity = new WebhookNonceEntity();
        entity.setEntrypointId(entrypointId);
        entity.setNonceHash(ApplicationEntrypointService.hash(nonce));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plus(SIGNATURE_WINDOW));
        try {
            nonceMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new ExternalInvocationException("WEBHOOK_REPLAYED", HttpStatus.CONFLICT, "检测到重放攻击：该 Webhook 随机串 Nonce 已在有效窗口内被处理过。");
        }
    }

    /** 解析秒级 UNIX 时间戳 */
    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (Exception exception) {
            throw unauthorized("WEBHOOK_TIMESTAMP_INVALID", "Webhook 时间戳 X-Webhook-Timestamp 格式解析失败。");
        }
    }

    /** HMAC-SHA256 签名计算算法 */
    private String sign(String secret, String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return java.util.HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Webhook HMAC-SHA256 签名计算器初始化失败。", exception);
        }
    }

    /** 防恒定时间对比攻击（Constant-Time Compare）字符串比对工具 */
    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    /** 构造 401 Unauthorized 外部调用异常 */
    private ExternalInvocationException unauthorized(String code, String message) {
        return new ExternalInvocationException(code, HttpStatus.UNAUTHORIZED, message);
    }

    /** 必填校验并查找外部入口 Entity */
    private ApplicationEntrypointEntity requireExternalEntrypoint(String invokeCode, ApplicationEntrypointType type) {
        try {
            return entrypointService.requireByInvokeCode(invokeCode, type);
        } catch (RuntimeException exception) {
            throw unauthorized("ENTRYPOINT_CREDENTIAL_INVALID", "应用入口 Code 或外部调用凭证无效。");
        }
    }
}

