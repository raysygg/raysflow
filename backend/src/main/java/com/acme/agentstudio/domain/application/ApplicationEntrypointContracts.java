package com.acme.agentstudio.domain.application;

import com.acme.agentstudio.application.application.ApplicationContracts.ProtocolField;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生产环境 Agent/Workflow 对外应用入口 (Entrypoint) 跨模块契约类（Application Entrypoint Contracts）。
 * 集中管理 Webhook, API, 表单、定时 Cron 入口配置 EntrypointConfiguration, 保存请求与一次性凭证密钥结果模型。
 */
public final class ApplicationEntrypointContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private ApplicationEntrypointContracts() {
    }

    /** 应用入口详细配置实体 Record */
    public record EntrypointConfiguration(
            Long id,
            Long applicationId,
            String invokeCode,
            String name,
            ApplicationEntrypointType type,
            EntrypointVersionPolicy versionPolicy,
            String pinnedVersionId,
            RunDeliveryMode deliveryMode,
            List<ProtocolField> inputFields,
            String cronExpression,
            String timezone,
            LocalDateTime nextFireAt,
            boolean enabled,
            boolean credentialConfigured,
            String credentialMask
    ) {
        public EntrypointConfiguration {
            inputFields = (inputFields == null) ? List.of() : List.copyOf(inputFields);
        }
    }

    /** 保存入口配置请求 Record */
    public record SaveEntrypointRequest(
            String name,
            ApplicationEntrypointType type,
            EntrypointVersionPolicy versionPolicy,
            String pinnedVersionId,
            RunDeliveryMode deliveryMode,
            List<ProtocolField> inputFields,
            String cronExpression,
            String timezone,
            boolean enabled
    ) {
    }

    /** 入口 API Key/Secret 首次生成一次性返回明文凭证 Record */
    public record EntrypointSecretResult(
            EntrypointConfiguration entrypoint,
            String secret
    ) {
    }

    /** 入口触发调用请求载荷 Record */
    public record InvocationRequest(
            String idempotencyKey,
            String conversationId,
            String messageId,
            Map<String, Object> input
    ) {
        public InvocationRequest {
            input = (input == null) ? Map.of() : Map.copyOf(input);
        }
    }
}

