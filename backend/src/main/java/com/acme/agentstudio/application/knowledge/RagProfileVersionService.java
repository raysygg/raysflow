package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.ProfileVersionStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.RagProfileVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagProfileVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * RAG Embedding 检索配置 Profile 版本与向量空间对比服务。
 * 明确区分向量空间参数变更（Embedding 模型 Key、维度、Instruction）与常规在线排序参数变更，计算 Profile 指纹以判定是否需要触发出重构索引。
 */
@Service
public class RagProfileVersionService {

    /** 哈希算法名称 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** Rag Profile 版本 Mapper */
    private final RagProfileVersionMapper versionMapper;

    /**
     * 构造函数注入版本 Persistence 组件。
     */
    public RagProfileVersionService(RagProfileVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    /**
     * 创建一个全新的 RagEmbeddingProfile 配置草稿版本。
     *
     * @param user 当前登录用户
     * @param profile Embedding 配置 Profile 实体对象
     * @param configurationJson JSON 序列化配置参数
     * @return 新建的版本实体对象
     */
    @Transactional
    public RagProfileVersionEntity create(SecurityUser user, RagEmbeddingProfile profile,
                                           String configurationJson) {
        requireTenant(user);
        String fingerprint = fingerprint(profile.embeddingModelKey(), profile.modelVersion(),
                profile.vectorDimension(), profile.queryInstruction(), profile.documentInstruction());
        Integer latest = versionMapper.selectList(new LambdaQueryWrapper<RagProfileVersionEntity>()
                        .eq(RagProfileVersionEntity::getTenantId, user.getTenantId())
                        .eq(RagProfileVersionEntity::getProfileId, profile.id())
                        .orderByDesc(RagProfileVersionEntity::getVersionNo))
                .stream().map(RagProfileVersionEntity::getVersionNo).findFirst().orElse(0);

        RagProfileVersionEntity entity = new RagProfileVersionEntity();
        entity.setTenantId(user.getTenantId());
        entity.setProfileId(profile.id());
        entity.setVersionNo(latest + 1);
        entity.setEmbeddingFingerprint(fingerprint);
        entity.setConfigurationJson(configurationJson);
        entity.setVersionStatus(ProfileVersionStatus.DRAFT.name());
        entity.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(entity);
        return entity;
    }

    /**
     * 对比传入的指纹与活动（ACTIVE）Profile 指纹，判定向量空间是否发生了破坏性变更（如更换模型或改变维度）。
     *
     * @param user 当前登录用户
     * @param profileId Profile ID
     * @param fingerprint 待对比的新指纹
     * @return 若向量空间发生改变或未查找到活动配置则返回 true，否则返回 false
     */
    public boolean vectorSpaceChanged(SecurityUser user, Long profileId, String fingerprint) {
        requireTenant(user);
        RagProfileVersionEntity active = versionMapper.selectOne(new LambdaQueryWrapper<RagProfileVersionEntity>()
                .eq(RagProfileVersionEntity::getTenantId, user.getTenantId())
                .eq(RagProfileVersionEntity::getProfileId, profileId)
                .eq(RagProfileVersionEntity::getVersionStatus, ProfileVersionStatus.ACTIVE.name())
                .orderByDesc(RagProfileVersionEntity::getVersionNo));
        return active == null || !fingerprint.equals(active.getEmbeddingFingerprint());
    }

    /**
     * 计算向量空间参数指纹（SHA-256 Hex）。
     */
    private String fingerprint(String modelKey, String modelVersion, int dimension,
                               String queryInstruction, String documentInstruction) {
        try {
            String value = String.join("|", nullToEmpty(modelKey), nullToEmpty(modelVersion),
                    String.valueOf(dimension), nullToEmpty(queryInstruction), nullToEmpty(documentInstruction));
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Profile 向量空间指纹生成失败", exception);
        }
    }

    /**
     * 空字符串安全转换。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

