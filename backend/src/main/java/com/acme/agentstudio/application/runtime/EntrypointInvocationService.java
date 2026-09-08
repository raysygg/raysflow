package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.application.ApplicationContracts.ProtocolField;
import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.common.exception.ExternalInvocationException;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.InvocationRequest;
import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import com.acme.agentstudio.domain.application.RunDeliveryMode;
import com.acme.agentstudio.domain.application.RunTriggerSource;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunRequest;
import com.acme.agentstudio.domain.runtime.model.RunContracts.RunSubmission;
import com.acme.agentstudio.domain.workflow.model.RunType;
import com.acme.agentstudio.infrastructure.persistence.entity.ApplicationEntrypointEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationEnvironmentEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationEnvironmentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 应用入口调用归一化与路由服务（Entrypoint Invocation Service）。
 * 负责将内部用户调用、外部 API/Webhook/定时任务调用及画布草稿测试统一归一化转换为不可伪造的标准 RunRequest，
 * 并执行输入契约校验（Input Schema Validation）、幂等键清洗（Idempotency Key Sanitization）与生产版本解耦路由。
 */
@Service
public class EntrypointInvocationService {

    /** 生产环境标识 */
    private static final String PRODUCTION_ENVIRONMENT = "PRODUCTION";

    /** 幂等校验 Key 的最大允许字符长度 */
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 160;

    /** 应用入口元数据服务 */
    private final ApplicationEntrypointService entrypointService;

    /** 运行环境映射 Mapper */
    private final OrchestrationEnvironmentMapper environmentMapper;

    /** 运行 Run 应用调度服务 */
    private final RuntimeRunApplicationService runService;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入所有必要的依赖服务。
     */
    public EntrypointInvocationService(
            ApplicationEntrypointService entrypointService,
            OrchestrationEnvironmentMapper environmentMapper,
            RuntimeRunApplicationService runService,
            ObjectMapper objectMapper
    ) {
        this.entrypointService = entrypointService;
        this.environmentMapper = environmentMapper;
        this.runService = runService;
        this.objectMapper = objectMapper;
    }

    /**
     * 处理内部平台用户发起的入口调用。
     *
     * @param user 当前登录 SecurityUser
     * @param entrypointId 应用入口 ID
     * @param request 原始调用参数 Request
     * @return 提交成功的运行对象 RunSubmission
     */
    public RunSubmission invokeInternal(SecurityUser user, Long entrypointId, InvocationRequest request) {
        requireUser(user);
        ApplicationEntrypointEntity entrypoint = entrypointService.requireById(user.getTenantId(), entrypointId);
        return invoke(user, entrypoint, request, RunTriggerSource.INTERNAL_USER, null, false);
    }

    /**
     * 处理外部系统（API / Webhook）发起的入口调用。
     *
     * @param entrypoint 入口实体 ApplicationEntrypointEntity
     * @param request 原始调用参数 Request
     * @param source 触发源 RunTriggerSource
     * @return 提交成功的运行对象 RunSubmission
     */
    public RunSubmission invokeExternal(
            ApplicationEntrypointEntity entrypoint,
            InvocationRequest request,
            RunTriggerSource source
    ) {
        SecurityUser systemUser = new SecurityUser(0L, entrypoint.getTenantId(), "应用外部入口系统用户", "SYSTEM");
        return invoke(systemUser, entrypoint, request, source, null, true);
    }

    /**
     * 处理 Cron 定时调度触发的入口调用。
     *
     * @param entrypoint 入口实体 ApplicationEntrypointEntity
     * @param fireTime 本次触发的时间点
     * @return 提交成功的运行对象 RunSubmission
     */
    public RunSubmission invokeSchedule(ApplicationEntrypointEntity entrypoint, LocalDateTime fireTime) {
        String key = entrypoint.getId() + ":" + fireTime;
        SecurityUser systemUser = new SecurityUser(0L, entrypoint.getTenantId(), "应用定时入口系统用户", "SYSTEM");
        InvocationRequest request = new InvocationRequest(key, null, null, Map.of("scheduledAt", fireTime.toString()));
        return invoke(systemUser, entrypoint, request, RunTriggerSource.SCHEDULE, fireTime, true);
    }

    /**
     * 提交画布草稿测试单次运行（草稿测试不校验线上生产环境指针）。
     *
     * @param user 当前登录 SecurityUser
     * @param applicationId 应用 ID
     * @param invocation 测试调用参数
     * @return 提交成功的运行对象 RunSubmission
     */
    public RunSubmission submitDraftTest(SecurityUser user, Long applicationId, InvocationRequest invocation) {
        requireUser(user);
        String key = normalizeIdempotency(invocation == null ? null : invocation.idempotencyKey(), false);
        RunRequest request = new RunRequest(
                user.getTenantId(),
                user.getUserId(),
                applicationId,
                null,
                ApplicationEntrypointType.TEST,
                null,
                RunType.DRAFT_TEST,
                RunDeliveryMode.REALTIME,
                RunTriggerSource.STUDIO_TEST,
                key,
                invocation == null ? null : invocation.conversationId(),
                invocation == null ? null : invocation.messageId(),
                invocation == null ? Map.of() : invocation.input(),
                null
        );
        return runService.submit(user, request);
    }

