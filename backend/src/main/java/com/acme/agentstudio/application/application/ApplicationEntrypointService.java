package com.acme.agentstudio.application.application;

import com.acme.agentstudio.application.application.ApplicationContracts.ProtocolField;
import com.acme.agentstudio.application.workflow.OrchestrationAuthorizationService;
import com.acme.agentstudio.config.SecretCipher;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.EntrypointConfiguration;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.EntrypointSecretResult;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.SaveEntrypointRequest;
import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.domain.application.EntrypointVersionPolicy;
import com.acme.agentstudio.domain.application.RunDeliveryMode;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.ApplicationEntrypointMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 应用对外/对内开放端点（Entrypoint）管理服务。
 * 负责提供 API、Webhook、Form 表单、Cron 定时任务及 Conversation 聊天的入口配置、统一版本绑定约束、ApiKey 凭证密文初始化与轮换（Rotate Secret）、Webhook HMAC 密钥解密以及 Cron 下次触发时间计算功能。
 */
@Service
public class ApplicationEntrypointService {

    /** 发布版本已发布状态常数 */
    private static final String VERSION_STATUS_PUBLISHED = "PUBLISHED";

    /** 默认时区（Asia/Shanghai） */
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** 生成凭证随机字节数 */
    private static final int SECRET_BYTES = 32;

    /** 开放端点 Mapper */
    private final ApplicationEntrypointMapper entrypointMapper;

    /** 编排应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 编排版本 Mapper */
    private final OrchestrationVersionMapper versionMapper;

    /** 权限与授权服务 */
    private final OrchestrationAuthorizationService authorizationService;

    /** JSON 序列化映射器 */
    private final ObjectMapper objectMapper;

    /** AES-GCM 敏感数据加解密器 */
    private final SecretCipher secretCipher;

    /** 安全随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造函数注入开放端点依赖服务组件。
     */
    public ApplicationEntrypointService(ApplicationEntrypointMapper entrypointMapper,
                                         OrchestrationAppMapper appMapper,
                                         OrchestrationVersionMapper versionMapper,
                                         OrchestrationAuthorizationService authorizationService,
                                         ObjectMapper objectMapper, SecretCipher secretCipher) {
        this.entrypointMapper = entrypointMapper;
        this.appMapper = appMapper;
        this.versionMapper = versionMapper;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
        this.secretCipher = secretCipher;
    }

    /**
     * 查询指定应用下的所有开放端点配置列表。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @return 开放端点配置列表
     */
    public List<EntrypointConfiguration> list(SecurityUser user, Long appId) {
        requireApp(user, appId, "READ_DRAFT");
        return entrypointMapper.selectList(new LambdaQueryWrapper<ApplicationEntrypointEntity>()
                        .eq(ApplicationEntrypointEntity::getTenantId, user.getTenantId())
                        .eq(ApplicationEntrypointEntity::getApplicationId, appId)
                        .orderByAsc(ApplicationEntrypointEntity::getId))
                .stream().map(this::toConfiguration).toList();
    }

    /**
     * 为应用新建开放端点并初始化安全凭证（仅在创建响应中返回一次明文 Secret）。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param request 保存请求配置
     * @return 包含配置与一次性明文 Secret 的结果
     */
    @Transactional
    public EntrypointSecretResult create(SecurityUser user, Long appId, SaveEntrypointRequest request) {
        requireApp(user, appId, "EDIT_DRAFT");
        validateRequest(user.getTenantId(), appId, request);
        ApplicationEntrypointEntity entity = new ApplicationEntrypointEntity();
        entity.setTenantId(user.getTenantId());
        entity.setApplicationId(appId);
        entity.setInvokeCode(UUID.randomUUID().toString().replace("-", ""));
        apply(entity, request);
        String secret = initializeSecret(entity);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        entrypointMapper.insert(entity);
        return new EntrypointSecretResult(toConfiguration(entity), secret);
    }

