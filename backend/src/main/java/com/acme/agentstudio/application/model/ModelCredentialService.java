package com.acme.agentstudio.application.model;

import com.acme.agentstudio.config.SecretCipher;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformCredentialRefEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformCredentialRefMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型凭证引用服务，只向产品层暴露凭证元数据与维护接口，并在模型基础设施内部安全解析明文。
 */
@Service
public class ModelCredentialService {

    /** 模型 API 密钥凭证类型。 */
    public static final String MODEL_API_KEY = "MODEL_API_KEY";

    /** 当前凭证加密版本。 */
    private static final String KEY_VERSION = "v1";

    /** 凭证引用持久化接口。 */
    private final PlatformCredentialRefMapper credentialMapper;

    /** 模型底座配置持久化接口。 */
    private final SysModelConfigMapper sysModelConfigMapper;

    /** 敏感值加解密服务。 */
    private final SecretCipher secretCipher;

    /**
     * 注入凭证持久化、模型持久化与加解密依赖。
     *
     * @param credentialMapper 凭证持久化接口
     * @param sysModelConfigMapper 模型配置持久化接口
     * @param secretCipher 敏感值加解密服务
     */
    public ModelCredentialService(PlatformCredentialRefMapper credentialMapper,
                                  SysModelConfigMapper sysModelConfigMapper,
                                  SecretCipher secretCipher) {
        this.credentialMapper = credentialMapper;
        this.sysModelConfigMapper = sysModelConfigMapper;
        this.secretCipher = secretCipher;
    }

