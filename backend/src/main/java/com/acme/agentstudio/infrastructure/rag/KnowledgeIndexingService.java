package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.KnowledgeConstants;
import com.acme.agentstudio.domain.knowledge.model.ActiveIndexGeneration;
import com.acme.agentstudio.domain.knowledge.model.ChunkVectorStatus;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeChunkRole;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeIndexingPhase;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.LexicalEncoding;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.SemanticChunkPlan;
import com.acme.agentstudio.domain.knowledge.model.VectorIndexPoint;
import com.acme.agentstudio.domain.knowledge.port.EmbeddingProvider;
import com.acme.agentstudio.domain.knowledge.port.ModelProfileResolver;
import com.acme.agentstudio.domain.knowledge.port.SemanticChunkingService;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.acme.agentstudio.infrastructure.rag.parser.KnowledgeDocumentParser;
import com.acme.agentstudio.infrastructure.rag.retrieval.MultilingualSparseEncoder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * KnowledgeIndexing 业务服务接口。
 * 定义 KnowledgeIndexing 相关的核心业务契约与流程接口。
 */
@Service
/**
 * KnowledgeIndexing 业务逻辑服务接口。
 * 负责 KnowledgeIndexing 核心业务逻辑与流程编排。
 */
public class KnowledgeIndexingService {
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeIndexingService.class);
    private static final String MISSING_SOURCE_MESSAGE = "知识文档原始文件不存在，无法重新索引。";
    private static final String UNSUPPORTED_FORMAT_MESSAGE = "不支持的知识文档格式，请上传：";
    private static final int MAX_DATABASE_BATCH_SIZE = 200;
    private static final int MAX_EMBEDDING_BATCH_SIZE = 20;

    private final EmbeddingProvider embeddingProvider;
    private final ModelProfileResolver profileResolver;
    private final VectorIndexProvider vectorIndexProvider;
    private final KnowledgeIndexGenerationService generationService;
    private final SemanticChunkingService chunkingService;
    private final KnowledgeChunkMapper chunkMapper;
    private final RagProperties properties;
    private final Path uploadDir;
    private final List<KnowledgeDocumentParser> documentParsers;
    private final KnowledgeProgressBroadcaster broadcaster;
    private final MultilingualSparseEncoder sparseEncoder;

    public KnowledgeIndexingService(EmbeddingProvider embeddingProvider,
                                    ModelProfileResolver profileResolver,
                                    VectorIndexProvider vectorIndexProvider,
                                    KnowledgeIndexGenerationService generationService,
                                    SemanticChunkingService chunkingService,
                                    KnowledgeChunkMapper chunkMapper,
                                    RagProperties properties,
                                    List<KnowledgeDocumentParser> documentParsers,
                                    KnowledgeProgressBroadcaster broadcaster,
                                    MultilingualSparseEncoder sparseEncoder,
                                    @Value("${app.storage.upload-dir:data/uploads}") String uploadDir) {
        this.embeddingProvider = embeddingProvider;
        this.profileResolver = profileResolver;
        this.vectorIndexProvider = vectorIndexProvider;
        this.generationService = generationService;
        this.chunkingService = chunkingService;
        this.chunkMapper = chunkMapper;
        this.properties = properties;
        this.documentParsers = List.copyOf(documentParsers);
        this.broadcaster = broadcaster;
        this.sparseEncoder = sparseEncoder;
        this.uploadDir = Path.of(uploadDir);
    }

    /**
     * 只持久化原始上传文件，不在 HTTP 请求线程中解析、调用模型或写入 Qdrant。
     * 大文档上传成功后由持久化任务队列接管索引，避免网关超时和请求线程长期占用。
     */
    public StoredDocument storeMultipartFile(Long documentId, MultipartFile file) {
        try {
            Files.createDirectories(uploadDir);
            String originalName = effectiveFilename(file.getOriginalFilename());
            validateSupportedFormat(originalName);
            Path target = uploadDir.resolve(documentId + "-" + sanitizeFilename(originalName));
            file.transferTo(target);
            return new StoredDocument(target.toString(), originalName);
        } catch (IOException exception) {
            throw new IllegalStateException("知识文档保存失败。", exception);
        }
    }

    public IndexingResult reindexExistingFile(Long tenantId, Long documentId, String filePath,
                                               String originalName, KnowledgeLanguage language,
                                               RagModelSelection modelSelection) {
        validateSupportedFormat(originalName);
        Path source = Path.of(filePath);
        if (!Files.exists(source)) throw new IllegalStateException(MISSING_SOURCE_MESSAGE);
        RagEmbeddingProfile profile = profileResolver.resolve(tenantId, modelSelection);
        ActiveIndexGeneration generation = generationService.requireActive(tenantId, profile);
        return indexFile(tenantId, documentId, source, effectiveFilename(originalName), language,
                profile, generation, GenerationWriteMode.ACTIVATE_DOCUMENT);
    }

    /**
     * 将文档写入指定 BUILDING Generation。该路径只写暂存数据，不切换文档活动指针，也不清理旧索引。
     */
    public IndexingResult reindexExistingFileIntoGeneration(Long tenantId, Long documentId, String filePath,
                                                             String originalName, KnowledgeLanguage language,
                                                             RagEmbeddingProfile profile,
                                                             ActiveIndexGeneration generation) {
        validateSupportedFormat(originalName);
        Path source = Path.of(filePath);
        if (!Files.exists(source)) throw new IllegalStateException(MISSING_SOURCE_MESSAGE);
        generationService.requireBuildingTarget(tenantId, profile, generation);
        return indexFile(tenantId, documentId, source, effectiveFilename(originalName), language,
                profile, generation, GenerationWriteMode.STAGE_GENERATION);
    }

        /**
         * 删除deleteDocumentIndex 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         * @param documentId documentId 参数
         */
    public void deleteDocumentIndex(Long tenantId, Long documentId) {
        generationService.deleteDocumentIndex(tenantId, documentId);
        chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getDocumentId, documentId));
    }

    private IndexingResult indexFile(Long tenantId, Long documentId, Path filePath,
                                     String originalName, KnowledgeLanguage language,
                                     RagEmbeddingProfile profile, ActiveIndexGeneration generation,
                                     GenerationWriteMode writeMode) {
        long startedAt = System.currentTimeMillis();
        List<KnowledgeChunkEntity> oldChunks = chunks(documentId).stream()
                .filter(chunk -> generation.id().equals(chunk.getIndexGenerationId()))
                .toList();
        List<KnowledgeChunkEntity> newChunks = new ArrayList<>();
        List<String> newPointIds = new ArrayList<>();
        LOG.info("知识索引开始，tenantId={}, documentId={}, profile={}, generation={}",
                tenantId, documentId, profile.code(), generation.id());
        progress(documentId, KnowledgeIndexingPhase.PARSING, "读取文档中", 10, 0, 0, "正在解析知识文档。");

        try {
            Document document = selectParser(originalName).parse(filePath, originalName);
            SemanticChunkPlan plan = chunkingService.split(document, originalName);
            if (plan.children().isEmpty()) {
                throw new IllegalStateException("知识文档没有生成可检索子块。");
            }
            progress(documentId, KnowledgeIndexingPhase.CHUNKING, "结构化切块完成", 30, 0, plan.children().size(),
                    "已生成父级段落和可检索子块。");
            PersistedChunks persisted = persistStagingChunks(documentId, generation.id(), profile, plan, newChunks);
            int pointCount = embedAndIndex(tenantId, documentId, generation, profile, language,
                    persisted.children(), newPointIds);
            boolean documentActivated = false;
            if (writeMode == GenerationWriteMode.ACTIVATE_DOCUMENT) {
                documentActivated = generationService.activateDocument(tenantId, documentId, profile, generation);
            }
            int replacedChunkCount = activeChildCount(oldChunks, generation.id());
            recordSuccess(generation.id(), plan.children().size() - replacedChunkCount,
                    writeMode == GenerationWriteMode.STAGE_GENERATION || documentActivated);
            cleanupOldData(generation.collectionName(), oldChunks);
            long elapsedMs = System.currentTimeMillis() - startedAt;
            progress(documentId, KnowledgeIndexingPhase.COMPLETED, "索引完成", 100, pointCount, pointCount,
                    "知识文档已完成上下文增强索引。");
            LOG.info("知识索引完成，tenantId={}, documentId={}, parents={}, children={}, elapsedMs={}",
                    tenantId, documentId, plan.parents().size(), plan.children().size(), elapsedMs);
            return new IndexingResult(filePath.toString(), plan.children().size(), generation.id(), profile.code());
        } catch (Exception exception) {
            rollbackStaging(generation.collectionName(), newPointIds, newChunks);
            if (writeMode == GenerationWriteMode.ACTIVATE_DOCUMENT) {
                generationService.markDocumentFailed(tenantId, documentId, profile, safeError(exception));
            }
            recordFailure(generation.id(), safeError(exception));
            progress(documentId, KnowledgeIndexingPhase.FAILED, "索引失败", 0, 0, 0,
                    "知识文档索引失败：" + safeError(exception));
            LOG.error("知识索引失败，tenantId={}, documentId={}, profile={}",
                    tenantId, documentId, profile.code(), exception);
            throw new IllegalStateException("知识文档索引失败，旧索引已保留。", exception);
        }
    }

    private PersistedChunks persistStagingChunks(Long documentId, Long generationId, RagEmbeddingProfile profile,
                                                 SemanticChunkPlan plan, List<KnowledgeChunkEntity> allNewChunks) {
        Map<Integer, Long> parentIds = new LinkedHashMap<>();
        List<KnowledgeChunkEntity> parents = new ArrayList<>(plan.parents().size());
        for (SemanticChunkPlan.ParentChunk parent : plan.parents()) {
            KnowledgeChunkEntity entity = entity(documentId, generationId, parent.ordinal(), parent.text(),
                    null, KnowledgeChunkRole.PARENT, parent.sectionPath(), parent.pageNo(), parent.tokenCount(),
                    parent.contentHash(), ChunkVectorStatus.DISABLED, ChunkVectorStatus.DISABLED);
            entity.setVectorKey("parent-" + UUID.randomUUID());
            parentIds.put(parent.ordinal(), entity.getId());
            parents.add(entity);
        }
        insertChunkBatches(parents);
        allNewChunks.addAll(parents);

        List<IndexedChild> children = new ArrayList<>();
        List<KnowledgeChunkEntity> childEntities = new ArrayList<>(plan.children().size());
        for (SemanticChunkPlan.ChildChunk child : plan.children()) {
            Long parentId = parentIds.get(child.parentOrdinal());
            String pointId = UUID.randomUUID().toString();
            KnowledgeChunkEntity entity = entity(documentId, generationId, child.ordinal(), child.text(), parentId,
                    KnowledgeChunkRole.CHILD, child.sectionPath(), child.pageNo(), child.tokenCount(), child.contentHash(),
                    ChunkVectorStatus.PENDING,
                    profile.lateInteractionEnabled() ? ChunkVectorStatus.PENDING : ChunkVectorStatus.DISABLED);
            entity.setVectorKey(pointId);
            childEntities.add(entity);
            children.add(new IndexedChild(entity, child.embeddingText()));
        }
        insertChunkBatches(childEntities);
        allNewChunks.addAll(childEntities);
        return new PersistedChunks(List.copyOf(children));
    }

    private KnowledgeChunkEntity entity(Long documentId, Long generationId, int chunkNo, String text,
                                        Long parentId, KnowledgeChunkRole role, String sectionPath, Integer pageNo,
                                        int tokenCount, String contentHash, ChunkVectorStatus embeddingStatus,
                                        ChunkVectorStatus lateStatus) {
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        // 应用侧提前生成主键，父子关系可在批量 INSERT 前确定，避免为每个切块往返 MySQL 获取自增 ID。
        entity.setId(IdWorker.getId());
        entity.setDocumentId(documentId);
        entity.setIndexGenerationId(generationId);
        entity.setParentChunkId(parentId);
        entity.setChunkRole(role.name());
        entity.setChunkNo(chunkNo);
        entity.setChunkText(text);
        entity.setSectionPath(sectionPath);
        entity.setPageNo(pageNo);
        entity.setTokenCount(tokenCount);
        entity.setContentHash(contentHash);
        entity.setEmbeddingStatus(embeddingStatus.name());
        entity.setLateVectorStatus(lateStatus.name());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * Embedding、Qdrant 写入和 MySQL 状态更新按同一有界批次流式执行。
     * 这样大文档不会同时在 JVM 中保留全部浮点向量和 Qdrant 请求体，且每个完成批次都能明确校验数量与维度。
     */
    private int embedAndIndex(Long tenantId, Long documentId, ActiveIndexGeneration generation,
                              RagEmbeddingProfile profile, KnowledgeLanguage language,
                              List<IndexedChild> children, List<String> pointIds) {
        int processed = 0;
        int batchSize = Math.max(1, Math.min(MAX_EMBEDDING_BATCH_SIZE, properties.getEmbeddingBatchSize()));
        for (int start = 0; start < children.size(); start += batchSize) {
            List<IndexedChild> batch = children.subList(start, Math.min(start + batchSize, children.size()));
            int startPercent = 30 + (int) ((double) processed / Math.max(1, children.size()) * 55);
            progress(documentId, KnowledgeIndexingPhase.EMBEDDING, "生成语义向量", startPercent,
                    processed, children.size(), "正在分批生成多语言稠密向量。");
            List<List<Float>> vectors = embeddingProvider.embedDocuments(profile,
                    batch.stream().map(IndexedChild::embeddingText).toList());
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("Embedding 模型返回数量与知识切块数量不一致。");
            }
            validateDimensions(profile, vectors);
            List<VectorIndexPoint> points = vectorPoints(tenantId, documentId, generation.id(), language,
                    batch, vectors, pointIds);
            vectorIndexProvider.upsert(generation.collectionName(), points);
            markReady(batch);
            processed += batch.size();
            int percent = 30 + (int) ((double) processed / Math.max(1, children.size()) * 55);
            progress(documentId, KnowledgeIndexingPhase.PERSISTING, "写入向量索引", percent,
                    processed, children.size(), "Embedding 与 Qdrant 写入批次已完成。");
        }
        return processed;
    }

    private void validateDimensions(RagEmbeddingProfile profile, List<List<Float>> vectors) {
        for (List<Float> vector : vectors) {
            if (vector.size() != profile.vectorDimension()) {
                throw new IllegalStateException("Embedding 模型向量维度与 Profile 不一致，期望 "
                        + profile.vectorDimension() + "，实际 " + vector.size() + "。");
            }
        }
    }

    private List<VectorIndexPoint> vectorPoints(Long tenantId, Long documentId, Long generationId,
                                                KnowledgeLanguage language, List<IndexedChild> children,
                                                List<List<Float>> vectors, List<String> pointIds) {
        List<VectorIndexPoint> points = new ArrayList<>();
        for (int index = 0; index < children.size(); index++) {
            KnowledgeChunkEntity chunk = children.get(index).entity();
            LexicalEncoding lexical = sparseEncoder.encodeDocument(chunk.getChunkText());
            pointIds.add(chunk.getVectorKey());
            points.add(new VectorIndexPoint(chunk.getVectorKey(), tenantId, documentId, chunk.getId(), generationId,
                    chunk.getParentChunkId(), language == null ? KnowledgeLanguage.OTHER : language,
                    chunk.getChunkNo(), chunk.getSectionPath(), vectors.get(index), lexical.sparseVector(),
                    lexical.exactKeys(), List.of()));
        }
        return List.copyOf(points);
    }

    private void markReady(List<IndexedChild> children) {
        List<Long> chunkIds = children.stream().map(child -> child.entity().getId()).toList();
        chunkMapper.updateVectorStatus(chunkIds, ChunkVectorStatus.READY.name(), ChunkVectorStatus.DISABLED.name());
    }

    private void cleanupOldData(String collectionName, List<KnowledgeChunkEntity> oldChunks) {
        if (oldChunks.isEmpty()) return;
        try {
            List<String> oldPointIds = oldChunks.stream()
                    .filter(chunk -> KnowledgeChunkRole.CHILD.name().equals(chunk.getChunkRole()))
                    .map(KnowledgeChunkEntity::getVectorKey)
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
            vectorIndexProvider.deletePoints(collectionName, oldPointIds);
            deleteChunkBatches(oldChunks.stream().map(KnowledgeChunkEntity::getId).toList());
        } catch (RuntimeException exception) {
            LOG.warn("新索引已激活，但旧索引数据清理失败，collection={}, oldChunks={}",
                    collectionName, oldChunks.size());
        }
    }

    private void rollbackStaging(String collectionName, List<String> pointIds, List<KnowledgeChunkEntity> chunks) {
        try {
            vectorIndexProvider.deletePoints(collectionName, pointIds);
        } catch (RuntimeException cleanupException) {
            LOG.warn("清理失败索引的 Qdrant 临时点失败，collection={}, count={}", collectionName, pointIds.size());
        }
        if (!chunks.isEmpty()) {
            deleteChunkBatches(chunks.stream().map(KnowledgeChunkEntity::getId).toList());
        }
    }

    private void recordSuccess(Long generationId, int chunkDelta, boolean documentActivated) {
        try {
            generationService.recordSuccess(generationId, chunkDelta, documentActivated);
        } catch (RuntimeException exception) {
            LOG.warn("索引已激活，但索引版本成功指标记录失败，generationId={}", generationId);
        }
    }

    private void recordFailure(Long generationId, String errorMessage) {
        try {
            generationService.recordFailure(generationId, errorMessage);
        } catch (RuntimeException exception) {
            LOG.warn("索引失败指标记录失败，generationId={}", generationId);
        }
    }

    private List<KnowledgeChunkEntity> chunks(Long documentId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .select(KnowledgeChunkEntity::getId, KnowledgeChunkEntity::getDocumentId,
                        KnowledgeChunkEntity::getIndexGenerationId, KnowledgeChunkEntity::getChunkRole,
                        KnowledgeChunkEntity::getVectorKey)
                .eq(KnowledgeChunkEntity::getDocumentId, documentId));
    }

    private int activeChildCount(List<KnowledgeChunkEntity> chunks, Long generationId) {
        return (int) chunks.stream()
                .filter(chunk -> generationId.equals(chunk.getIndexGenerationId()))
                .filter(chunk -> KnowledgeChunkRole.CHILD.name().equals(chunk.getChunkRole()))
                .count();
    }

    private void insertChunkBatches(List<KnowledgeChunkEntity> chunks) {
        int batchSize = Math.max(1, Math.min(MAX_DATABASE_BATCH_SIZE, properties.getMysqlChunkBatchSize()));
        for (int start = 0; start < chunks.size(); start += batchSize) {
            chunkMapper.insertBatch(chunks.subList(start, Math.min(start + batchSize, chunks.size())));
        }
    }

    private void deleteChunkBatches(List<Long> chunkIds) {
        for (int start = 0; start < chunkIds.size(); start += MAX_DATABASE_BATCH_SIZE) {
            chunkMapper.deleteByIds(chunkIds.subList(start, Math.min(start + MAX_DATABASE_BATCH_SIZE, chunkIds.size())));
        }
    }

        /**
         * 验证validateSupportedFormat 业务逻辑处理。
         *
         * @param filename filename 参数
         */
    public void validateSupportedFormat(String filename) {
        selectParser(filename);
    }

    private KnowledgeDocumentParser selectParser(String filename) {
        String extension = extensionOf(filename);
        return documentParsers.stream().filter(parser -> parser.supports(extension)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        UNSUPPORTED_FORMAT_MESSAGE + KnowledgeConstants.SUPPORTED_FORMAT_LABEL + "。"));
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? "" : filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String effectiveFilename(String filename) {
        return filename == null || filename.isBlank() ? KnowledgeConstants.DEFAULT_UPLOAD_FILENAME : filename;
    }

    private String sanitizeFilename(String filename) {
        String baseName = filename.replace('\\', '/');
        int separatorIndex = baseName.lastIndexOf('/');
        if (separatorIndex >= 0) baseName = baseName.substring(separatorIndex + 1);
        baseName = baseName.replaceAll("[\\/:*?\"<>|]", "_");
        return baseName.isBlank() ? KnowledgeConstants.DEFAULT_UPLOAD_FILENAME : baseName;
    }

    private void progress(Long documentId, KnowledgeIndexingPhase phase, String label, int percent,
                          int processed, int total, String message) {
        broadcaster.broadcast(com.acme.agentstudio.domain.knowledge.model.KnowledgeIndexingProgressEvent.of(
                documentId, phase.name(), label, percent, processed, total, message));
    }

    private String safeError(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "未知索引异常" : exception.getMessage();
    }

    private record PersistedChunks(List<IndexedChild> children) {
    }

    private record IndexedChild(KnowledgeChunkEntity entity, String embeddingText) {
    }

    private enum GenerationWriteMode {
        ACTIVATE_DOCUMENT,
        STAGE_GENERATION
    }

    public record IndexingResult(String filePath, int chunkCount, Long generationId, String profileCode) {
    }

    public record StoredDocument(String filePath, String originalName) {
    }
}