    /**
     * 修改已有开放端点的名称、速率限制、状态或输入协议。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param entrypointId 端点 ID
     * @param request 保存请求配置
     * @return 更新后的端点配置
     */
    @Transactional
    public EntrypointConfiguration update(SecurityUser user, Long appId, Long entrypointId,
                                           SaveEntrypointRequest request) {
        requireApp(user, appId, "EDIT_DRAFT");
        validateRequest(user.getTenantId(), appId, request);
        ApplicationEntrypointEntity entity = requireEntry(user.getTenantId(), appId, entrypointId);
        if (!entity.getEntrypointType().equals(request.type().name())) {
            throw new IllegalArgumentException("应用入口类型创建后不能修改，请新建入口");
        }
        apply(entity, request);
        entity.setUpdatedAt(LocalDateTime.now());
        entrypointMapper.updateById(entity);
        return toConfiguration(entity);
    }

    /**
     * 轮换（Rotate）已有端点的 ApiKey / Webhook Secret 秘钥凭证，重新生成新密钥。
     *
     * @param user 当前登录用户
     * @param appId 应用 ID
     * @param entrypointId 端点 ID
     * @return 包含新明文 Secret 的结果
     */
    @Transactional
    public EntrypointSecretResult rotateSecret(SecurityUser user, Long appId, Long entrypointId) {
        requireApp(user, appId, "EDIT_DRAFT");
        ApplicationEntrypointEntity entity = requireEntry(user.getTenantId(), appId, entrypointId);
        String secret = initializeSecret(entity);
        entity.setUpdatedAt(LocalDateTime.now());
        entrypointMapper.updateById(entity);
        return new EntrypointSecretResult(toConfiguration(entity), secret);
    }

    /**
     * 按 ID 与租户查找获取端点实体对象，若不存在则抛出异常。
     *
     * @param tenantId 租户 ID
     * @param entrypointId 端点 ID
     * @return 端点实体
     */
    public ApplicationEntrypointEntity requireById(Long tenantId, Long entrypointId) {
        ApplicationEntrypointEntity entity = entrypointMapper.selectOne(
                new LambdaQueryWrapper<ApplicationEntrypointEntity>()
                        .eq(ApplicationEntrypointEntity::getTenantId, tenantId)
                        .eq(ApplicationEntrypointEntity::getId, entrypointId));
        if (entity == null) {
            throw new IllegalArgumentException("应用入口不存在或无权访问");
        }
        return entity;
    }

    /**
     * 按 invokeCode 与端点类型查找端点实体。
     *
     * @param invokeCode 唯一调用编码
     * @param type 端点类型
     * @return 端点实体
     */
    public ApplicationEntrypointEntity requireByInvokeCode(String invokeCode, ApplicationEntrypointType type) {
        ApplicationEntrypointEntity entity = entrypointMapper.selectOne(
                new LambdaQueryWrapper<ApplicationEntrypointEntity>()
                        .eq(ApplicationEntrypointEntity::getInvokeCode, invokeCode)
                        .eq(ApplicationEntrypointEntity::getEntrypointType, type.name()));
        if (entity == null) {
            throw new IllegalArgumentException("应用调用入口不存在");
        }
        return entity;
    }

    /**
     * 解密存储在端点实休中的 Webhook 签名 HMAC 密钥明文。
     *
     * @param entity 端点实体
     * @return 明文 Secret 字符串
     */
    public String decryptWebhookSecret(ApplicationEntrypointEntity entity) {
        return secretCipher.decrypt(entity.getWebhookSecretCiphertext());
    }

