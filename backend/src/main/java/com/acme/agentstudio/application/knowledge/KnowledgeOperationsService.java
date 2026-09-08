package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.KnowledgeBaseStatus;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SourceStatus;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SourceSummary;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.SourceType;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeSourceEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库与数据源运维应用服务。
 * 负责在多租户隔离约束下提供知识库（Knowledge Base）的创建维护、外部数据源（Source）管理与数据源列表查询能力。
 */
@Service
public class KnowledgeOperationsService {

    /** 知识库 Mapper */
    private final KnowledgeBaseMapper baseMapper;

    /** 数据源 Mapper */
    private final KnowledgeSourceMapper sourceMapper;

    /**
     * 构造函数注入知识库与数据源 Mapper 依赖。
     */
    public KnowledgeOperationsService(KnowledgeBaseMapper baseMapper, KnowledgeSourceMapper sourceMapper) {
        this.baseMapper = baseMapper;
        this.sourceMapper = sourceMapper;
    }

    /**
     * 在当前租户下创建新的知识库领域对象。
     *
     * @param user 当前登录用户
     * @param code 知识库编码
     * @param name 知识库名称
     * @return 创建成功的知识库实体
     */
    @Transactional
    public KnowledgeBaseEntity createBase(SecurityUser user, String code, String name) {
        requireTenant(user);
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setTenantId(user.getTenantId());
        entity.setBaseCode(code.trim());
        entity.setBaseName(name.trim());
        entity.setBaseStatus(KnowledgeBaseStatus.ACTIVE.name());
        entity.setCreatedBy(user.getUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        baseMapper.insert(entity);
        return entity;
    }

    /**
     * 为指定知识库挂载新的外部数据源（如语雀、飞书文档、Notion 或本地文件上传源）。
     *
     * @param user 当前登录用户
     * @param baseId 知识库 ID
     * @param code 数据源编码
     * @param displayName 显示名称
     * @param type 数据源类型枚举
     * @param connectorType 连接器类型
     * @param credentialReference 挂载的认证凭据引用 ID
     * @return 创建的数据源实体
     */
    @Transactional
    public KnowledgeSourceEntity createSource(SecurityUser user, Long baseId, String code,
                                               String displayName, SourceType type,
                                               String connectorType, String credentialReference) {
        requireTenant(user);
        requireBase(user.getTenantId(), baseId);
        KnowledgeSourceEntity entity = new KnowledgeSourceEntity();
        entity.setTenantId(user.getTenantId());
        entity.setKnowledgeBaseId(baseId);
        entity.setSourceCode(code.trim());
        entity.setSourceType(type.name());
        entity.setDisplayName(displayName.trim());
        entity.setConnectorType(connectorType);
        entity.setCredentialReference(credentialReference);
        entity.setSourceStatus(SourceStatus.CREATED.name());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        sourceMapper.insert(entity);
        return entity;
    }

    /**
     * 查询指定知识库下的非删除状态数据源摘要列表。
     *
     * @param user 当前登录用户
     * @param baseId 知识库 ID
     * @return 数据源摘要契约列表
     */
    public List<SourceSummary> listSources(SecurityUser user, Long baseId) {
        requireTenant(user);
        requireBase(user.getTenantId(), baseId);
        return sourceMapper.selectList(new LambdaQueryWrapper<KnowledgeSourceEntity>()
                        .eq(KnowledgeSourceEntity::getTenantId, user.getTenantId())
                        .eq(KnowledgeSourceEntity::getKnowledgeBaseId, baseId)
                        .ne(KnowledgeSourceEntity::getSourceStatus, SourceStatus.DELETED.name())
                        .orderByDesc(KnowledgeSourceEntity::getUpdatedAt))
                .stream()
                .map(item -> new SourceSummary(item.getId(), item.getTenantId(), item.getKnowledgeBaseId(),
                        SourceType.valueOf(item.getSourceType()), item.getDisplayName(),
                        SourceStatus.valueOf(item.getSourceStatus()), item.getLastSyncedAt()))
                .toList();
    }

    /**
     * 校验知识库归属。
     */
    private void requireBase(Long tenantId, Long baseId) {
        if (baseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getTenantId, tenantId)
                .eq(KnowledgeBaseEntity::getId, baseId)) == null) {
            throw new IllegalArgumentException("知识库不存在或不属于当前租户");
        }
    }

    /**
     * 校验租户身份。
     */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
    }
}

