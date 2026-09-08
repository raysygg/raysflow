package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.model.ActiveIndexGeneration;
import com.acme.agentstudio.domain.knowledge.model.IndexGenerationStatus;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.VectorCollectionInspection;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentIndexStateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeIndexGenerationEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentIndexStateMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeIndexGenerationMapper;
import com.acme.agentstudio.infrastructure.rag.retrieval.MultilingualSparseEncoder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * KnowledgeIndexGeneration 业务服务接口。
 * 定义 KnowledgeIndexGeneration 相关的核心业务契约与流程接口。
 */
/**
 * 管理租户 RAG 模型空间和文档活动索引指针。
 *
 * <p>Qdrant 写入完成之前不更新文档指针；失败时只记录新一轮错误，已有活动版本继续服务。
 * 该顺序是跨 MySQL 与 Qdrant 的一致性边界，不能改成长事务，也不能提前删除旧向量。</p>
 */
@Service
/**
 * KnowledgeIndexGeneration 业务逻辑服务接口。
 * 负责 KnowledgeIndexGeneration 核心业务逻辑与流程编排。
 */
public class KnowledgeIndexGenerationService {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeIndexGenerationService.class);
    private static final int MINIMUM_RETENTION_DAYS = 1;
    private static final String FINGERPRINT_ALGORITHM = "SHA-256";
    private static final int COLLECTION_FINGERPRINT_LENGTH = 16;
    private static final int MAX_COLLECTION_NAME_LENGTH = 64;
    private final KnowledgeIndexGenerationMapper generationMapper;
    private final KnowledgeDocumentIndexStateMapper stateMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final VectorIndexProvider vectorIndexProvider;
    private final RagProperties properties;

    public KnowledgeIndexGenerationService(KnowledgeIndexGenerationMapper generationMapper,
                                           KnowledgeDocumentIndexStateMapper stateMapper,
                                           KnowledgeChunkMapper chunkMapper,
                                           VectorIndexProvider vectorIndexProvider,
                                           RagProperties properties) {
        this.generationMapper = generationMapper;
        this.stateMapper = stateMapper;
        this.chunkMapper = chunkMapper;
        this.vectorIndexProvider = vectorIndexProvider;
        this.properties = properties;
    }

        /**
         * requireActive 方法。
         *
         * @param tenantId tenantId 参数
         * @param profile profile 参数
         * @return ActiveIndexGeneration 返回对象
         */
    public ActiveIndexGeneration requireActive(Long tenantId, RagEmbeddingProfile profile) {
        KnowledgeIndexGenerationEntity entity = activeGeneration(tenantId, profile.id(), fingerprint(profile));
        if (entity == null) {
            // 租户已有向量空间时，Embedding 切换必须走整批 BUILDING 重建，禁止逐文档创建新 ACTIVE。
            if (activeGenerationForTenant(tenantId) != null) {
                throw new IllegalStateException("Embedding 配置已变化，请提交租户级批量重建后再切换索引。");
            }
            entity = createInitial(tenantId, profile);
        }
        vectorIndexProvider.ensureCollection(entity.getCollectionName(), profile);
        return toDomain(entity);
    }

    /**
     * 检索只能读取已完成写入的模型空间，不能因为一次查询自动创建空 Qdrant 集合。
     * 用户切换 Embedding 模型后必须先重建索引，否则明确提示模型空间尚未准备完成。
     */
    public ActiveIndexGeneration requireSearchable(Long tenantId, RagEmbeddingProfile profile) {
        KnowledgeIndexGenerationEntity entity = activeGeneration(tenantId, profile.id(), fingerprint(profile));
        if (entity == null || entity.getIndexedChunks() == null || entity.getIndexedChunks() <= 0) {
            throw new IllegalStateException("当前选择的 Embedding 模型尚未建立可检索索引，请先使用该模型重建知识文档。");
        }
        return toDomain(entity);
    }

    public boolean activateDocument(Long tenantId, Long documentId, RagEmbeddingProfile profile,
                                    ActiveIndexGeneration generation) {
        KnowledgeDocumentIndexStateEntity state = stateMapper.selectById(documentId);
        if (state == null) {
            state = new KnowledgeDocumentIndexStateEntity();
            state.setDocumentId(documentId);
            state.setTenantId(tenantId);
            state.setProfileId(profile.id());
            state.setActiveGenerationId(generation.id());
            state.setIndexStatus(BusinessStatus.INDEXED);
            state.setIndexedAt(LocalDateTime.now());
            state.setUpdatedAt(LocalDateTime.now());
            stateMapper.insert(state);
            return true;
        }
        boolean generationChanged = !generation.id().equals(state.getActiveGenerationId());
        state.setProfileId(profile.id());
        state.setActiveGenerationId(generation.id());
        state.setIndexStatus(BusinessStatus.INDEXED);
        state.setLastError(null);
        state.setIndexedAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        stateMapper.updateById(state);
        return generationChanged;
    }

        /**
         * markDocumentFailed 方法。
         *
         * @param tenantId tenantId 参数
         * @param documentId documentId 参数
         * @param profile profile 参数
         * @param error error 参数
         */
    public void markDocumentFailed(Long tenantId, Long documentId, RagEmbeddingProfile profile, String error) {
        KnowledgeDocumentIndexStateEntity state = stateMapper.selectById(documentId);
        if (state == null) {
            state = new KnowledgeDocumentIndexStateEntity();
            state.setDocumentId(documentId);
            state.setTenantId(tenantId);
            state.setProfileId(profile.id());
            state.setIndexStatus(BusinessStatus.INDEX_FAILED);
            state.setLastError(error);
            state.setUpdatedAt(LocalDateTime.now());
            stateMapper.insert(state);
            return;
        }
        state.setIndexStatus(state.getActiveGenerationId() == null
                ? BusinessStatus.INDEX_FAILED : BusinessStatus.INDEXED);
        state.setLastError(error);
        state.setUpdatedAt(LocalDateTime.now());
        stateMapper.updateById(state);
    }

        /**
         * recordSuccess 方法。
         *
         * @param generationId generationId 参数
         * @param chunkDelta chunkDelta 参数
         * @param documentActivated documentActivated 参数
         */
    public void recordSuccess(Long generationId, int chunkDelta, boolean documentActivated) {
        generationMapper.recordSuccess(generationId, chunkDelta, documentActivated ? 1 : 0);
    }

        /**
         * recordFailure 方法。
         *
         * @param generationId generationId 参数
         * @param errorMessage errorMessage 参数
         */
    public void recordFailure(Long generationId, String errorMessage) {
        generationMapper.recordFailure(generationId, errorMessage == null ? "索引失败" : errorMessage);
    }

    /** 创建后台重建版本。BUILDING 版本不会被查询使用，旧 ACTIVE 版本继续提供线上服务。 */
    public ActiveIndexGeneration beginRebuild(Long tenantId, RagEmbeddingProfile profile) {
        KnowledgeIndexGenerationEntity entity = new KnowledgeIndexGenerationEntity();
        entity.setTenantId(tenantId);
        entity.setProfileId(profile.id());
        entity.setEmbeddingFingerprint(fingerprint(profile));
        entity.setCollectionName(rebuildCollectionName(tenantId, profile));
        entity.setGenerationStatus(IndexGenerationStatus.BUILDING.name());
        entity.setTotalDocuments(0);
        entity.setTotalChunks(0);
        entity.setIndexedChunks(0);
        entity.setFailureCount(0);
        entity.setStartedAt(LocalDateTime.now());
        generationMapper.insert(entity);
        try {
            vectorIndexProvider.ensureCollection(entity.getCollectionName(), profile);
        } catch (RuntimeException exception) {
            markGenerationFailed(entity.getId(), exception.getMessage() == null
                    ? "创建向量集合失败。" : exception.getMessage());
            throw exception;
        }
        return toDomain(entity);
    }

    public void requireBuildingTarget(Long tenantId, RagEmbeddingProfile profile,
                                      ActiveIndexGeneration generation) {
        if (generation == null || !tenantId.equals(generation.tenantId())
                || !profile.id().equals(generation.profileId())) {
            throw new IllegalArgumentException("索引重建目标与租户或 Embedding Profile 不匹配。");
        }
        Long count = generationMapper.selectCount(new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getId, generation.id())
                .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                .eq(KnowledgeIndexGenerationEntity::getProfileId, profile.id())
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.BUILDING.name()));
        if (count == null || count != 1L) {
            throw new IllegalStateException("索引重建目标不存在或已不处于 BUILDING 状态。");
        }
    }

    /** 在批量文档全部完成且 Qdrant 维度一致后，原子切换租户的活动 Generation。 */
    @Transactional(noRollbackFor = GenerationValidationException.class)
    public void validateAndActivate(Long tenantId, RagEmbeddingProfile profile, Long generationId,
                                    int expectedDocuments, int expectedChunks) {
        validateAndActivate(tenantId, profile, generationId, expectedDocuments, expectedChunks, List.of());
    }

    @Transactional(noRollbackFor = GenerationValidationException.class)
    public void validateAndActivate(Long tenantId, RagEmbeddingProfile profile, Long generationId,
                                    int expectedDocuments, int expectedChunks, List<Long> documentIds) {
        KnowledgeIndexGenerationEntity building = generationMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getId, generationId)
                .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                .eq(KnowledgeIndexGenerationEntity::getProfileId, profile.id())
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.BUILDING.name()));
        if (building == null) throw new IllegalStateException("待激活的索引 Generation 不存在或状态不正确。");
        VectorCollectionInspection inspection = vectorIndexProvider.inspectCollection(building.getCollectionName(), profile);
        boolean valid = inspection.available()
                && inspection.dimension() == profile.vectorDimension()
                && inspection.vectorCount() == expectedChunks
                && inspection.sparseAvailable()
                && inspection.exactIndexAvailable()
                && safeInt(building.getTotalDocuments()) == expectedDocuments
                && safeInt(building.getTotalChunks()) == expectedChunks
                && safeInt(building.getIndexedChunks()) == expectedChunks;
        if (!valid) {
            markGenerationFailed(generationId, "索引校验失败：文档数、chunk 数或向量维度不匹配。");
            throw new GenerationValidationException("新索引校验失败，线上继续使用旧 Generation。");
        }
        generationMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.ACTIVE.name())
                .ne(KnowledgeIndexGenerationEntity::getId, generationId)
                .set(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.RETIRED.name()));
        building.setGenerationStatus(IndexGenerationStatus.ACTIVE.name());
        building.setTotalDocuments(expectedDocuments);
        building.setTotalChunks(expectedChunks);
        building.setIndexedChunks(expectedChunks);
        building.setCompletedAt(LocalDateTime.now());
        building.setActivatedAt(LocalDateTime.now());
        generationMapper.updateById(building);
        if (documentIds != null && !documentIds.isEmpty()) {
            stateMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocumentIndexStateEntity>()
                    .eq(KnowledgeDocumentIndexStateEntity::getTenantId, tenantId)
                    .in(KnowledgeDocumentIndexStateEntity::getDocumentId, documentIds)
                    .set(KnowledgeDocumentIndexStateEntity::getProfileId, profile.id())
                    .set(KnowledgeDocumentIndexStateEntity::getActiveGenerationId, generationId)
                    .set(KnowledgeDocumentIndexStateEntity::getIndexStatus, BusinessStatus.INDEXED)
                    .set(KnowledgeDocumentIndexStateEntity::getLastError, null)
                    .set(KnowledgeDocumentIndexStateEntity::getIndexedAt, LocalDateTime.now())
                    .set(KnowledgeDocumentIndexStateEntity::getUpdatedAt, LocalDateTime.now()));
        }
    }

        /**
         * markGenerationFailed 方法。
         *
         * @param generationId generationId 参数
         * @param errorMessage errorMessage 参数
         */
    public void markGenerationFailed(Long generationId, String errorMessage) {
        generationMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getId, generationId)
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.BUILDING.name())
                .set(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.FAILED.name())
                .set(KnowledgeIndexGenerationEntity::getErrorMessage, errorMessage)
                .set(KnowledgeIndexGenerationEntity::getCompletedAt, LocalDateTime.now()));
    }

    /** 只清理超过保留期的失败或退役版本，活动版本和构建中版本不进入候选集。 */
    @Scheduled(fixedDelayString = "${app.rag.generation-cleanup-delay-ms:3600000}")
    public void cleanupExpiredGenerations() {
        int retentionDays = Math.max(MINIMUM_RETENTION_DAYS, properties.getGenerationRetentionDays());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<String> cleanupStatuses = List.of(IndexGenerationStatus.FAILED.name(),
                IndexGenerationStatus.RETIRED.name());
        List<KnowledgeIndexGenerationEntity> expired = generationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                        .in(KnowledgeIndexGenerationEntity::getGenerationStatus, cleanupStatuses)
                        .lt(KnowledgeIndexGenerationEntity::getCompletedAt, cutoff));
        for (KnowledgeIndexGenerationEntity generation : expired) {
            try {
                vectorIndexProvider.deleteCollection(generation.getCollectionName());
                chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                        .eq(KnowledgeChunkEntity::getIndexGenerationId, generation.getId()));
                generationMapper.delete(new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                        .eq(KnowledgeIndexGenerationEntity::getId, generation.getId())
                        .in(KnowledgeIndexGenerationEntity::getGenerationStatus, cleanupStatuses));
            } catch (RuntimeException exception) {
                LOG.warn("历史索引 Generation 清理失败，稍后重试，generationId={}", generation.getId(), exception);
            }
        }
    }

    private int safeInt(Integer value) { return value == null ? 0 : value; }

        /**
         * 删除deleteDocumentIndex 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         * @param documentId documentId 参数
         */
    public void deleteDocumentIndex(Long tenantId, Long documentId) {
        KnowledgeDocumentIndexStateEntity state = stateMapper.selectById(documentId);
        if (state == null) return;
        if (!tenantId.equals(state.getTenantId())) {
            throw new IllegalArgumentException("知识文档索引不属于当前租户。");
        }
        if (state.getActiveGenerationId() != null) {
            KnowledgeIndexGenerationEntity generation = generationMapper.selectById(state.getActiveGenerationId());
            if (generation != null) {
                vectorIndexProvider.deleteDocument(generation.getCollectionName(), tenantId, documentId);
            }
        }
        stateMapper.deleteById(documentId);
    }

    private KnowledgeIndexGenerationEntity createInitial(Long tenantId, RagEmbeddingProfile profile) {
        KnowledgeIndexGenerationEntity entity = new KnowledgeIndexGenerationEntity();
        entity.setTenantId(tenantId);
        entity.setProfileId(profile.id());
        entity.setEmbeddingFingerprint(fingerprint(profile));
        entity.setCollectionName(collectionName(tenantId, profile));
        entity.setGenerationStatus(IndexGenerationStatus.ACTIVE.name());
        entity.setTotalDocuments(0);
        entity.setTotalChunks(0);
        entity.setIndexedChunks(0);
        entity.setFailureCount(0);
        entity.setStartedAt(LocalDateTime.now());
        entity.setCompletedAt(LocalDateTime.now());
        entity.setActivatedAt(LocalDateTime.now());
        generationMapper.insert(entity);
        return entity;
    }

    private KnowledgeIndexGenerationEntity activeGenerationForTenant(Long tenantId) {
        return generationMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.ACTIVE.name())
                .orderByDesc(KnowledgeIndexGenerationEntity::getId)
                .last("LIMIT 1"));
    }

    private KnowledgeIndexGenerationEntity activeGeneration(Long tenantId, Long profileId, String fingerprint) {
        return generationMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                .eq(KnowledgeIndexGenerationEntity::getProfileId, profileId)
                .eq(KnowledgeIndexGenerationEntity::getEmbeddingFingerprint, fingerprint)
                .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.ACTIVE.name())
                .orderByDesc(KnowledgeIndexGenerationEntity::getId).last("LIMIT 1"));
    }

    private String collectionName(Long tenantId, RagEmbeddingProfile profile) {
        String fullFingerprint = fingerprint(profile);
        String name = properties.getQdrant().getCollectionPrefix() + "_t" + tenantId
                + "_p" + profile.id() + "_v" + profile.versionNo() + "_f"
                + fullFingerprint.substring(0, COLLECTION_FINGERPRINT_LENGTH);
        return validateCollectionName(name);
    }

    private String rebuildCollectionName(Long tenantId, RagEmbeddingProfile profile) {
        return validateCollectionName(collectionName(tenantId, profile)
                + "_r" + Long.toString(System.currentTimeMillis(), Character.MAX_RADIX));
    }

    private String fingerprint(RagEmbeddingProfile profile) {
        String contract = String.join("|", nullToEmpty(profile.embeddingModelKey()),
                nullToEmpty(profile.modelVersion()), String.valueOf(profile.vectorDimension()),
                nullToEmpty(profile.distanceMetric()), nullToEmpty(profile.queryInstruction()),
                nullToEmpty(profile.documentInstruction()), MultilingualSparseEncoder.INDEX_CONTRACT_VERSION);
        try {
            byte[] digest = MessageDigest.getInstance(FINGERPRINT_ALGORITHM)
                    .digest(contract.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持索引指纹算法。", exception);
        }
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }

    private String validateCollectionName(String collectionName) {
        if (collectionName.length() > MAX_COLLECTION_NAME_LENGTH) {
            throw new IllegalStateException("Qdrant collection 名称超过 " + MAX_COLLECTION_NAME_LENGTH
                    + " 个字符，请缩短 QDRANT_COLLECTION_PREFIX。");
        }
        return collectionName;
    }

    private ActiveIndexGeneration toDomain(KnowledgeIndexGenerationEntity entity) {
        return new ActiveIndexGeneration(entity.getId(), entity.getTenantId(), entity.getProfileId(),
                entity.getCollectionName());
    }

    private static final class GenerationValidationException extends IllegalStateException {
        private GenerationValidationException(String message) {
            super(message);
        }
    }
}