    /**
     * 将 Request 配置更新映射应用到 DB 实体对象。
     */
    private void apply(ApplicationEntrypointEntity entity, SaveEntrypointRequest request) {
        entity.setName(request.name().trim());
        entity.setEntrypointType(request.type().name());
        entity.setVersionPolicy(EntrypointVersionPolicy.FOLLOW_PRODUCTION.name());
        entity.setPinnedVersionId(null);
        entity.setDeliveryMode(resolveDelivery(request.type(), request.deliveryMode()).name());
        entity.setInputSchemaJson(writeFields(request.inputFields()));
        entity.setCronExpression(request.type() == ApplicationEntrypointType.SCHEDULE ? request.cronExpression() : null);
        entity.setTimezone(request.type() == ApplicationEntrypointType.SCHEDULE ? normalizeTimezone(request.timezone()) : null);
        entity.setNextFireAt(request.type() == ApplicationEntrypointType.SCHEDULE && request.enabled()
                ? nextFire(request.cronExpression(), entity.getTimezone(), LocalDateTime.now()) : null);
        entity.setEnabled(request.enabled());
    }

    /**
     * 校验入口创建/修改请求字段合法性。
     */
    private void validateRequest(Long tenantId, Long appId, SaveEntrypointRequest request) {
        if (request == null || request.type() == null) {
            throw new IllegalArgumentException("应用入口类型不能为空");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("应用入口名称不能为空");
        }
        if (request.versionPolicy() == null) {
            throw new IllegalArgumentException("应用入口版本策略不能为空");
        }
        if (request.versionPolicy() == EntrypointVersionPolicy.PINNED_VERSION) {
            throw new IllegalArgumentException("生产入口只允许跟随 PRODUCTION 活动 Release。");
        }
        resolveDelivery(request.type(), request.deliveryMode());
        if (request.type() == ApplicationEntrypointType.SCHEDULE) {
            if (request.cronExpression() == null || request.cronExpression().isBlank()) {
                throw new IllegalArgumentException("定时入口必须配置 Cron 表达式");
            }
            nextFire(request.cronExpression(), normalizeTimezone(request.timezone()), LocalDateTime.now());
        }
    }

    /**
     * 解析并确定端点的默认或合法交付模式（Delivery Mode）。
     */
    private RunDeliveryMode resolveDelivery(ApplicationEntrypointType type, RunDeliveryMode requested) {
        return switch (type) {
            case CONVERSATION, TEST -> requireDelivery(requested, RunDeliveryMode.REALTIME, type);
            case FORM -> requireDelivery(requested, RunDeliveryMode.IMMEDIATE, type);
            case WEBHOOK, SCHEDULE -> requireDelivery(requested, RunDeliveryMode.BACKGROUND, type);
            case API -> requested == null ? RunDeliveryMode.BACKGROUND : requested;
        };
    }

    /**
     * 内部校验期望的交付模式。
     */
    private RunDeliveryMode requireDelivery(RunDeliveryMode requested, RunDeliveryMode required,
                                             ApplicationEntrypointType type) {
        if (requested != null && requested != required) {
            throw new IllegalArgumentException(type + " 入口只支持" + required + "交付方式");
        }
        return required;
    }

    /**
     * 校验固定版本的有效性。
     */
    private void requirePublishedVersion(Long tenantId, Long appId, String versionId) {
        if (versionId == null || versionId.isBlank()) {
            throw new IllegalArgumentException("固定版本入口必须选择发布版本");
        }
        Long count = versionMapper.selectCount(new LambdaQueryWrapper<OrchestrationVersionEntity>()
                .eq(OrchestrationVersionEntity::getTenantId, tenantId)
                .eq(OrchestrationVersionEntity::getAppId, appId)
                .eq(OrchestrationVersionEntity::getVersionId, versionId)
                .eq(OrchestrationVersionEntity::getStatus, VERSION_STATUS_PUBLISHED));
        if (count == 0) {
            throw new IllegalArgumentException("固定版本不存在、未发布或不属于当前应用");
        }
    }

