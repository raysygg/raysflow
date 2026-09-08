package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.DocumentVersionStatus;
import com.acme.agentstudio.domain.knowledge.KnowledgeLifecycleContracts.DocumentVersionSummary;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentVersionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * 知识库文档版本控制与内容指纹幂等重析服务。
 * 基于 SHA-256 规范化算法对文档内容生成特征指纹，在增量同步或重新解析时判断内容是否变更，避免无意义的文档分块与向量重新索引。
 */
@Service
public class KnowledgeDocumentVersionService {

    /** 哈希摘要算法常数 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** 知识库文档版本 Mapper */
    private final KnowledgeDocumentVersionMapper versionMapper;

    /**
     * 构造函数注入版本持久化依赖。
     */
    public KnowledgeDocumentVersionService(KnowledgeDocumentVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    /**
     * 判断文档内容指纹是否发生变化；若变化则创建新版本号记录，若无变化则直接返回已有的版本摘要。
     *
     * @param user 当前登录用户
     * @param sourceId 数据源 ID
     * @param documentId 文档 ID
     * @param externalId 外部文件 ID
     * @param content 文档正文内容
     * @return 文档版本摘要
     */
    @Transactional
    public DocumentVersionSummary createIfChanged(SecurityUser user, Long sourceId,
                                                 Long documentId, String externalId,
                                                 String content) {
        requireTenant(user);
        String fingerprint = fingerprint(content);
        KnowledgeDocumentVersionEntity existing = versionMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentVersionEntity>()
                .eq(KnowledgeDocumentVersionEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeDocumentVersionEntity::getSourceId, sourceId)
                .eq(KnowledgeDocumentVersionEntity::getExternalDocumentId, externalId)
                .eq(KnowledgeDocumentVersionEntity::getContentFingerprint, fingerprint));
        if (existing != null) {
            return toSummary(existing);
        }

        Integer latest = versionMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentVersionEntity>()
                        .eq(KnowledgeDocumentVersionEntity::getTenantId, user.getTenantId())
                        .eq(KnowledgeDocumentVersionEntity::getSourceId, sourceId)
                        .eq(KnowledgeDocumentVersionEntity::getExternalDocumentId, externalId)
                        .orderByDesc(KnowledgeDocumentVersionEntity::getVersionNo))
                .stream().map(KnowledgeDocumentVersionEntity::getVersionNo).findFirst().orElse(0);

        KnowledgeDocumentVersionEntity entity = new KnowledgeDocumentVersionEntity();
        entity.setTenantId(user.getTenantId());
        entity.setSourceId(sourceId);
        entity.setDocumentId(documentId);
        entity.setExternalDocumentId(externalId);
        entity.setVersionNo(latest + 1);
        entity.setContentFingerprint(fingerprint);
        entity.setNormalizedLength(normalize(content).length());
        entity.setVersionStatus(DocumentVersionStatus.DISCOVERED.name());
        entity.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(entity);
        return toSummary(entity);
    }

    /**
     * 将删除或停用来源关联的文档版本标记为 EXCLUDED（从活动检索范围排除）。
     *
     * @param user 当前登录用户
     * @param sourceId 数据源 ID
     * @param externalId 外部文件 ID
     */
    @Transactional
    public void exclude(SecurityUser user, Long sourceId, String externalId) {
        requireTenant(user);
        versionMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocumentVersionEntity>()
                .eq(KnowledgeDocumentVersionEntity::getTenantId, user.getTenantId())
                .eq(KnowledgeDocumentVersionEntity::getSourceId, sourceId)
                .eq(KnowledgeDocumentVersionEntity::getExternalDocumentId, externalId)
                .ne(KnowledgeDocumentVersionEntity::getVersionStatus, DocumentVersionStatus.DELETED.name())
                .set(KnowledgeDocumentVersionEntity::getVersionStatus, DocumentVersionStatus.EXCLUDED.name()));
    }

    /**
     * 规范化消除多余连续空白符。
     */
    private String normalize(String content) {
        return content == null ? "" : content.replaceAll("\\s+", " ").trim();
    }

    /**
     * 生成文本指纹（SHA-256 Hex）。
     */
    private String fingerprint(String content) {
        try {
            byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM)
                    .digest(normalize(content).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("文档内容指纹生成失败", exception);
        }
    }

    /**
     * 转化实体为版本摘要契约对象。
     */
    private DocumentVersionSummary toSummary(KnowledgeDocumentVersionEntity entity) {
        return new DocumentVersionSummary(entity.getDocumentId(), entity.getExternalDocumentId(),
                entity.getContentFingerprint(), entity.getVersionNo(),
                DocumentVersionStatus.valueOf(entity.getVersionStatus()));
    }

    /**
     * 校验租户上下文。
     */
    private void requireTenant(SecurityUser user) {
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new IllegalArgumentException("当前身份无效");
        }
    }
}

