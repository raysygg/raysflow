package com.acme.agentstudio.application.knowledge;

import com.acme.agentstudio.domain.task.model.AsyncTaskSummary;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.KnowledgeConstants;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentSummary;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeBatchReindexSubmission;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentIndexDetail;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentPage;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentPageItem;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentPageQuery;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeDocumentOption;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeIndexGenerationSummary;
import com.acme.agentstudio.domain.knowledge.model.IndexGenerationStatus;
import com.acme.agentstudio.domain.knowledge.model.ActiveIndexGeneration;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalOutcome;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingModelOption;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.port.EmbeddingProvider;
import com.acme.agentstudio.domain.knowledge.port.ModelProfileResolver;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentIndexStateEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeIndexGenerationEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.RagEmbeddingProfileEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentIndexStateMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeIndexGenerationMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.RagEmbeddingProfileMapper;
import com.acme.agentstudio.infrastructure.rag.KnowledgeIndexingService;
import com.acme.agentstudio.infrastructure.rag.KnowledgeIndexGenerationService;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalService;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalMetricService;
import com.acme.agentstudio.application.task.PersistentTaskQueueService;
import com.acme.agentstudio.application.task.TaskType;
import com.acme.agentstudio.application.saas.TenantEntitlementService;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeTagEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentTagEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeTagMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentTagMapper;
import com.acme.agentstudio.interfaces.rest.dto.CreateKnowledgeDocumentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 知识库与 RAG 混合检索核心应用服务（Knowledge Application Service）。
 * 负责提供知识库文档全生命周期管理（上传、解析、向量切块索引）、全量/批量重新构建向量索引（Reindex）、向量模型 Profile 策略调度以及跨语言混合 RAG 检索。
 */
@Service
public class KnowledgeApplicationService {

    private static final Set<String> TENANT_ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final int REINDEX_TASK_BATCH_SIZE = 200;
    private static final int REINDEX_MAX_RETRIES = 3;
    private static final String TASK_FIELD_TENANT_ID = "tenantId";
    private static final String TASK_FIELD_DOCUMENT_ID = "documentId";
    private static final String TASK_FIELD_LANGUAGE = "language";
    private static final String TASK_FIELD_MODEL_SOURCE = "modelSource";
    private static final String TASK_FIELD_MODEL_ID = "modelId";
    private static final String TASK_FIELD_MODEL_KEY = "modelKey";

    private final EmbeddingProvider embeddingProvider;
    private final ModelProfileResolver profileResolver;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentIndexStateMapper documentIndexStateMapper;
    private final KnowledgeIndexGenerationMapper indexGenerationMapper;
    private final RagEmbeddingProfileMapper embeddingProfileMapper;
    private final KnowledgeIndexingService knowledgeIndexingService;
    private final KnowledgeIndexGenerationService indexGenerationService;
    private final RagRetrievalService ragRetrievalService;
    private final RagRetrievalMetricService ragRetrievalMetricService;
    private final KnowledgeTagMapper knowledgeTagMapper;
    private final KnowledgeDocumentTagMapper knowledgeDocumentTagMapper;
    private final TenantEntitlementService entitlementService;

    /** taskQueueService 业务逻辑服务层对象 */
    @Autowired
    @Lazy
    private PersistentTaskQueueService taskQueueService;

    public KnowledgeApplicationService(
            EmbeddingProvider embeddingProvider,
            ModelProfileResolver profileResolver,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeDocumentIndexStateMapper documentIndexStateMapper,
            KnowledgeIndexGenerationMapper indexGenerationMapper,
            RagEmbeddingProfileMapper embeddingProfileMapper,
            KnowledgeIndexingService knowledgeIndexingService,
            KnowledgeIndexGenerationService indexGenerationService,
            RagRetrievalService ragRetrievalService,
            RagRetrievalMetricService ragRetrievalMetricService,
            KnowledgeTagMapper knowledgeTagMapper,
            KnowledgeDocumentTagMapper knowledgeDocumentTagMapper,
            TenantEntitlementService entitlementService
    ) {
        this.embeddingProvider = embeddingProvider;
        this.profileResolver = profileResolver;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.documentIndexStateMapper = documentIndexStateMapper;
        this.indexGenerationMapper = indexGenerationMapper;
        this.embeddingProfileMapper = embeddingProfileMapper;
        this.knowledgeIndexingService = knowledgeIndexingService;
        this.indexGenerationService = indexGenerationService;
        this.ragRetrievalService = ragRetrievalService;
        this.ragRetrievalMetricService = ragRetrievalMetricService;
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.knowledgeDocumentTagMapper = knowledgeDocumentTagMapper;
        this.entitlementService = entitlementService;
    }