    /** 核心逻辑：归一化校验并构造生产环境 RunRequest 提交给应用服务调度 */
    private RunSubmission invoke(
            SecurityUser user,
            ApplicationEntrypointEntity entrypoint,
            InvocationRequest invocation,
            RunTriggerSource source,
            LocalDateTime scheduledFireTime,
            boolean external
    ) {
        if (!Boolean.TRUE.equals(entrypoint.getEnabled())) {
            throw externalError("ENTRYPOINT_DISABLED", HttpStatus.CONFLICT, "该应用入口目前已被禁用，无法发起调用。");
        }
        Map<String, Object> input = invocation == null ? Map.of() : invocation.input();
        validateInput(entrypoint, input, external);

        String idempotencyKey = entrypoint.getId() + ":" +
                normalizeIdempotency(invocation == null ? null : invocation.idempotencyKey(), external);
        String releaseId = resolveRelease(entrypoint, external);

        RunRequest request = new RunRequest(
                entrypoint.getTenantId(),
                user.getUserId(),
                entrypoint.getApplicationId(),
                entrypoint.getId(),
                ApplicationEntrypointType.valueOf(entrypoint.getEntrypointType()),
                releaseId,
                RunType.PRODUCTION,
                RunDeliveryMode.valueOf(entrypoint.getDeliveryMode()),
                source,
                idempotencyKey,
                invocation == null ? null : invocation.conversationId(),
                invocation == null ? null : invocation.messageId(),
                input,
                scheduledFireTime
        );
        return runService.submit(user, request);
    }

    /** 解析应用生产环境绑定的已发布版本号 */
    private String resolveRelease(ApplicationEntrypointEntity entrypoint, boolean external) {
        OrchestrationEnvironmentEntity environment = environmentMapper.selectOne(
                new LambdaQueryWrapper<OrchestrationEnvironmentEntity>()
                        .eq(OrchestrationEnvironmentEntity::getTenantId, entrypoint.getTenantId())
                        .eq(OrchestrationEnvironmentEntity::getAppId, entrypoint.getApplicationId())
                        .eq(OrchestrationEnvironmentEntity::getEnvironmentCode, PRODUCTION_ENVIRONMENT)
        );
        if (environment == null || environment.getCurrentVersionId() == null) {
            throw externalError("VERSION_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "当前应用在生产环境未绑定任何可供执行的已发布版本。");
        }
        return environment.getCurrentVersionId();
    }

    /** 校验输入参数是否符合 Schema 强类型与必填约束 */
    private void validateInput(ApplicationEntrypointEntity entrypoint, Map<String, Object> input, boolean external) {
        for (ProtocolField field : readFields(entrypoint.getInputSchemaJson())) {
            Object value = input.get(field.name());
            if (field.required() && (value == null || (value instanceof String text && text.isBlank()))) {
                throw externalError("INPUT_CONTRACT_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "缺少必填输入参数字段：" + field.name());
            }
            if (value != null && !matchesType(value, field.type())) {
                throw externalError("INPUT_CONTRACT_INVALID", HttpStatus.UNPROCESSABLE_ENTITY, "输入字段 [" + field.name() + "] 的类型必须为 " + field.type());
            }
        }
    }

    /** 校验 Java 对象类型是否符合 Schema 数据类型 */
    private boolean matchesType(Object value, String type) {
        return switch (type == null ? "string" : type.toLowerCase()) {
            case "string", "text" -> value instanceof String;
            case "integer", "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List<?>;
            case "object" -> value instanceof Map<?, ?>;
            default -> true;
        };
    }

    /** 从 JSON 反序列化输入 Schema 协议字段 */
    private List<ProtocolField> readFields(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("应用入口配置的输入 Schema 无法被正常解析。", exception);
        }
    }

    /** 清洗规范幂等键 */
    private String normalizeIdempotency(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw externalError("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST, "外部 API 调用必须在 Header 或 Request Body 中提供 idempotencyKey 幂等校验键。");
            }
            return UUID.randomUUID().toString();
        }
        String normalized = value.trim();
        if (normalized.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw externalError("IDEMPOTENCY_KEY_INVALID", HttpStatus.BAD_REQUEST, "idempotencyKey 幂等校验键的字符长度不能超过 160 个字符。");
        }
        return normalized;
    }

    /** 构造带 HTTP Status 的外部异常 */
    private ExternalInvocationException externalError(String code, HttpStatus status, String message) {
        return new ExternalInvocationException(code, status, message);
    }

    /** 安全用户非空校验 */
    private void requireUser(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前登录身份信息无效，请重新登录。");
        }
    }
}