    /**
     * 查询租户可选择的活动模型凭证，返回包含绑定模型统计的元数据。
     *
     * @param tenantId 租户标识
     * @return 凭证引用元数据列表
     */
    public List<CredentialReference> list(Long tenantId) {
        List<PlatformCredentialRefEntity> entities = credentialMapper.selectList(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getCredentialType, MODEL_API_KEY)
                .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE)
                .orderByAsc(PlatformCredentialRefEntity::getCredentialName));

        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询当前租户下所有关联了凭证的模型配置，建立凭证到模型名称的映射
        List<SysModelConfigEntity> models = sysModelConfigMapper.selectList(new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getTenantId, tenantId)
                .isNotNull(SysModelConfigEntity::getCredentialRefId));

        Map<Long, List<String>> boundModelsMap = models.stream()
                .filter(m -> m.getCredentialRefId() != null)
                .collect(Collectors.groupingBy(
                        SysModelConfigEntity::getCredentialRefId,
                        Collectors.mapping(SysModelConfigEntity::getModelName, Collectors.toList())
                ));

        return entities.stream()
                .map(entity -> {
                    List<String> boundNames = boundModelsMap.getOrDefault(entity.getId(), Collections.emptyList());
                    return new CredentialReference(
                            entity.getId(),
                            entity.getCredentialName(),
                            entity.getStatus(),
                            entity.getCreatedAt(),
                            entity.getRotatedAt(),
                            boundNames,
                            boundNames.size()
                    );
                })
                .toList();
    }

    /**
     * 创建只显示一次明文的模型凭证引用，接口响应仅返回引用元数据。
     *
     * @param tenantId 租户标识
     * @param actorId 操作人标识
     * @param name 凭证名称
     * @param secret 凭证明文密钥
     * @return 凭证引用元数据
     */
    public CredentialReference create(Long tenantId, Long actorId, String name, String secret) {
        if (name == null || name.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("凭证名称和密钥内容不能为空。");
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
        entity.setCredentialType(MODEL_API_KEY);
        entity.setCiphertext(secretCipher.encrypt(secret.trim()));
        entity.setKeyVersion(KEY_VERSION);
        entity.setStatus(BusinessStatus.ACTIVE);
        entity.setCreatedBy(actorId);
        entity.setCreatedAt(LocalDateTime.now());
        credentialMapper.insert(entity);
        return new CredentialReference(entity.getId(), entity.getCredentialName(), entity.getStatus(),
                entity.getCreatedAt(), null, Collections.emptyList(), 0);
    }

    /**
     * 更新已有模型凭证的名称，或安全轮换（更新）其 API 密钥内容。
     *
     * @param tenantId 租户标识
     * @param id 凭证标识
     * @param name 新凭证名称（若提供）
     * @param secret 新密钥内容（若提供则重新加密轮换）
     * @return 更新后的凭证元数据
     */
    public CredentialReference update(Long tenantId, Long id, String name, String secret) {
        if (id == null) {
            throw new IllegalArgumentException("请指定要更新的模型凭证。");
        }
        PlatformCredentialRefEntity entity = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getId, id)
                .eq(PlatformCredentialRefEntity::getCredentialType, MODEL_API_KEY));
        if (entity == null) {
            throw new IllegalArgumentException("模型凭证不存在或不属于当前企业。");
        }

        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (!trimmedName.equals(entity.getCredentialName())) {
                Long duplicate = credentialMapper.selectCount(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                        .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                        .eq(PlatformCredentialRefEntity::getCredentialName, trimmedName)
                        .ne(PlatformCredentialRefEntity::getId, id));
                if (duplicate > 0) {
                    throw new IllegalArgumentException("当前企业已存在同名凭证，请更换名称。");
                }
                entity.setCredentialName(trimmedName);
            }
        }

        if (secret != null && !secret.isBlank()) {
            entity.setCiphertext(secretCipher.encrypt(secret.trim()));
            entity.setRotatedAt(LocalDateTime.now());
        }

        credentialMapper.updateById(entity);

        List<SysModelConfigEntity> boundModels = sysModelConfigMapper.selectList(new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getTenantId, tenantId)
                .eq(SysModelConfigEntity::getCredentialRefId, id));
        List<String> boundNames = boundModels.stream().map(SysModelConfigEntity::getModelName).toList();

        return new CredentialReference(
                entity.getId(),
                entity.getCredentialName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRotatedAt(),
                boundNames,
                boundNames.size()
        );
    }

    /**
     * 安全删除模型凭证，删除前检查是否仍被现有模型绑定使用。
     *
     * @param tenantId 租户标识
     * @param id 凭证标识
     */
    public void delete(Long tenantId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("请指定要删除的模型凭证。");
        }
        PlatformCredentialRefEntity entity = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getId, id)
                .eq(PlatformCredentialRefEntity::getCredentialType, MODEL_API_KEY));
        if (entity == null) {
            throw new IllegalArgumentException("模型凭证不存在或不属于当前企业。");
        }

        // 校验是否有模型正在引用该凭证
        List<SysModelConfigEntity> boundModels = sysModelConfigMapper.selectList(new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getTenantId, tenantId)
                .eq(SysModelConfigEntity::getCredentialRefId, id));
        if (!boundModels.isEmpty()) {
            String names = boundModels.stream().map(SysModelConfigEntity::getModelName).collect(Collectors.joining("、"));
            throw new IllegalArgumentException("该安全凭证正在被模型【" + names + "】绑定使用，请先解绑或更换模型凭证后再删除。");
        }

        credentialMapper.deleteById(id);
    }

    /**
     * 校验凭证引用属于指定租户且处于活动状态。
     *
     * @param tenantId 租户标识
     * @param credentialRefId 凭证标识
     */
    public void requireAvailable(Long tenantId, Long credentialRefId) {
        requireEntity(tenantId, credentialRefId);
    }

    /**
     * 在模型基础设施内部解析凭证明文，不允许进入 API 响应或日志。
     *
     * @param tenantId 租户标识
     * @param credentialRefId 凭证标识
     * @return 解密后的明文凭证密钥
     */
    public String resolveSecret(Long tenantId, Long credentialRefId) {
        return secretCipher.decrypt(requireEntity(tenantId, credentialRefId).getCiphertext());
    }

    /**
     * 按租户边界读取可用凭证实体。
     *
     * @param tenantId 租户标识
     * @param credentialRefId 凭证标识
     * @return 凭证持久化实体
     */
    private PlatformCredentialRefEntity requireEntity(Long tenantId, Long credentialRefId) {
        if (tenantId == null || credentialRefId == null) {
            throw new IllegalArgumentException("请选择有效的模型凭证。");
        }
        PlatformCredentialRefEntity entity = credentialMapper.selectOne(new LambdaQueryWrapper<PlatformCredentialRefEntity>()
                .eq(PlatformCredentialRefEntity::getTenantId, tenantId)
                .eq(PlatformCredentialRefEntity::getId, credentialRefId)
                .eq(PlatformCredentialRefEntity::getCredentialType, MODEL_API_KEY)
                .eq(PlatformCredentialRefEntity::getStatus, BusinessStatus.ACTIVE));
        if (entity == null) {
            throw new IllegalArgumentException("模型凭证不存在、已停用或不属于当前企业。");
        }
        return entity;
    }

    /**
     * 可安全返回给前端的凭证引用元数据。
     *
     * @param id 凭证唯一标识
     * @param name 凭证显示名称
     * @param status 凭证可用状态
     * @param createdAt 创建时间
     * @param rotatedAt 最近轮换/更新时间
     * @param boundModelNames 绑定的模型名称列表
     * @param boundModelCount 绑定的模型数量
     */
    public record CredentialReference(
            Long id,
            String name,
            String status,
            LocalDateTime createdAt,
            LocalDateTime rotatedAt,
            List<String> boundModelNames,
            int boundModelCount
    ) {
        /**
         * 提供兼容旧参数的重载构造。
         */
        public CredentialReference(Long id, String name, String status, LocalDateTime rotatedAt) {
            this(id, name, status, null, rotatedAt, Collections.emptyList(), 0);
        }
    }
}