        /**
         * 查询列表listDocuments 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         * @return List<KnowledgeDocumentSummary> 返回对象
         */
    public List<KnowledgeDocumentSummary> listDocuments(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        return knowledgeDocumentMapper.selectList(tenantQuery(tenantId)
                        .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED))
                .stream().map(this::toSummary).toList();
    }

        /**
         * 查询列表listDocuments 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         * @param userId userId 参数
         * @param role role 参数
         * @return List<KnowledgeDocumentSummary> 返回对象
         */
    public List<KnowledgeDocumentSummary> listDocuments(Long tenantId, Long userId, String role) {
        // 管理员及超级管理员角色直接查看该租户下的全部文档
        if (TENANT_ADMIN_ROLES.contains(role)) {
            return listDocuments(tenantId);
        }
        // 当前版本只按菜单权限控制入口，知识文档查询限定在当前租户内。
        return listDocuments(tenantId);
    }

    /**
     * 分页读取租户知识资产，并批量补齐索引状态。
     *
     * <p>这里先限制文档页大小，再用一次 IN 查询加载该页索引状态，避免文档列表出现 N+1；
     * Qdrant 不参与资产列表查询，因为文档和索引状态的事实源是 MySQL。</p>
     */
    public KnowledgeDocumentPage pageDocuments(Long tenantId, Long userId, String role,
                                                KnowledgeDocumentPageQuery query) {
        requireTenant(tenantId);
        LambdaQueryWrapper<KnowledgeDocumentEntity> conditions = documentPageConditions(tenantId, query);
        long total = knowledgeDocumentMapper.selectCount(conditions);
        if (total == 0) return KnowledgeDocumentPage.of(List.of(), 0, query);

        List<KnowledgeDocumentEntity> documents = knowledgeDocumentMapper.selectList(
                documentPageConditions(tenantId, query)
                        .orderByDesc(KnowledgeDocumentEntity::getUpdatedAt)
                        .orderByDesc(KnowledgeDocumentEntity::getId)
                        .last("LIMIT " + query.size() + " OFFSET " + query.offset()));
        Map<Long, KnowledgeDocumentIndexStateEntity> states = documentIndexStates(documents);
        List<KnowledgeDocumentPageItem> items = documents.stream()
                .map(document -> toPageItem(document, states.get(document.getId())))
                .toList();
        return KnowledgeDocumentPage.of(items, total, query);
    }

    /** 读取单个文档的索引版本和模型摘要，前端无需拼接多个底层接口。 */
    public KnowledgeDocumentIndexDetail documentIndexDetail(Long tenantId, Long documentId) {
        KnowledgeDocumentEntity document = requireDocument(tenantId, documentId);
        KnowledgeDocumentIndexStateEntity state = documentIndexStateMapper.selectById(documentId);
        KnowledgeIndexGenerationEntity generation = state == null || state.getActiveGenerationId() == null
                ? null : indexGenerationMapper.selectById(state.getActiveGenerationId());
        RagEmbeddingProfileEntity profile = state == null || state.getProfileId() == null
                ? null : embeddingProfileMapper.selectById(state.getProfileId());
        return new KnowledgeDocumentIndexDetail(toSummary(document),
                state == null ? document.getDocumentStatus() : state.getIndexStatus(),
                state == null ? null : state.getLastError(),
                state == null ? null : state.getIndexedAt(),
                generation == null ? null : generation.getId(),
                generation == null ? null : generation.getCollectionName(),
                profile == null ? null : profile.getProfileCode(),
                profile == null ? null : profile.getProfileName(),
                profile == null ? null : profile.getEmbeddingModelKey(),
                profile == null ? null : profile.getRerankerModelKey());
    }

    /** 批量回显远程选择器已选文档，避免前端为每个 ID 单独请求详情。 */
    public List<KnowledgeDocumentOption> documentOptions(Long tenantId, List<Long> documentIds) {
        requireTenant(tenantId);
        if (documentIds == null || documentIds.isEmpty()) return List.of();
        List<Long> boundedIds = documentIds.stream().filter(java.util.Objects::nonNull).distinct()
                .limit(KnowledgeDocumentPageQuery.MAX_SIZE).toList();
        if (boundedIds.isEmpty()) return List.of();
        return knowledgeDocumentMapper.selectList(tenantQuery(tenantId)
                        .in(KnowledgeDocumentEntity::getId, boundedIds)
                        .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED))
                .stream().map(document -> new KnowledgeDocumentOption(document.getId(), document.getTitle(),
                        documentLanguage(document), Boolean.TRUE.equals(document.getLanguageConfirmed()),
                        document.getDocumentStatus()))
                .toList();
    }

    /** 当前租户只展示一个生效 Profile 及其活动索引，不返回 Qdrant 凭证和模型连接地址。 */
    public KnowledgeIndexGenerationSummary currentIndexGeneration(Long tenantId) {
        return currentIndexGeneration(tenantId, null);
    }

        /**
         * currentIndexGeneration 方法。
         *
         * @param tenantId tenantId 参数
         * @param selection selection 参数
         * @return KnowledgeIndexGenerationSummary 返回对象
         */
    public KnowledgeIndexGenerationSummary currentIndexGeneration(Long tenantId, RagModelSelection selection) {
        requireTenant(tenantId);
        RagEmbeddingProfile profile = profileResolver.resolve(tenantId, selection);
        KnowledgeIndexGenerationEntity generation = indexGenerationMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexGenerationEntity>()
                        .eq(KnowledgeIndexGenerationEntity::getTenantId, tenantId)
                        .eq(KnowledgeIndexGenerationEntity::getProfileId, profile.id())
                        .eq(KnowledgeIndexGenerationEntity::getGenerationStatus, IndexGenerationStatus.ACTIVE.name())
                        .orderByDesc(KnowledgeIndexGenerationEntity::getId)
                        .last("LIMIT 1"));
        return generationSummary(profile, generation);
    }

    /** 批量重建只提交协调任务，HTTP 请求不扫描文档、不调用模型，也不访问 Qdrant。 */
    public KnowledgeBatchReindexSubmission submitBatchReindex(Long tenantId) {
        return submitBatchReindex(tenantId, null);
    }

        /**
         * submitBatchReindex 方法。
         *
         * @param tenantId tenantId 参数
         * @param selection selection 参数
         * @return KnowledgeBatchReindexSubmission 返回对象
         */
    public KnowledgeBatchReindexSubmission submitBatchReindex(Long tenantId, RagModelSelection selection) {
        requireTenant(tenantId);
        Map<String, Object> payload = modelPayload(tenantId, selection);
        Long taskId = taskQueueService.enqueue(tenantId, TaskType.KNOWLEDGE_REINDEX_BATCH,
                payload, REINDEX_MAX_RETRIES);
        return new KnowledgeBatchReindexSubmission(taskId, BusinessStatus.PENDING,
                "批量重建任务已提交，后台将按文档游标分批加入索引队列。");
    }

    /**
     * 后台协调任务按主键游标分页拆分文档任务。
     *
     * <p>游标分页不会随着数据量增大产生 OFFSET 扫描成本；每个文档仍是独立可重试任务，
     * 单个数据源解析失败不会阻断其他文档，也不会让一个长事务覆盖模型和 Qdrant 调用。</p>
     */
    public int enqueueTenantReindexTasks(Long tenantId) {
        return enqueueTenantReindexTasks(tenantId, null);
    }

        /**
         * enqueueTenantReindexTasks 方法。
         *
         * @param tenantId tenantId 参数
         * @param selection selection 参数
         * @return int 返回对象
         */
    public int enqueueTenantReindexTasks(Long tenantId, RagModelSelection selection) {
        requireTenant(tenantId);
        long cursor = 0L;
        int submitted = 0;
        while (true) {
            List<KnowledgeDocumentEntity> batch = knowledgeDocumentMapper.selectList(
                    tenantQuery(tenantId)
                            .select(KnowledgeDocumentEntity::getId, KnowledgeDocumentEntity::getLanguage)
                            .gt(KnowledgeDocumentEntity::getId, cursor)
                            .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED)
                            .orderByAsc(KnowledgeDocumentEntity::getId)
                            .last("LIMIT " + REINDEX_TASK_BATCH_SIZE));
            if (batch.isEmpty()) return submitted;
            for (KnowledgeDocumentEntity document : batch) {
                enqueueDocumentReindex(tenantId, document, selection);
                submitted++;
            }
            cursor = batch.get(batch.size() - 1).getId();
        }
    }

    /**
     * 在后台任务中按租户整批重建索引。所有文档先写入 BUILDING Generation，校验通过后统一切换活动指针。
     */
    public int rebuildTenantIndex(Long tenantId, RagModelSelection selection) {
        requireTenant(tenantId);
        RagEmbeddingProfile profile = profileResolver.resolve(tenantId, selection);
        ActiveIndexGeneration generation = indexGenerationService.beginRebuild(tenantId, profile);
        List<StagedDocumentResult> stagedDocuments = new ArrayList<>();
        long cursor = 0L;
        int expectedChunks = 0;
        try {
            while (true) {
                List<KnowledgeDocumentEntity> batch = knowledgeDocumentMapper.selectList(
                        tenantQuery(tenantId)
                                .select(KnowledgeDocumentEntity::getId, KnowledgeDocumentEntity::getFilePath,
                                        KnowledgeDocumentEntity::getTitle, KnowledgeDocumentEntity::getLanguage,
                                        KnowledgeDocumentEntity::getVersionNo)
                                .gt(KnowledgeDocumentEntity::getId, cursor)
                                .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED)
                                .orderByAsc(KnowledgeDocumentEntity::getId)
                                .last("LIMIT " + REINDEX_TASK_BATCH_SIZE));
                if (batch.isEmpty()) break;
                for (KnowledgeDocumentEntity document : batch) {
                    if (document.getFilePath() == null || document.getFilePath().isBlank()) {
                        throw new IllegalStateException("知识文档缺少原始文件，无法完成整批索引重建。");
                    }
                    KnowledgeIndexingService.IndexingResult result =
                            knowledgeIndexingService.reindexExistingFileIntoGeneration(
                                    tenantId, document.getId(), document.getFilePath(), document.getTitle(),
                                    documentLanguage(document), profile, generation);
                    stagedDocuments.add(new StagedDocumentResult(document.getId(), result.chunkCount(),
                            document.getVersionNo() == null ? 1 : document.getVersionNo() + 1));
                    expectedChunks += result.chunkCount();
                }
                cursor = batch.get(batch.size() - 1).getId();
            }

            List<Long> documentIds = stagedDocuments.stream().map(StagedDocumentResult::documentId).toList();
            indexGenerationService.validateAndActivate(tenantId, profile, generation.id(),
                    stagedDocuments.size(), expectedChunks, documentIds);
            for (StagedDocumentResult staged : stagedDocuments) {
                knowledgeDocumentMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getTenantId, tenantId)
                        .eq(KnowledgeDocumentEntity::getId, staged.documentId())
                        .set(KnowledgeDocumentEntity::getChunkCount, staged.chunkCount())
                        .set(KnowledgeDocumentEntity::getVersionNo, staged.versionNo())
                        .set(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.INDEXED)
                        .set(KnowledgeDocumentEntity::getUpdatedAt, LocalDateTime.now()));
            }
            return stagedDocuments.size();
        } catch (RuntimeException exception) {
            indexGenerationService.markGenerationFailed(generation.id(), safeGenerationError(exception));
            throw exception;
        }
    }

    /** 校验知识文档属于当前租户，供详情、任务和实时进度入口统一复用。 */
    public void requireDocumentAccess(Long tenantId, Long documentId) {
        KnowledgeDocumentEntity document = requireDocument(tenantId, documentId);
        if (BusinessStatus.DISABLED.equals(document.getDocumentStatus())) {
            throw new IllegalArgumentException("知识文档已停用，不能访问索引进度或详情。");
        }
    }

    /**
     * 逻辑删除指定的知识库文档，并清除对应的向量切片
     *
     * @param tenantId   租户 ID
     * @param documentId 目标文档 ID
     */
    public void deleteDocument(Long tenantId, Long documentId) {
        // 校验租户参数非空
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        // 确认要删除的文档属于当前租户
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getId, documentId).eq(KnowledgeDocumentEntity::getTenantId, tenantId));
        if (document == null) {
            throw new IllegalArgumentException("知识文档不属于当前租户。");
        }
        // 删除该文档对应的全部切片记录
        knowledgeIndexingService.deleteDocumentIndex(tenantId, documentId);
        // 将文档状态置为逻辑删除 DELETED 并保存
        document.setDocumentStatus(BusinessStatus.DELETED);
        document.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 同步重新索引已有的知识库文档
     *
     * @param tenantId   租户 ID
     * @param documentId 目标文档 ID
     * @return 重新索引完成后的文档摘要
     */
    public KnowledgeDocumentSummary reindexDocument(Long tenantId, Long documentId) {
        return reindexDocument(tenantId, documentId, null);
    }

    public KnowledgeDocumentSummary reindexDocument(Long tenantId, Long documentId,
                                                    RagModelSelection selection) {
        // 校验租户参数
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        // 校验文档归属
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getId, documentId).eq(KnowledgeDocumentEntity::getTenantId, tenantId));
        if (document == null) {
            throw new IllegalArgumentException("知识文档不属于当前租户。");
        }
        // 校验原始文件路径是否存在
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            throw new IllegalArgumentException("知识文档没有可用的原始文件，无法重新索引。");
        }
        // 切换文档状态为 INDEXING，表示开始处理
        String previousStatus = document.getDocumentStatus();
        if (!BusinessStatus.INDEXED.equals(previousStatus)) {
            document.setDocumentStatus(BusinessStatus.INDEXING);
            document.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(document);
        }
        try {
            // 调用向量索引服务解析文件并重建 Embedding
            KnowledgeIndexingService.IndexingResult result = knowledgeIndexingService.reindexExistingFile(
                    tenantId, documentId, document.getFilePath(), document.getTitle(), documentLanguage(document), selection);
            document.setChunkCount(result.chunkCount());
            document.setVersionNo(document.getVersionNo() + 1);
            document.setDocumentStatus(BusinessStatus.INDEXED);
            document.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(document);
            return toSummary(document);
        } catch (RuntimeException exception) {
            // 捕获索引过程中的异常，将状态置为 INDEX_FAILED 以供运维和重试队列接管
            document.setDocumentStatus(BusinessStatus.INDEXED.equals(previousStatus)
                    ? BusinessStatus.INDEXED : BusinessStatus.INDEX_FAILED);
            document.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(document);
            throw exception;
        }
    }

    /**
     * 将知识库文档的重索引任务加入异步持久化队列
     *
     * @param tenantId   租户 ID
     * @param documentId 目标文档 ID
     */
    public void enqueueReindex(Long tenantId, Long documentId) {
        enqueueReindex(tenantId, documentId, null);
    }

        /**
         * enqueueReindex 方法。
         *
         * @param tenantId tenantId 参数
         * @param documentId documentId 参数
         * @param selection selection 参数
         */
    public void enqueueReindex(Long tenantId, Long documentId, RagModelSelection selection) {
        // 查询文档并校验归属
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getId, documentId).eq(KnowledgeDocumentEntity::getTenantId, tenantId));
        if (document == null) {
            throw new IllegalArgumentException("知识文档不属于当前租户。");
        }
        // 标记文档状态为索引中 INDEXING
        if (!BusinessStatus.INDEXED.equals(document.getDocumentStatus())) {
            document.setDocumentStatus(BusinessStatus.INDEXING);
            document.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(document);
        }
        // 向持久化任务队列服务入队异步重索引任务，最大重试 3 次
        enqueueDocumentReindex(tenantId, document, selection);
    }

    /**
     * 查询指定知识库文档关联的异步索引任务列表
     *
     * @param tenantId   租户 ID
     * @param documentId 目标文档 ID
     * @return 异步任务摘要列表
     */
    public List<AsyncTaskSummary> listIndexTasks(Long tenantId, Long documentId) {
        // 校验文档归属
        if (knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>().eq(KnowledgeDocumentEntity::getId, documentId).eq(KnowledgeDocumentEntity::getTenantId, tenantId)) == null) {
            throw new IllegalArgumentException("知识文档不属于当前租户。");
        }
        // 从任务队列检索索引关联的任务
        return taskQueueService.listKnowledgeReindexTasks(tenantId, documentId);
    }

    /**
     * 查询租户下所有的知识标签列表
     *
     * @param tenantId 租户 ID
     * @return 标签实体列表
     */
    public List<KnowledgeTagEntity> listTags(Long tenantId) {
        return knowledgeTagMapper.selectList(new LambdaQueryWrapper<KnowledgeTagEntity>().eq(KnowledgeTagEntity::getTenantId, tenantId).orderByAsc(KnowledgeTagEntity::getTagName));
    }

    /**
     * 按用户权限范围查询可读文档关联的知识标签列表
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param role     角色编码
     * @return 标签实体列表
     */
    public List<KnowledgeTagEntity> listTags(Long tenantId, Long userId, String role) {
        // 管理员及超级管理员查看租户下所有标签
        if (TENANT_ADMIN_ROLES.contains(role)) {
            return listTags(tenantId);
        }
        // 获取用户有权阅读的文档 ID 集合
        List<Long> visibleDocumentIds = knowledgeDocumentMapper.selectList(tenantQuery(tenantId).ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED))
                .stream()
                .map(KnowledgeDocumentEntity::getId)
                .toList();
        // 若没有可读文档则直接返回空列表
        if (visibleDocumentIds.isEmpty()) return List.of();
        // 查询可读文档关联的标签 ID 集合
        List<Long> tagIds = knowledgeDocumentTagMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentTagEntity>()
                        .eq(KnowledgeDocumentTagEntity::getTenantId, tenantId).in(KnowledgeDocumentTagEntity::getDocumentId, visibleDocumentIds)).stream()
                .map(KnowledgeDocumentTagEntity::getTagId).distinct().toList();
        // 若无标签关联则返回空列表
        if (tagIds.isEmpty()) return List.of();
        // 根据标签 ID 集合查出标签详细数据
        return knowledgeTagMapper.selectList(new LambdaQueryWrapper<KnowledgeTagEntity>()
                .eq(KnowledgeTagEntity::getTenantId, tenantId).in(KnowledgeTagEntity::getId, tagIds).orderByAsc(KnowledgeTagEntity::getTagName));
    }

    /**
     * 创建新的知识库标签
     *
     * @param tenantId 租户 ID
     * @param tagName  标签名称
     * @param tagColor 标签展示颜色 HEX
     * @return 创建成功的标签实体
     */
    public KnowledgeTagEntity createTag(Long tenantId, String tagName, String tagColor) {
        // 校验标签名称非空
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("标签名称不能为空。");
        }
        KnowledgeTagEntity tag = new KnowledgeTagEntity();
        tag.setTenantId(tenantId);
        tag.setTagName(tagName.trim());
        tag.setTagColor(tagColor);
        tag.setCreatedAt(LocalDateTime.now());
        knowledgeTagMapper.insert(tag);
        return tag;
    }

    /**
     * 为知识库文档绑定标签
     *
     * @param tenantId   租户 ID
     * @param documentId 目标文档 ID
     * @param tagId      目标标签 ID
     * @param userId     操作用户 ID
     * @param role       操作用户角色
     */
    public void bindTag(Long tenantId, Long documentId, Long tagId, Long userId, String role) {
        // 校验文档和标签的租户归属
        if (knowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>().eq(KnowledgeDocumentEntity::getId, documentId).eq(KnowledgeDocumentEntity::getTenantId, tenantId)) == null
                || knowledgeTagMapper.selectOne(new LambdaQueryWrapper<KnowledgeTagEntity>().eq(KnowledgeTagEntity::getId, tagId).eq(KnowledgeTagEntity::getTenantId, tenantId)) == null) {
            throw new IllegalArgumentException("文档或标签不属于当前租户。");
        }
        // 若未绑定过该标签则新增绑定关系
        if (knowledgeDocumentTagMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentTagEntity>()
                .eq(KnowledgeDocumentTagEntity::getTenantId, tenantId)
                .eq(KnowledgeDocumentTagEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentTagEntity::getTagId, tagId)) == 0) {
            KnowledgeDocumentTagEntity binding = new KnowledgeDocumentTagEntity();
            binding.setTenantId(tenantId);
            binding.setDocumentId(documentId);
            binding.setTagId(tagId);
            binding.setCreatedAt(LocalDateTime.now());
            knowledgeDocumentTagMapper.insert(binding);
        }
    }

    /**
     * 创建知识库文档基础记录（准备上传或导入）
     *
     * @param tenantId 租户 ID
     * @param request  创建文档请求参数
     * @return 创建的文档摘要
     */
    public KnowledgeDocumentSummary createDocument(Long tenantId, CreateKnowledgeDocumentRequest request) {
        // 校验租户参数
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        // 校验标题参数
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException(ApplicationMessages.KNOWLEDGE_TITLE_REQUIRED);
        }

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setTenantId(tenantId);
        entity.setTitle(request.title());
        entity.setLanguage(effectiveLanguage(request.language()).name());
        entity.setLanguageConfirmed(request.language() != null);
        entity.setSourceType(request.sourceType());
        entity.setFilePath(request.filePath());
        entity.setDocumentStatus(BusinessStatus.PENDING);
        entity.setVersionNo(1);
        entity.setChunkCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.insert(entity);
        return toSummary(entity);
    }

    /**
     * 上传知识文档文件并自动解析切片与构建 Embedding 向量索引
     *
     * @param tenantId 租户 ID
     * @param file     上传的 Multipart 文件
     * @return 处理完成的文档摘要
     */
    public KnowledgeDocumentSummary uploadAndIndex(Long tenantId, MultipartFile file, KnowledgeLanguage language) {
        return uploadAndIndex(tenantId, file, language, null);
    }

    public KnowledgeDocumentSummary uploadAndIndex(Long tenantId, MultipartFile file, KnowledgeLanguage language,
                                                   RagModelSelection selection) {
        // 校验租户 ID
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        // 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ApplicationMessages.KNOWLEDGE_FILE_REQUIRED);
        }
        long estimatedMegabytes = Math.max(1, (file.getSize() + 1_048_575L) / 1_048_576L);
        var admission = entitlementService.admitForConsumption(new AdmissionRequest(
                tenantId, null, "KNOWLEDGE_STORAGE", estimatedMegabytes,
                "知识文档上传", "knowledge-upload-" + tenantId + "-" + file.getSize()), true);
        if (admission.decision() == AdmissionDecision.DENY) {
            throw new IllegalStateException(admission.reason() + " " + admission.remediation());
        }
        knowledgeIndexingService.validateSupportedFormat(file.getOriginalFilename());
        // 先校验并解析模型选择，再落库和保存文件，避免产生必然失败的索引任务。
        profileResolver.resolve(tenantId, selection);

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setTenantId(tenantId);
        entity.setTitle(file.getOriginalFilename() == null ? KnowledgeConstants.DEFAULT_DOCUMENT_TITLE : file.getOriginalFilename());
        entity.setLanguage(effectiveLanguage(language).name());
        entity.setLanguageConfirmed(language != null);
        entity.setSourceType(resolveSourceType(entity.getTitle()));
        entity.setDocumentStatus(BusinessStatus.INDEXING);
        entity.setVersionNo(1);
        entity.setChunkCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        knowledgeDocumentMapper.insert(entity);

        // HTTP 请求只保存原始文件并提交持久化任务，模型调用和 Qdrant 写入由后台 Worker 执行。
        try {
            KnowledgeIndexingService.StoredDocument stored = knowledgeIndexingService.storeMultipartFile(entity.getId(), file);
            entity.setFilePath(stored.filePath());
            entity.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(entity);
            enqueueDocumentReindex(tenantId, entity, selection);
            return toSummary(entity);
        } catch (RuntimeException exception) {
            entity.setDocumentStatus(BusinessStatus.INDEX_FAILED);
            entity.setUpdatedAt(LocalDateTime.now());
            knowledgeDocumentMapper.updateById(entity);
            throw exception;
        }
    }

    /**
     * 对一段输入文本实时测试切片与 Embedding 向量生成
     *
     * @param tenantId 租户 ID
     * @param text     测试文本
     * @return 预览处理结果消息
     */
    public String previewEmbeddingFlow(Long tenantId, String text) {
        // 校验租户 ID
        if (tenantId == null) {
            throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
        }
        // 校验文本非空
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(ApplicationMessages.KNOWLEDGE_TEXT_REQUIRED);
        }
        RagEmbeddingProfile profile = profileResolver.resolveActive(tenantId);
        List<Float> vector = embeddingProvider.embedQuery(profile, text.trim());
        if (vector.size() != profile.vectorDimension()) {
            throw new IllegalStateException("Embedding 预览返回的向量维度与当前 RAG Profile 不一致。");
        }
        return "文本向量化预览完成，当前 Profile：" + profile.code() + "，向量维度：" + vector.size() + "。";
    }

    /**
     * 在知识库中根据查询语句进行向量与全文混合检索
     *
     * @param tenantId 租户 ID
     * @param query    搜索关键词或提问文本
     * @return 匹配的切片与相似度得分结果列表
     */
    public RagRetrievalOutcome search(Long tenantId, String query, KnowledgeLanguage queryLanguage) {
        return search(tenantId, query, queryLanguage, null);
    }

    public RagRetrievalOutcome search(Long tenantId, String query, KnowledgeLanguage queryLanguage,
                                      RagModelSelection selection) {
        // 校验查询语句非空
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(ApplicationMessages.KNOWLEDGE_QUERY_REQUIRED);
        }
        RagRetrievalOutcome outcome = ragRetrievalService.retrieve(new RagRetrievalRequest(
                tenantId, null, query, RetrievalLanguageStrategy.QUERY, queryLanguage,
                RetrievalScopeType.VISIBLE_DOCUMENTS, List.of(), 5, selection));
        ragRetrievalMetricService.record(tenantId, "KNOWLEDGE_SEARCH", outcome, false);
        return outcome;
    }

    private KnowledgeDocumentSummary toSummary(KnowledgeDocumentEntity entity) {
        return new KnowledgeDocumentSummary(
                entity.getId(),
                entity.getTitle(),
                entity.getSourceType(),
                entity.getDocumentStatus(),
                entity.getChunkCount(),
                documentLanguage(entity),
                Boolean.TRUE.equals(entity.getLanguageConfirmed())
        );
    }

    private KnowledgeLanguage documentLanguage(KnowledgeDocumentEntity entity) {
        try {
            return entity.getLanguage() == null ? KnowledgeLanguage.OTHER : KnowledgeLanguage.valueOf(entity.getLanguage());
        } catch (IllegalArgumentException ignored) {
            return KnowledgeLanguage.OTHER;
        }
    }

    private KnowledgeLanguage effectiveLanguage(KnowledgeLanguage language) {
        return language == null ? KnowledgeLanguage.OTHER : language;
    }

    private String resolveSourceType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return KnowledgeConstants.SOURCE_TYPE_TEXT;
        }
        return filename.substring(dotIndex + 1).toUpperCase();
    }

    private LambdaQueryWrapper<KnowledgeDocumentEntity> tenantQuery(Long tenantId) {
        return new LambdaQueryWrapper<KnowledgeDocumentEntity>().eq(KnowledgeDocumentEntity::getTenantId, tenantId);
    }

    private LambdaQueryWrapper<KnowledgeDocumentEntity> documentPageConditions(
            Long tenantId, KnowledgeDocumentPageQuery query) {
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = tenantQuery(tenantId)
                .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED);
        if (query.keyword() != null) wrapper.like(KnowledgeDocumentEntity::getTitle, query.keyword());
        if (query.language() != null) wrapper.eq(KnowledgeDocumentEntity::getLanguage, query.language().name());
        if (query.status() != null) wrapper.eq(KnowledgeDocumentEntity::getDocumentStatus, query.status());
        return wrapper;
    }

    private Map<Long, KnowledgeDocumentIndexStateEntity> documentIndexStates(
            List<KnowledgeDocumentEntity> documents) {
        if (documents.isEmpty()) return Map.of();
        List<Long> documentIds = documents.stream().map(KnowledgeDocumentEntity::getId).toList();
        Map<Long, KnowledgeDocumentIndexStateEntity> states = new LinkedHashMap<>();
        documentIndexStateMapper.selectBatchIds(documentIds)
                .forEach(state -> states.put(state.getDocumentId(), state));
        return states;
    }

    private KnowledgeDocumentPageItem toPageItem(KnowledgeDocumentEntity document,
                                                  KnowledgeDocumentIndexStateEntity state) {
        return new KnowledgeDocumentPageItem(document.getId(), document.getTitle(), document.getSourceType(),
                document.getDocumentStatus(), document.getChunkCount(), documentLanguage(document),
                Boolean.TRUE.equals(document.getLanguageConfirmed()), document.getVersionNo(),
                state == null ? document.getDocumentStatus() : state.getIndexStatus(),
                state == null ? null : state.getProfileId(),
                state == null ? null : state.getActiveGenerationId(),
                state == null ? null : state.getIndexedAt(), document.getUpdatedAt());
    }

    private KnowledgeIndexGenerationSummary generationSummary(RagEmbeddingProfile profile,
                                                               KnowledgeIndexGenerationEntity generation) {
        return new KnowledgeIndexGenerationSummary(
                generation == null ? null : generation.getId(),
                generation == null ? IndexGenerationStatus.NOT_CREATED.name() : generation.getGenerationStatus(),
                generation == null ? null : generation.getCollectionName(), profile.code(), profile.name(),
                profile.embeddingModelKey(), profile.rerankerModelKey(),
                generation == null ? 0 : generation.getTotalDocuments(),
                generation == null ? 0 : generation.getTotalChunks(),
                generation == null ? 0 : generation.getIndexedChunks(),
                generation == null ? 0 : generation.getFailureCount(),
                generation == null ? null : generation.getStartedAt(),
                generation == null ? null : generation.getCompletedAt(),
                generation == null ? null : generation.getActivatedAt());
    }

    private KnowledgeDocumentEntity requireDocument(Long tenantId, Long documentId) {
        requireTenant(tenantId);
        if (documentId == null) throw new IllegalArgumentException("知识文档标识不能为空。");
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getId, documentId)
                        .eq(KnowledgeDocumentEntity::getTenantId, tenantId)
                        .ne(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.DELETED));
        if (document == null) throw new IllegalArgumentException("知识文档不存在或不属于当前租户。");
        return document;
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) throw new IllegalArgumentException(ApplicationMessages.TENANT_ID_REQUIRED);
    }

        /**
         * embeddingModels 方法。
         *
         * @param tenantId tenantId 参数
         * @return List<RagEmbeddingModelOption> 返回对象
         */
    public List<RagEmbeddingModelOption> embeddingModels(Long tenantId) {
        return profileResolver.listEmbeddingModels(tenantId);
    }

    private void enqueueDocumentReindex(Long tenantId, KnowledgeDocumentEntity document,
                                         RagModelSelection selection) {
        Map<String, Object> payload = modelPayload(tenantId, selection);
        payload.put(TASK_FIELD_DOCUMENT_ID, document.getId());
        payload.put(TASK_FIELD_LANGUAGE, documentLanguage(document).name());
        taskQueueService.enqueue(tenantId, TaskType.KNOWLEDGE_REINDEX,
                payload,
                REINDEX_MAX_RETRIES);
    }

    private Map<String, Object> modelPayload(Long tenantId, RagModelSelection selection) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(TASK_FIELD_TENANT_ID, tenantId);
        if (selection != null && selection.source() != null) {
            payload.put(TASK_FIELD_MODEL_SOURCE, selection.source().name());
            payload.put(TASK_FIELD_MODEL_ID, selection.modelId());
            payload.put(TASK_FIELD_MODEL_KEY, selection.modelKey());
        }
        return payload;
    }

    private String safeGenerationError(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "整批索引重建失败。" : exception.getMessage();
    }

    private record StagedDocumentResult(Long documentId, int chunkCount, int versionNo) {
    }
}
