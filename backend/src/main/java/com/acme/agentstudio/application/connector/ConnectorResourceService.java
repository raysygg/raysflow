package com.acme.agentstudio.application.connector;

import com.acme.agentstudio.config.SecretCipher;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformCredentialRefEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformCredentialRefMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolConnectorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具连接器（Tool Connector）与加密凭据密钥解析管理服务。
 * 负责提供工具连接器及其关联加密凭据的统一解析拉取、校验，使用 AES/GCM 安全解密 API Key / Bearer Token 凭据以及新凭据的加密落盘存储。
 */
@Service
public class ConnectorResourceService {

    /** 工具连接器 Persistence Mapper */
    private final PlatformToolConnectorMapper connectorMapper;

    /** 加密凭据 Persistence Mapper */
    private final PlatformCredentialRefMapper credentialMapper;

    /** AES-GCM 敏感数据加解密服务 */
    private final SecretCipher secretCipher;

    /** 结构化重试策略序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入连接器资源服务所需 Mapper 与加密依赖。
     */
    public ConnectorResourceService(PlatformToolConnectorMapper connectorMapper, PlatformCredentialRefMapper credentialMapper,
                                     SecretCipher secretCipher, ObjectMapper objectMapper) {
        this.connectorMapper = connectorMapper;
        this.credentialMapper = credentialMapper;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据连接器 ID 查询活动状态的连接器实体及其绑定的加密凭据引用。
     *
     * @param tenantId 租户 ID
     * @param connectorId 工具连接器 ID
     * @return 包含连接器与凭据的 ConnectorResource 组合对象
     */
    public ConnectorResource resolve(Long tenantId, Long connectorId) {
        PlatformToolConnectorEntity connector = connectorMapper.selectOne(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                .eq(PlatformToolConnectorEntity::getId, connectorId)
                .eq(PlatformToolConnectorEntity::getStatus, BusinessStatus.ACTIVE));
        if (connector == null) {
            throw new IllegalArgumentException("工具连接器不可用或未启用。");
        }

        PlatformCredentialRefEntity credential = null;
        if (connector.getCredentialRefId() != null) {
            credential = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                    .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                    .eq(PlatformCredentialRefEntity::getId, connector.getCredentialRefId())
                    .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE));
        }
        if (connector.getCredentialRefId() != null && credential == null) {
            throw new IllegalArgumentException("绑定的凭据密钥不可用或已失效。");
        }
        return new ConnectorResource(connector, credential);
    }

    /**
     * 解密并获取连接器绑定的明文 API Secret 密钥。
     *
     * @param tenantId 租户 ID
     * @param connectorId 工具连接器 ID
     * @return 解密后的明文 Secret 字符串（若未绑定凭据则返回 null）
     */
    public String resolveSecret(Long tenantId, Long connectorId) {
        ConnectorResource resource = resolve(tenantId, connectorId);
        return resource.credential() == null ? null : secretCipher.decrypt(resource.credential().getCiphertext());
    }

    /**
     * 查询指定租户下所有活动的工具连接器列表。
     *
     * @param tenantId 租户 ID
     * @return 连接器实体列表
     */
    public List<PlatformToolConnectorEntity> listActive(Long tenantId) {
        return connectorMapper.selectList(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                .eq(PlatformToolConnectorEntity::getStatus, BusinessStatus.ACTIVE)
                .orderByAsc(PlatformToolConnectorEntity::getConnectorCode));
    }

    /**
     * 查询租户下全部连接器配置，并以凭证名称代替内部密文。
     */
    public List<ConnectorSummary> listManaged(Long tenantId) {
        List<PlatformToolConnectorEntity> connectors = connectorMapper.selectList(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                .orderByAsc(PlatformToolConnectorEntity::getConnectorCode));
        Map<Long, CredentialSummary> credentials = listCredentialReferences(tenantId).stream()
                .collect(Collectors.toMap(CredentialSummary::id, Function.identity()));
        List<ConnectorSummary> result = new ArrayList<>();
        for (PlatformToolConnectorEntity connector : connectors) {
            RetryPolicy retryPolicy = readRetryPolicy(connector.getRetryPolicyJson());
            CredentialSummary credential = credentials.get(connector.getCredentialRefId());
            result.add(new ConnectorSummary(connector.getId(), connector.getConnectorCode(), connector.getConnectorType(),
                    connector.getEndpoint(), connector.getCredentialRefId(), credential == null ? null : credential.name(),
                    connector.getTimeoutMs(), retryPolicy.retryCount(), retryPolicy.backoffMs(), connector.getStatus()));
        }
        return result;
    }

    /**
     * 查询租户可选择的连接器凭证引用，不返回密文。
     */
    public List<CredentialSummary> listCredentialReferences(Long tenantId) {
        return listCredentialReferences(tenantId, "CONNECTOR_SECRET");
    }

    /**
     * 按凭证用途查询租户可选择的安全引用。
     */
    public List<CredentialSummary> listCredentialReferences(Long tenantId, String credentialType) {
        return credentialMapper.selectList(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                        .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                        .eq(PlatformCredentialRefEntity::getCredentialType, credentialType)
                        .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE)
                        .orderByAsc(PlatformCredentialRefEntity::getCredentialName))
                .stream().map(entity -> new CredentialSummary(entity.getId(), entity.getCredentialName(), entity.getStatus())).toList();
    }

    /**
     * 校验指定用途的凭证引用属于当前租户。
     */
    public void requireCredentialReference(Long tenantId, Long credentialRefId, String credentialType) {
        PlatformCredentialRefEntity credential = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getId, credentialRefId)
                .eq(PlatformCredentialRefEntity::getCredentialType, credentialType)
                .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE));
        if (credential == null) {
            throw new IllegalArgumentException("安全凭证不存在、已停用或不属于当前企业。");
        }
    }

    /**
     * 保存结构化连接器配置，重试策略由服务端生成 JSON。
     */
    public ConnectorSummary saveConnector(Long tenantId, Long actorId, ConnectorCommand command) {
        validateConnector(command);
        PlatformToolConnectorEntity entity = command.id() == null ? new PlatformToolConnectorEntity() : connectorMapper.selectOne(
                new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                        .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                        .eq(PlatformToolConnectorEntity::getId, command.id()));
        if (entity == null) {
            throw new IllegalArgumentException("连接器不存在或不属于当前企业。");
        }
        if (command.credentialRefId() != null) {
            requireCredential(tenantId, command.credentialRefId());
        }
        Long duplicate = connectorMapper.selectCount(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                .eq(PlatformToolConnectorEntity::getConnectorCode, command.name().trim())
                .ne(command.id() != null, PlatformToolConnectorEntity::getId, command.id()));
        if (duplicate > 0) {
            throw new IllegalArgumentException("当前企业已存在同名连接器。");
        }
        entity.setTenantId(tenantId);
        entity.setConnectorCode(command.name().trim());
        entity.setConnectorType(command.type());
        entity.setEndpoint(command.endpoint().trim());
        entity.setCredentialRefId(command.credentialRefId());
        entity.setTimeoutMs(command.timeoutMs());
        entity.setRetryPolicyJson(writeRetryPolicy(command.retryCount(), command.backoffMs()));
        entity.setStatus(BusinessStatus.ACTIVE);
        if (entity.getId() == null) {
            entity.setCreatedBy(actorId);
            entity.setCreatedAt(LocalDateTime.now());
            connectorMapper.insert(entity);
        } else {
            connectorMapper.updateById(entity);
        }
        String credentialName = command.credentialRefId() == null ? null : requireCredential(tenantId, command.credentialRefId()).getCredentialName();
        return new ConnectorSummary(entity.getId(), entity.getConnectorCode(), entity.getConnectorType(), entity.getEndpoint(),
                entity.getCredentialRefId(), credentialName, entity.getTimeoutMs(), command.retryCount(), command.backoffMs(), entity.getStatus());
    }

    /**
     * 删除租户自有连接器配置。
     */
    public void deleteConnector(Long tenantId, Long connectorId) {
        PlatformToolConnectorEntity entity = connectorMapper.selectOne(new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                .eq(PlatformToolConnectorEntity::getId, connectorId));
        if (entity == null) {
            throw new IllegalArgumentException("连接器不存在或不属于当前企业。");
        }
        connectorMapper.deleteById(entity.getId());
    }

    /**
     * 校验连接器结构化字段。
     */
    private void validateConnector(ConnectorCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()
                || command.endpoint() == null || command.endpoint().isBlank()) {
            throw new IllegalArgumentException("连接器名称和服务地址不能为空。");
        }
        if (!Set.of("HTTP", "OPENAPI", "MCP", "WEBHOOK").contains(command.type())) {
            throw new IllegalArgumentException("请选择支持的连接器类型。");
        }
        try {
            URI endpoint = URI.create(command.endpoint().trim());
            if (!Set.of("http", "https").contains(endpoint.getScheme())) {
                throw new IllegalArgumentException("连接器服务地址只支持 HTTP 或 HTTPS。");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("请输入有效的 HTTP 或 HTTPS 服务地址。");
        }
        if (command.timeoutMs() == null || command.timeoutMs() < 1000 || command.timeoutMs() > 120000) {
            throw new IllegalArgumentException("超时时间必须在 1 秒到 120 秒之间。");
        }
        if (command.retryCount() == null || command.retryCount() < 0 || command.retryCount() > 5
                || command.backoffMs() == null || command.backoffMs() < 0 || command.backoffMs() > 60000) {
            throw new IllegalArgumentException("重试次数或等待时间超出允许范围。");
        }
    }

    /**
     * 校验连接器凭证引用的租户边界。
     */
    private PlatformCredentialRefEntity requireCredential(Long tenantId, Long credentialRefId) {
        PlatformCredentialRefEntity credential = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getId, credentialRefId)
                .eq(PlatformCredentialRefEntity::getCredentialType, "CONNECTOR_SECRET")
                .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE));
        if (credential == null) {
            throw new IllegalArgumentException("连接器凭证不存在、已停用或不属于当前企业。");
        }
        return credential;
    }

    /**
     * 序列化受控重试策略。
     */
    private String writeRetryPolicy(Integer retryCount, Integer backoffMs) {
        try {
            return objectMapper.writeValueAsString(Map.of("maxAttempts", retryCount, "backoffMs", backoffMs));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("连接器重试策略保存失败。", exception);
        }
    }

    /**
     * 读取历史重试策略并提供安全默认值，原始 JSON 不进入接口响应。
     */
    private RetryPolicy readRetryPolicy(String json) {
        try {
            if (json == null || json.isBlank()) return new RetryPolicy(0, 0);
            @SuppressWarnings("unchecked") Map<String, Object> value = objectMapper.readValue(json, Map.class);
            return new RetryPolicy(((Number) value.getOrDefault("maxAttempts", 0)).intValue(),
                    ((Number) value.getOrDefault("backoffMs", 0)).intValue());
        } catch (Exception exception) {
            return new RetryPolicy(0, 0);
        }
    }

    /**
     * 安全加密并保存一份新的 API 凭据密钥（如 API Key）。
     *
     * @param tenantId 租户 ID
     * @param name 凭据名称
     * @param type 凭据类型（如 API_KEY / BASIC_AUTH）
     * @param plaintext 待加密的明文密钥值
     * @param keyVersion 密钥版本号
     * @param actorId 创建人用户 ID
     * @return 保存的凭据实体对象
     */
    public PlatformCredentialRefEntity saveCredential(Long tenantId, String name, String type, String plaintext,
                                                       String keyVersion, Long actorId) {
        if (name == null || name.isBlank() || plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("凭证名称和内容不能为空。");
        }
        Long duplicate = credentialMapper.selectCount(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getCredentialName, name.trim()));
        if (duplicate > 0) {
            throw new IllegalArgumentException("当前企业已存在同名凭证，请更换名称。");
        }
        PlatformCredentialRefEntity entity = new PlatformCredentialRefEntity();
        entity.setTenantId(tenantId);
        entity.setCredentialName(name.trim());
        entity.setCredentialType(type);
        entity.setCiphertext(secretCipher.encrypt(plaintext));
        entity.setKeyVersion(keyVersion);
        entity.setStatus(BusinessStatus.ACTIVE);
        entity.setCreatedBy(actorId);
        entity.setCreatedAt(LocalDateTime.now());
        credentialMapper.insert(entity);
        return entity;
    }

    /** 工具连接器与其关联凭据引用的聚合契约对象 */
    public record ConnectorResource(PlatformToolConnectorEntity connector, PlatformCredentialRefEntity credential) {}

    /** 连接器结构化保存命令。 */
    public record ConnectorCommand(Long id, String name, String type, String endpoint, Long credentialRefId,
                                   Integer timeoutMs, Integer retryCount, Integer backoffMs) {}

    /** 可安全返回的连接器摘要。 */
    public record ConnectorSummary(Long id, String name, String type, String endpoint, Long credentialRefId,
                                   String credentialName, Integer timeoutMs, Integer retryCount, Integer backoffMs, String status) {}

    /** 可安全返回的凭证引用摘要。 */
    public record CredentialSummary(Long id, String name, String status) {}

    /** 内部重试策略值。 */
    private record RetryPolicy(Integer retryCount, Integer backoffMs) {}
}

