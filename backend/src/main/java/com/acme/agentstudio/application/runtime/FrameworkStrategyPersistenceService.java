package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.FrameworkCapability;
import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.infrastructure.persistence.entity.RuntimeFrameworkStrategyEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RuntimeFrameworkStrategyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户级框架策略与能力快照持久化服务（Framework Strategy Persistence Service）。
 * 负责持久化管理租户启用的开源框架策略配置、适配器版本号及发布时冻结的能力描述快照 JSON（capabilityJson）。
 */
@Service
public class FrameworkStrategyPersistenceService {

    /** 框架策略实体 Mapper */
    private final RuntimeFrameworkStrategyMapper mapper;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入依赖服务。
     */
    public FrameworkStrategyPersistenceService(RuntimeFrameworkStrategyMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询指定租户下已配置的所有框架策略配置列表。
     *
     * @param tenantId 租户 ID
     * @return 框架策略实体列表 List&lt;RuntimeFrameworkStrategyEntity&gt;
     */
    public List<RuntimeFrameworkStrategyEntity> list(long tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<RuntimeFrameworkStrategyEntity>()
                .eq(RuntimeFrameworkStrategyEntity::getTenantId, tenantId)
                .orderByAsc(RuntimeFrameworkStrategyEntity::getFrameworkCode));
    }

    /**
     * 新增或更新租户下的某个原生框架策略配置及能力快照。
     *
     * @param tenantId 租户 ID
     * @param framework 原生框架 NativeFramework
     * @param adapterVersion 适配器版本号
     * @param enabled 是否开启该框架
     * @param capability 当前对应的框架能力描述实体 FrameworkCapability
     * @return 保存成功后的实体 RuntimeFrameworkStrategyEntity
     */
    @Transactional
    public RuntimeFrameworkStrategyEntity save(
            long tenantId,
            NativeFramework framework,
            String adapterVersion,
            boolean enabled,
            FrameworkCapability capability
    ) {
        if (tenantId <= 0 || framework == null || adapterVersion == null || adapterVersion.isBlank() || capability == null) {
            throw new IllegalArgumentException("框架策略的租户 ID、框架 NativeFramework、适配器版本与能力快照必须填写完整。");
        }

        RuntimeFrameworkStrategyEntity entity = mapper.selectOne(new LambdaQueryWrapper<RuntimeFrameworkStrategyEntity>()
                .eq(RuntimeFrameworkStrategyEntity::getTenantId, tenantId)
                .eq(RuntimeFrameworkStrategyEntity::getFrameworkCode, framework.name()));

        if (entity == null) {
            entity = new RuntimeFrameworkStrategyEntity();
            entity.setTenantId(tenantId);
            entity.setFrameworkCode(framework.name());
            entity.setCreatedAt(LocalDateTime.now());
        }

        entity.setAdapterVersion(adapterVersion);
        entity.setEnabled(enabled);
        entity.setCapabilityJson(write(capability));
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    /** 将对象转换为 JSON 字符串 */
    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("框架能力快照 JSON 序列化失败。", exception);
        }
    }
}