    /**
     * 初始化 API Key / Webhook 密钥凭证及其密文存储。
     */
    private String initializeSecret(ApplicationEntrypointEntity entity) {
        if (!List.of(ApplicationEntrypointType.API.name(), ApplicationEntrypointType.WEBHOOK.name())
                .contains(entity.getEntrypointType())) {
            return null;
        }
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        entity.setCredentialMask("****" + secret.substring(secret.length() - 4));
        if (ApplicationEntrypointType.API.name().equals(entity.getEntrypointType())) {
            entity.setApiCredentialHash(hash(secret));
            entity.setWebhookSecretCiphertext(null);
        } else {
            entity.setWebhookSecretCiphertext(secretCipher.encrypt(secret));
            entity.setApiCredentialHash(null);
        }
        return secret;
    }

    /**
     * 检查端点与应用的所属关系。
     */
    private ApplicationEntrypointEntity requireEntry(Long tenantId, Long appId, Long entrypointId) {
        ApplicationEntrypointEntity entity = requireById(tenantId, entrypointId);
        if (!appId.equals(entity.getApplicationId())) {
            throw new IllegalArgumentException("应用入口不属于当前应用");
        }
        return entity;
    }

    /**
     * 校验用户对应用的访问权限。
     */
    private void requireApp(SecurityUser user, Long appId, String permission) {
        if (user == null || user.getTenantId() == null || appId == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
        Long count = appMapper.selectCount(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationAppEntity::getId, appId));
        if (count == 0) {
            throw new IllegalArgumentException("应用不存在或无权访问");
        }
        authorizationService.require(user, appId, permission);
    }

    /**
     * 将实体对象转化为对外展示配置。
     */
    private EntrypointConfiguration toConfiguration(ApplicationEntrypointEntity entity) {
        return new EntrypointConfiguration(entity.getId(), entity.getApplicationId(), entity.getInvokeCode(),
                entity.getName(), ApplicationEntrypointType.valueOf(entity.getEntrypointType()),
                EntrypointVersionPolicy.valueOf(entity.getVersionPolicy()), entity.getPinnedVersionId(),
                RunDeliveryMode.valueOf(entity.getDeliveryMode()), readFields(entity.getInputSchemaJson()),
                entity.getCronExpression(), entity.getTimezone(), entity.getNextFireAt(),
                Boolean.TRUE.equals(entity.getEnabled()), entity.getCredentialMask() != null, entity.getCredentialMask());
    }

    /**
     * 规范化时区字符串，若非法则抛出异常。
     */
    private String normalizeTimezone(String timezone) {
        String value = timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone.trim();
        try {
            ZoneId.of(value);
            return value;
        } catch (Exception exception) {
            throw new IllegalArgumentException("定时入口时区无效：" + value, exception);
        }
    }

    /**
     * 根据 Cron 表达式与时区，计算基准时间以后的下一次触发时间（Next Fire Time）。
     *
     * @param expression Cron 表达式
     * @param timezone 时区字符串
     * @param base 基准计算时间
     * @return 下一次触发的时间点
     */
    public static LocalDateTime nextFire(String expression, String timezone, LocalDateTime base) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime next = CronExpression.parse(expression).next(base.atZone(zoneId));
            if (next == null) {
                throw new IllegalArgumentException("Cron 表达式没有可计算的下次执行时间");
            }
            return next.toLocalDateTime();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cron 表达式或时区无效：" + exception.getMessage(), exception);
        }
    }

    /**
     * 序列化输入契约字段列表为 JSON。
     */
    private String writeFields(List<ProtocolField> fields) {
        try {
            return objectMapper.writeValueAsString(fields == null ? List.of() : fields);
        } catch (Exception exception) {
            throw new IllegalArgumentException("入口输入契约无法保存", exception);
        }
    }

    /**
     * 反序列化 JSON 为输入契约字段列表。
     */
    private List<ProtocolField> readFields(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("入口输入契约无法解析", exception);
        }
    }

    /**
     * 计算 API Key 凭证的 SHA-256 摘要哈希字符串。
     *
     * @param value 明文 API Key 凭证
     * @return Base64 编码的哈希字符串
     */
    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("入口凭证摘要计算失败", exception);
        }
    }
}

