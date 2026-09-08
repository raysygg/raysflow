package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.common.exception.RagEmbeddingInvocationException;
import com.acme.agentstudio.domain.knowledge.model.ActiveIndexGeneration;
import com.acme.agentstudio.domain.knowledge.model.ContextAssemblyResult;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.LexicalEncoding;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchRequest;
import com.acme.agentstudio.domain.knowledge.model.QueryUnderstandingResult;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalOutcome;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RagStageTimings;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalDebugSummary;
import com.acme.agentstudio.domain.knowledge.model.RagScoreLayerSummary;
import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankMode;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.domain.knowledge.model.RerankerExecutionResult;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCalibration;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidateScore;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannel;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannelExecution;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannelExecutionStatus;
import com.acme.agentstudio.domain.knowledge.model.RetrievalDegradePolicy;
import com.acme.agentstudio.domain.knowledge.model.RetrievalHitReason;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageSource;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchRequest;
import com.acme.agentstudio.domain.knowledge.port.ContextAssemblyService;
import com.acme.agentstudio.domain.knowledge.port.EmbeddingProvider;
import com.acme.agentstudio.domain.knowledge.port.ModelProfileResolver;
import com.acme.agentstudio.domain.knowledge.port.QueryUnderstandingService;
import com.acme.agentstudio.domain.knowledge.port.RerankerProvider;
import com.acme.agentstudio.domain.knowledge.port.RetrievalDiversitySelector;
import com.acme.agentstudio.domain.knowledge.port.RetrievalScoreCalibrator;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.acme.agentstudio.domain.model.ModelInvocationErrorCategory;
import com.acme.agentstudio.infrastructure.model.strategy.ModelInvocationErrorClassifier;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.acme.agentstudio.infrastructure.rag.retrieval.MultilingualSparseEncoder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RagRetrieval 业务服务接口。
 * 定义 RagRetrieval 相关的核心业务契约与流程接口。
 */
/** RAG 检索短流程协调器，评分、筛选和上下文还原分别交给领域组件处理。 */
@Service
public class RagRetrievalService {
    private static final Logger LOG = LoggerFactory.getLogger(RagRetrievalService.class);
    private static final double RRF_RANK_CONSTANT = 60D;
    private static final double UNRERANKED_FUSION_FACTOR = 0.85D;
    private static final String DEGRADE_RERANKER_BUDGET_EXCEEDED = "RERANKER_BUDGET_EXCEEDED";
    private static final String DEGRADE_RERANKER_COST_UNKNOWN = "RERANKER_COST_UNKNOWN";
    private static final String DEGRADE_RERANKER_EMPTY_RESPONSE = "RERANKER_EMPTY_RESPONSE";
    private static final String DEGRADE_RERANKER_TIMEOUT = "RERANKER_TIMEOUT";
    private static final String DEGRADE_RERANKER_FAILURE = "RERANKER_FAILURE";
    private static final String DEGRADE_RETRIEVAL_CHANNEL_FAILURE = "RETRIEVAL_CHANNEL_FAILURE";
    private static final int DEFAULT_CANDIDATE_MULTIPLIER = 10;
    private static final String CHILD_ROLE = "CHILD";

    private final ModelProfileResolver profileResolver;
    private final KnowledgeIndexGenerationService generationService;
    private final KnowledgeDocumentVisibilityFilter visibilityFilter;
    private final QueryUnderstandingService queryUnderstandingService;
    private final EmbeddingProvider embeddingProvider;
    private final VectorIndexProvider vectorIndexProvider;
    private final RerankerProvider rerankerProvider;
    private final RetrievalScoreCalibrator scoreCalibrator;
    private final RetrievalDiversitySelector diversitySelector;
    private final ContextAssemblyService contextAssemblyService;
    private final KnowledgeChunkMapper chunkMapper;
    private final MultilingualSparseEncoder sparseEncoder;
    private final RerankerBudgetGuard budgetGuard;
    private final ModelInvocationErrorClassifier modelErrorClassifier;

    public RagRetrievalService(ModelProfileResolver profileResolver,
                               KnowledgeIndexGenerationService generationService,
                               KnowledgeDocumentVisibilityFilter visibilityFilter,
                               QueryUnderstandingService queryUnderstandingService,
                               EmbeddingProvider embeddingProvider,
                               VectorIndexProvider vectorIndexProvider,
                               RerankerProvider rerankerProvider,
                               RetrievalScoreCalibrator scoreCalibrator,
                               RetrievalDiversitySelector diversitySelector,
                               ContextAssemblyService contextAssemblyService,
                               KnowledgeChunkMapper chunkMapper,
                               MultilingualSparseEncoder sparseEncoder,
                               RerankerBudgetGuard budgetGuard,
                               ModelInvocationErrorClassifier modelErrorClassifier) {
        this.profileResolver = profileResolver;
        this.generationService = generationService;
        this.visibilityFilter = visibilityFilter;
        this.queryUnderstandingService = queryUnderstandingService;
        this.embeddingProvider = embeddingProvider;
        this.vectorIndexProvider = vectorIndexProvider;
        this.rerankerProvider = rerankerProvider;
        this.scoreCalibrator = scoreCalibrator;
        this.diversitySelector = diversitySelector;
        this.contextAssemblyService = contextAssemblyService;
        this.chunkMapper = chunkMapper;
        this.sparseEncoder = sparseEncoder;
        this.budgetGuard = budgetGuard;
        this.modelErrorClassifier = modelErrorClassifier;
    }

        /**
         * retrieve 方法。
         *
         * @param request request 参数
         * @return RagRetrievalOutcome 返回对象
         */
    public RagRetrievalOutcome retrieve(RagRetrievalRequest request) {
        validate(request);
        long startedAt = System.nanoTime();
        RagEmbeddingProfile profile = profileResolver.resolve(request.tenantId(), request.modelSelection());
        ActiveIndexGeneration generation = generationService.requireSearchable(request.tenantId(), profile);
        List<KnowledgeDocumentEntity> documents = visibilityFilter.filter(request, profile.id(), generation.id());
        LanguageDecision language = resolveLanguage(request, documents);
        if (documents.isEmpty()) {
            return emptyOutcome(request, profile, language, elapsedMs(startedAt));
        }

        long rewriteStarted = System.nanoTime();
        QueryUnderstandingResult understood = queryUnderstandingService.understand(
                request.tenantId(), profile, request.query());
        long rewriteMs = elapsedMs(rewriteStarted);

        SearchBatch searchBatch = recall(request, profile, generation, documents, understood, language);
        List<RetrievalCandidate> candidates = loadCandidates(
                generation.id(), searchBatch, documents);
        RerankBatch rerankBatch = rerank(profile, understood.originalQuery(), candidates, searchBatch);
        RetrievalCalibration calibration = scoreCalibrator.calibrate(profile, rerankBatch.candidates());
        List<RetrievalCandidate> selected = calibration.noHit()
                ? List.of()
                : diversitySelector.select(profile, calibration.acceptedCandidates(), request.topK());

        long contextStarted = System.nanoTime();
        ContextAssemblyResult context = contextAssemblyService.assemble(profile, generation.id(), selected);
        long contextMs = elapsedMs(contextStarted);
        RagStageTimings timings = new RagStageTimings(rewriteMs, searchBatch.embeddingMs(),
                searchBatch.vectorSearchMs(), rerankBatch.elapsedMs(), contextMs);
        long elapsedMs = elapsedMs(startedAt);
        RagRetrievalOutcome outcome = outcome(request, profile, generation, language, understood, searchBatch,
                rerankBatch, calibration, context, timings, elapsedMs);
        logOutcome(request, outcome);
        return outcome;
    }

    /** 父级上下文已成为主检索链的一部分，保留该方法只为已有调用方提供同义入口。 */
    public RagRetrievalOutcome retrieveWithContext(RagRetrievalRequest request) {
        return retrieve(request);
    }

    private SearchBatch recall(RagRetrievalRequest request, RagEmbeddingProfile profile,
                               ActiveIndexGeneration generation, List<KnowledgeDocumentEntity> documents,
                               QueryUnderstandingResult understood, LanguageDecision language) {
        int candidateLimit = candidateLimit(profile, request.topK());
        Set<Long> documentIds = documents.stream().map(KnowledgeDocumentEntity::getId).collect(Collectors.toSet());
        LexicalEncoding lexicalQuery = sparseEncoder.encodeQuery(understood.originalQuery());
        LexicalSearchRequest lexicalRequest = new LexicalSearchRequest(request.tenantId(), generation.id(),
                generation.collectionName(), lexicalQuery.sparseVector(), lexicalQuery.exactKeys(),
                documentIds, candidateLimit);
        RetrievalChannel sparseChannel = languageChannel(language.language());
        long embeddingStarted = System.nanoTime();
        List<Float> originalVector = embedQuery(profile, understood.originalQuery());
        List<Float> rewrittenVector = understood.rewritten()
                ? embedQuery(profile, understood.rewrittenQuery())
                : List.of();
        long embeddingMs = elapsedMs(embeddingStarted);

        // Embedding 成功后才允许访问当前 Generation；随后三个召回通道独立并行执行。
        CompletableFuture<ChannelRecallResult> sparseFuture = CompletableFuture.supplyAsync(
                () -> recallSparse(lexicalRequest, sparseChannel));
        CompletableFuture<ChannelRecallResult> exactFuture = CompletableFuture.supplyAsync(
                () -> recallExact(lexicalRequest));
        long searchStarted = System.nanoTime();
        List<VectorSearchCandidate> original = vectorIndexProvider.search(searchRequest(
                request, generation, originalVector, documentIds, candidateLimit));
        List<VectorSearchCandidate> rewritten = rewrittenVector.isEmpty() ? List.of()
                : vectorIndexProvider.search(searchRequest(
                        request, generation, rewrittenVector, documentIds, candidateLimit));
        List<VectorSearchCandidate> merged = mergeCandidates(original, rewritten, candidateLimit);
        ChannelRecallResult sparseResult = sparseFuture.join();
        ChannelRecallResult exactResult = exactFuture.join();
        Map<Long, RetrievalCandidateScore> lexicalScores = new LinkedHashMap<>();
        for (LexicalSearchCandidate hit : sparseResult.candidates()) {
            double sparseScore = hit.sparseScore() / (hit.sparseScore() + 1D);
            RetrievalCandidateScore score = new RetrievalCandidateScore(hit.chunkId(), sparseScore,
                    0D, sparseScore, List.of(sparseChannel), List.of(RetrievalHitReason.SPARSE_TERM_MATCH));
            lexicalScores.merge(hit.chunkId(), score, this::mergeLexicalScore);
        }
        for (LexicalSearchCandidate hit : exactResult.candidates()) {
            RetrievalCandidateScore score = new RetrievalCandidateScore(hit.chunkId(), 0D,
                    profile.exactRecallBoost(), profile.exactRecallBoost(), List.of(RetrievalChannel.EXACT),
                    List.of(RetrievalHitReason.EXACT_IDENTIFIER));
            lexicalScores.merge(hit.chunkId(), score, this::mergeLexicalScore);
        }
        List<RetrievalChannelExecution> executions = List.of(
                RetrievalChannelExecution.success(RetrievalChannel.VECTOR, merged.size()),
                sparseResult.execution(), exactResult.execution());
        List<RetrievalChannel> channels = executions.stream()
                .filter(execution -> execution.status() == RetrievalChannelExecutionStatus.SUCCESS)
                .map(RetrievalChannelExecution::channel).distinct().toList();
        return new SearchBatch(merged, List.copyOf(lexicalScores.values()), channels, executions,
                embeddingMs, elapsedMs(searchStarted));
    }

    private ChannelRecallResult recallSparse(LexicalSearchRequest request, RetrievalChannel channel) {
        if (request.queryVector().isEmpty()) {
            return new ChannelRecallResult(List.of(), RetrievalChannelExecution.skipped(channel));
        }
        try {
            List<LexicalSearchCandidate> candidates = vectorIndexProvider.searchSparse(request);
            return new ChannelRecallResult(candidates, RetrievalChannelExecution.success(channel, candidates.size()));
        } catch (RuntimeException exception) {
            LOG.warn("Qdrant Sparse 召回失败，本次请求保留其他成功通道。", exception);
            return new ChannelRecallResult(List.of(), RetrievalChannelExecution.failed(channel));
        }
    }

    private ChannelRecallResult recallExact(LexicalSearchRequest request) {
        if (request.exactKeys().isEmpty()) {
            return new ChannelRecallResult(List.of(), RetrievalChannelExecution.skipped(RetrievalChannel.EXACT));
        }
        try {
            List<LexicalSearchCandidate> candidates = vectorIndexProvider.searchExact(request);
            return new ChannelRecallResult(candidates,
                    RetrievalChannelExecution.success(RetrievalChannel.EXACT, candidates.size()));
        } catch (RuntimeException exception) {
            LOG.warn("Qdrant Exact 召回失败，本次请求保留其他成功通道。", exception);
            return new ChannelRecallResult(List.of(), RetrievalChannelExecution.failed(RetrievalChannel.EXACT));
        }
    }

    private RetrievalCandidateScore mergeLexicalScore(RetrievalCandidateScore left,
                                                       RetrievalCandidateScore right) {
        List<RetrievalChannel> channels = java.util.stream.Stream.concat(
                left.channels().stream(), right.channels().stream()).distinct().toList();
        List<RetrievalHitReason> reasons = java.util.stream.Stream.concat(
                left.hitReasons().stream(), right.hitReasons().stream()).distinct().toList();
        double sparse = Math.max(left.sparseScore(), right.sparseScore());
        double exact = Math.max(left.exactScore(), right.exactScore());
        return new RetrievalCandidateScore(left.chunkId(), sparse, exact,
                Math.min(1D, sparse + exact), channels, reasons);
    }

    private VectorSearchRequest searchRequest(RagRetrievalRequest request, ActiveIndexGeneration generation,
                                              List<Float> vector, Set<Long> documentIds, int limit) {
        return new VectorSearchRequest(request.tenantId(), generation.id(), generation.collectionName(),
                vector, documentIds, limit);
    }

    private List<VectorSearchCandidate> mergeCandidates(List<VectorSearchCandidate> original,
                                                        List<VectorSearchCandidate> rewritten, int limit) {
        Map<Long, VectorSearchCandidate> merged = new LinkedHashMap<>();
        addCandidates(merged, original);
        addCandidates(merged, rewritten);
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(VectorSearchCandidate::vectorScore).reversed())
                .limit(limit)
                .toList();
    }

    private void addCandidates(Map<Long, VectorSearchCandidate> merged, List<VectorSearchCandidate> candidates) {
        for (VectorSearchCandidate candidate : candidates) {
            if (candidate.chunkId() == null) continue;
            merged.merge(candidate.chunkId(), candidate,
                    (left, right) -> left.vectorScore() >= right.vectorScore() ? left : right);
        }
    }

    private List<RetrievalCandidate> loadCandidates(Long generationId,
                                                    SearchBatch searchBatch,
                                                    List<KnowledgeDocumentEntity> documents) {
        List<VectorSearchCandidate> vectorCandidates = searchBatch.vectorCandidates();
        List<Long> lexicalIds = searchBatch.lexicalScores().stream().map(RetrievalCandidateScore::chunkId).toList();
        if (vectorCandidates.isEmpty() && lexicalIds.isEmpty()) return List.of();
        List<Long> chunkIds = new ArrayList<>(vectorCandidates.stream().map(VectorSearchCandidate::chunkId).distinct().toList());
        lexicalIds.stream().filter(id -> !chunkIds.contains(id)).forEach(chunkIds::add);
        Map<Long, KnowledgeChunkEntity> chunks = chunkMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeChunkEntity>()
                                .eq(KnowledgeChunkEntity::getIndexGenerationId, generationId)
                                .eq(KnowledgeChunkEntity::getChunkRole, CHILD_ROLE)
                                .in(KnowledgeChunkEntity::getId, chunkIds))
                .stream().collect(Collectors.toMap(KnowledgeChunkEntity::getId, Function.identity()));
        Map<Long, KnowledgeDocumentEntity> documentIndex = documents.stream()
                .collect(Collectors.toMap(KnowledgeDocumentEntity::getId, Function.identity()));
        List<RetrievalCandidate> result = new ArrayList<>();
        Map<Long, VectorSearchCandidate> vectors = vectorCandidates.stream()
                .collect(Collectors.toMap(VectorSearchCandidate::chunkId, Function.identity(), (a, b) -> a));
        for (Long chunkId : chunkIds) {
            VectorSearchCandidate vector = vectors.get(chunkId);
            KnowledgeChunkEntity chunk = chunks.get(chunkId);
            KnowledgeDocumentEntity document = chunk == null ? null : documentIndex.get(chunk.getDocumentId());
            if (chunk == null || document == null || chunk.getChunkText() == null || chunk.getChunkText().isBlank()) continue;
            double vectorScore = vector == null ? 0D : vector.vectorScore();
            result.add(new RetrievalCandidate(chunk.getId(), document.getId(), chunk.getParentChunkId(),
                    chunk.getChunkNo(), chunk.getSectionPath(), chunk.getChunkText(), document.getTitle(),
                    documentLanguage(document), chunk.getContentHash(), safeInt(chunk.getTokenCount()),
                    vectorScore, vectorScore));
        }
        return List.copyOf(result);
    }

    private RerankBatch rerank(RagEmbeddingProfile profile, String query, List<RetrievalCandidate> candidates,
                               SearchBatch searchBatch) {
        if (candidates.isEmpty()) return new RerankBatch(List.of(), false, RerankMode.STANDARD_HYBRID, 0);
        List<RetrievalCandidate> fused = fusedCandidates(profile, candidates, searchBatch.lexicalScores());
        boolean hasRerankerConfigured = profile.rerankerEnabled()
                && profile.rerankerModelKey() != null && !profile.rerankerModelKey().isBlank();
        if (!hasRerankerConfigured) {
            // 未配置远程重排模型时，使用 Dense、Sparse、Exact 的标准融合排序。
            List<RetrievalCandidate> fallback = fused.stream()
                    .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                    .toList();
            return new RerankBatch(fallback, false, RerankMode.STANDARD_HYBRID, 0, null, "", false);
        }
        long startedAt = System.nanoTime();
        try {
            if (profile.lateInteractionEnabled()) {
                LOG.info("当前模型服务未声明 Late Interaction 接口，使用配置的多语言 Reranker，profile={}",
                        profile.code());
            }
            int rerankLimit = Math.min(profile.rerankerCandidateLimit(), candidates.size());
            RerankerBudgetGuard.BudgetDecision budget = budgetGuard.check(profile.tenantId(), profile, query, rerankLimit);
            if (!budget.allowed()) {
                if (profile.degradePolicy().failClosed()) {
                    throw new IllegalStateException("Reranker 月度预算已用尽。");
                }
                List<RetrievalCandidate> fallback = List.copyOf(fused);
                return new RerankBatch(fallback, true, RerankMode.HYBRID_FALLBACK, 0,
                        budget.estimatedCost(), budget.unknownCost() ? DEGRADE_RERANKER_COST_UNKNOWN
                        : DEGRADE_RERANKER_BUDGET_EXCEEDED, !budget.unknownCost());
            }
            List<RetrievalCandidate> rerankInput = fused.stream().limit(rerankLimit).toList();
            List<RerankCandidate> rerankCandidates = rerankInput.stream().map(this::rerankCandidate).toList();
            RerankerExecutionResult execution = CompletableFuture.supplyAsync(
                            () -> rerankerProvider.rerank(profile, query, rerankCandidates))
                    .orTimeout(profile.rerankerTimeoutSeconds(), TimeUnit.SECONDS)
                    .join();
            execution = execution.withEstimate(budget.estimatedInputTokens(), budget.estimatedCost());
            List<RerankResult> results = execution.scores();
            if (results.isEmpty()) {
                if (profile.degradePolicy().failClosed()) {
                    throw new IllegalStateException("Reranker 返回了无效的空响应。");
                }
                // 远程返回空时同样降级为标准向量排序
                List<RetrievalCandidate> fallback = fused.stream()
                        .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                        .toList();
                return new RerankBatch(fallback, true, RerankMode.HYBRID_FALLBACK, elapsedMs(startedAt),
                        execution.cost(), DEGRADE_RERANKER_EMPTY_RESPONSE, false);
            }
            Map<Long, RerankResult> scores = results.stream().collect(Collectors.toMap(
                    RerankResult::chunkId, Function.identity(),
                    (left, right) -> left.score() >= right.score() ? left : right));
            List<RetrievalCandidate> reranked = fused.stream()
                    .map(candidate -> {
                        RerankResult score = scores.get(candidate.chunkId());
                        return score == null
                                ? candidate.withRelevanceScore(candidate.fusionScore() * UNRERANKED_FUSION_FACTOR)
                                : candidate.withRerankScore(score.score());
                    })
                    .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                    .toList();
            return new RerankBatch(reranked, false, RerankMode.MULTILINGUAL_RERANKER, elapsedMs(startedAt),
                    execution.cost(), "", false);
        } catch (RuntimeException exception) {
            if (profile.degradePolicy().failClosed()) {
                throw new IllegalStateException("RAG 重排服务不可用，当前 Profile 禁止降级检索。", exception);
            }
            LOG.info("RAG 重排失败或未响应，按标准向量排序执行，profile={}, candidates={}",
                    profile.code(), candidates.size());
            List<RetrievalCandidate> fallback = fused.stream()
                    .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                    .toList();
            return new RerankBatch(fallback, true, RerankMode.HYBRID_FALLBACK, elapsedMs(startedAt),
                    null, rerankerFailureReason(exception), false);
        }
    }

    private String rerankerFailureReason(RuntimeException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof java.util.concurrent.TimeoutException) return DEGRADE_RERANKER_TIMEOUT;
            cause = cause.getCause();
        }
        return DEGRADE_RERANKER_FAILURE;
    }

    private List<RetrievalCandidate> fusedCandidates(RagEmbeddingProfile profile,
                                                     List<RetrievalCandidate> candidates,
                                                     List<RetrievalCandidateScore> lexicalScores) {
        Map<Long, Integer> denseRanks = rankCandidates(candidates);
        Map<Long, Integer> lexicalRanks = rankLexicalScores(lexicalScores);
        double vectorWeight = Math.max(0D, profile.vectorRecallWeight());
        double lexicalWeight = Math.max(0D, profile.lexicalRecallWeight());
        double activeWeight = (denseRanks.isEmpty() ? 0D : vectorWeight)
                + (lexicalRanks.isEmpty() ? 0D : lexicalWeight);
        double maximumRrf = activeWeight <= 0D ? 0D : activeWeight / (RRF_RANK_CONSTANT + 1D);
        return candidates.stream().map(candidate -> {
                    RetrievalCandidateScore lexical = lexicalScore(candidate, lexicalScores);
                    double rrf = weightedRrf(candidate.chunkId(), denseRanks, lexicalRanks,
                            vectorWeight, lexicalWeight);
                    double normalizedRrf = maximumRrf <= 0D ? 0D : rrf / maximumRrf;
                    double exact = lexical == null ? 0D : lexical.exactScore();
                    double fusion = Math.min(1D, normalizedRrf + exact);
                    List<RetrievalChannel> channels = candidateChannels(candidate, lexical);
                    List<RetrievalHitReason> reasons = candidateHitReasons(candidate, lexical, channels);
                    return candidate.withChannelScores(candidate.vectorScore(),
                            lexical == null ? 0D : lexical.sparseScore(),
                            exact, fusion, 0D, channels, reasons);
                })
                .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed()).toList();
    }

    private Map<Long, Integer> rankCandidates(List<RetrievalCandidate> candidates) {
        Map<Long, Integer> ranks = new LinkedHashMap<>();
        List<RetrievalCandidate> ranked = candidates.stream().filter(candidate -> candidate.vectorScore() > 0D)
                .sorted(Comparator.comparingDouble(RetrievalCandidate::vectorScore).reversed()).toList();
        for (int index = 0; index < ranked.size(); index++) ranks.put(ranked.get(index).chunkId(), index + 1);
        return ranks;
    }

    private Map<Long, Integer> rankLexicalScores(List<RetrievalCandidateScore> lexicalScores) {
        Map<Long, Integer> ranks = new LinkedHashMap<>();
        List<RetrievalCandidateScore> ranked = lexicalScores.stream()
                .sorted(Comparator.comparingDouble(RetrievalCandidateScore::fusionScore).reversed()).toList();
        for (int index = 0; index < ranked.size(); index++) ranks.put(ranked.get(index).chunkId(), index + 1);
        return ranks;
    }

    private double weightedRrf(Long chunkId, Map<Long, Integer> denseRanks, Map<Long, Integer> lexicalRanks,
                               double vectorWeight, double lexicalWeight) {
        Integer denseRank = denseRanks.get(chunkId);
        Integer lexicalRank = lexicalRanks.get(chunkId);
        double dense = denseRank == null ? 0D : vectorWeight / (RRF_RANK_CONSTANT + denseRank);
        double lexical = lexicalRank == null ? 0D : lexicalWeight / (RRF_RANK_CONSTANT + lexicalRank);
        return dense + lexical;
    }

    private RetrievalCandidateScore lexicalScore(RetrievalCandidate candidate,
                                                  List<RetrievalCandidateScore> lexicalScores) {
        return lexicalScores.stream().filter(score -> candidate.chunkId().equals(score.chunkId())).findFirst().orElse(null);
    }

    private List<RetrievalChannel> candidateChannels(RetrievalCandidate candidate,
                                                     RetrievalCandidateScore lexical) {
        java.util.stream.Stream<RetrievalChannel> dense = candidate.vectorScore() > 0D
                ? java.util.stream.Stream.of(RetrievalChannel.VECTOR) : java.util.stream.Stream.empty();
        java.util.stream.Stream<RetrievalChannel> lexicalChannels = lexical == null
                ? java.util.stream.Stream.empty() : lexical.channels().stream();
        return java.util.stream.Stream.concat(dense, lexicalChannels).distinct().toList();
    }

    private List<RetrievalHitReason> candidateHitReasons(RetrievalCandidate candidate,
                                                         RetrievalCandidateScore lexical,
                                                         List<RetrievalChannel> channels) {
        List<RetrievalHitReason> reasons = new ArrayList<>();
        if (candidate.vectorScore() > 0D) reasons.add(RetrievalHitReason.VECTOR_SIMILARITY);
        if (lexical != null) reasons.addAll(lexical.hitReasons());
        if (channels.size() > 1) reasons.add(RetrievalHitReason.HYBRID_MATCH);
        return reasons.stream().distinct().toList();
    }

    private RetrievalChannel languageChannel(KnowledgeLanguage language) {
        return language == KnowledgeLanguage.ZH ? RetrievalChannel.LEXICAL_ZH
                : language == KnowledgeLanguage.EN ? RetrievalChannel.LEXICAL_EN
                : RetrievalChannel.LEXICAL_GENERAL;
    }

    private RerankCandidate rerankCandidate(RetrievalCandidate candidate) {
        String text = "文档：" + candidate.source() + "\n章节：" + candidate.sectionPath()
                + "\n内容：" + candidate.text();
        return new RerankCandidate(candidate.chunkId(), text, candidate.vectorScore());
    }

    private RagRetrievalOutcome outcome(RagRetrievalRequest request, RagEmbeddingProfile profile,
                                        ActiveIndexGeneration generation, LanguageDecision language,
                                        QueryUnderstandingResult understood, SearchBatch searchBatch,
                                        RerankBatch rerankBatch, RetrievalCalibration calibration,
                                        ContextAssemblyResult context, RagStageTimings timings, long elapsedMs) {
        boolean channelDegraded = searchBatch.channelExecutions().stream()
                .anyMatch(execution -> execution.status() == RetrievalChannelExecutionStatus.FAILED);
        boolean degraded = rerankBatch.degraded() || channelDegraded;
        String degradeReason = !rerankBatch.degradeReason().isBlank() ? rerankBatch.degradeReason()
                : channelDegraded ? DEGRADE_RETRIEVAL_CHANNEL_FAILURE : "";
        RagRetrievalOutcome base = new RagRetrievalOutcome(context.results(), language.language(), language.source(), request.scope(),
                searchBatch.channels(), uniqueCandidateCount(searchBatch),
                rerankBatch.candidates().size(), context.results().size(), calibration.topScore(),
                calibration.scoreGap(), understood.rewritten(), degraded, calibration.noHit(),
                rerankBatch.mode(), profile.code(), generation.id(), context.parentCoverage(), timings, elapsedMs);
        RagScoreLayerSummary scoreLayers = new RagScoreLayerSummary(
                maxScore(rerankBatch.candidates(), RetrievalCandidate::denseScore),
                maxScore(rerankBatch.candidates(), RetrievalCandidate::sparseScore),
                maxScore(rerankBatch.candidates(), RetrievalCandidate::exactScore),
                maxScore(rerankBatch.candidates(), RetrievalCandidate::fusionScore),
                maxScore(rerankBatch.candidates(), RetrievalCandidate::rerankScore));
        RagRetrievalDebugSummary debug = new RagRetrievalDebugSummary(profile.embeddingSource().name(),
                language.language(), searchBatch.channels(), searchBatch.vectorCandidates().size(),
                searchBatch.lexicalScores().size(), (int) searchBatch.lexicalScores().stream()
                .filter(score -> score.exactScore() > 0D).count(), uniqueCandidateCount(searchBatch),
                rerankBatch.mode(), degraded, calibration.topScore(), calibration.scoreGap(),
                scoreLayers, timings, context.results().stream().map(RagSearchResult::sourceLabel).toList(),
                searchBatch.channelExecutions(), context.skippedContexts());
        return base.withOperationalMetadata(profile.embeddingSource().name(), degradeReason,
                rerankBatch.estimatedCost(), rerankBatch.budgetExceeded(), debug);
    }

    private RagRetrievalOutcome emptyOutcome(RagRetrievalRequest request, RagEmbeddingProfile profile,
                                             LanguageDecision language,
                                             long elapsedMs) {
        return new RagRetrievalOutcome(List.of(), language.language(), language.source(), request.scope(),
                List.of(RetrievalChannel.VECTOR), 0, 0, 0, 0D, 0D, false,
                false, true, RerankMode.STANDARD_HYBRID, profile.code(), null, 0D,
                RagStageTimings.empty(), elapsedMs);
    }

    private void logOutcome(RagRetrievalRequest request, RagRetrievalOutcome outcome) {
        LOG.info("RAG 检索完成，tenantId={}, scope={}, queryFingerprint={}, profile={}, generation={}, "
                        + "initialCandidates={}, rerankedCandidates={}, hits={}, topScore={}, scoreGap={}, "
                        + "rewritten={}, degraded={}, elapsedMs={}",
                request.tenantId(), request.scope(), fingerprint(request.query()), outcome.embeddingProfile(),
                outcome.indexGenerationId(), outcome.initialCandidateCount(), outcome.rerankedCandidateCount(),
                outcome.hitCount(), outcome.topScore(), outcome.scoreGap(), outcome.queryRewritten(),
                outcome.degraded(), outcome.elapsedMs());
    }

    private LanguageDecision resolveLanguage(RagRetrievalRequest request, List<KnowledgeDocumentEntity> documents) {
        if (request.languageStrategy() == RetrievalLanguageStrategy.QUERY && request.queryLanguage() != null) {
            return new LanguageDecision(request.queryLanguage(), RetrievalLanguageSource.REQUEST);
        }
        KnowledgeLanguage detected = detect(request.query());
        if (request.languageStrategy() == RetrievalLanguageStrategy.AUTO) {
            return new LanguageDecision(detected, RetrievalLanguageSource.DETECTED);
        }
        return new LanguageDecision(documentLanguage(documents), RetrievalLanguageSource.DOCUMENT);
    }

    private KnowledgeLanguage documentLanguage(List<KnowledgeDocumentEntity> documents) {
        if (documents.isEmpty()) return KnowledgeLanguage.OTHER;
        KnowledgeLanguage first = documentLanguage(documents.get(0));
        return documents.stream().allMatch(document -> documentLanguage(document) == first)
                ? first : KnowledgeLanguage.MIXED;
    }

    private KnowledgeLanguage documentLanguage(KnowledgeDocumentEntity document) {
        try {
            return document.getLanguage() == null ? KnowledgeLanguage.OTHER
                    : KnowledgeLanguage.valueOf(document.getLanguage());
        } catch (IllegalArgumentException ignored) {
            return KnowledgeLanguage.OTHER;
        }
    }

    private KnowledgeLanguage detect(String query) {
        long han = query.codePoints().filter(value -> Character.UnicodeScript.of(value)
                == Character.UnicodeScript.HAN).count();
        long latin = query.codePoints().filter(value -> Character.UnicodeScript.of(value)
                == Character.UnicodeScript.LATIN).count();
        if (han > 0 && latin > 0) return KnowledgeLanguage.MIXED;
        if (han > 0) return KnowledgeLanguage.ZH;
        if (latin > 0) return KnowledgeLanguage.EN;
        return KnowledgeLanguage.OTHER;
    }

    private int candidateLimit(RagEmbeddingProfile profile, int topK) {
        int accuracyFirstLimit = Math.max(1, topK) * DEFAULT_CANDIDATE_MULTIPLIER;
        return Math.min(profile.candidateLimit(), Math.max(topK, accuracyFirstLimit));
    }

    private int uniqueCandidateCount(SearchBatch searchBatch) {
        return (int) java.util.stream.Stream.concat(
                        searchBatch.vectorCandidates().stream().map(VectorSearchCandidate::chunkId),
                        searchBatch.lexicalScores().stream().map(RetrievalCandidateScore::chunkId))
                .filter(java.util.Objects::nonNull).distinct().count();
    }

    private double maxScore(List<RetrievalCandidate> candidates,
                            java.util.function.ToDoubleFunction<RetrievalCandidate> score) {
        return candidates.stream().mapToDouble(score).max().orElse(0D);
    }

    private void validateVector(RagEmbeddingProfile profile, List<Float> vector) {
        if (vector == null || vector.isEmpty() || vector.size() != profile.vectorDimension()) {
            int actual = vector == null ? 0 : vector.size();
            throw new IllegalStateException("查询向量维度与 RAG Profile 不一致，期望 "
                    + profile.vectorDimension() + "，实际 " + actual + "。");
        }
    }

    /**
     * 当前 Profile 与活动 Generation 绑定同一向量空间。
     * 调用失败只能返回稳定业务错误，不能临时切换其他 Embedding 查询现有 collection。
     */
    private List<Float> embedQuery(RagEmbeddingProfile profile, String query) {
        try {
            List<Float> vector = embeddingProvider.embedQuery(profile, query);
            validateVector(profile, vector);
            return vector;
        } catch (RagEmbeddingInvocationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            ModelInvocationErrorCategory category = modelErrorClassifier.classify(exception);
            throw new RagEmbeddingInvocationException(category, profile.embeddingSource(),
                    embeddingFailureMessage(profile.embeddingSource(), category), exception);
        }
    }

    private String embeddingFailureMessage(RagModelSource source, ModelInvocationErrorCategory category) {
        String modelName = switch (source) {
            case TENANT_PRIVATE -> "客户配置的 Embedding";
            case PLATFORM_SHARED -> "平台共享 Embedding";
            case LOCAL -> "平台本地 Embedding";
        };
        return switch (category) {
            case AUTHENTICATION_FAILED -> modelName + " 凭证无效，请检查模型中心的访问凭证。";
            case RATE_LIMITED -> modelName + " 已触发限流或配额不足，请检查供应商额度。";
            case TIMEOUT -> modelName + " 响应超时，请检查模型服务状态和网络连接。";
            case DIMENSION_MISMATCH -> modelName + " 返回维度与当前索引不一致，请检查模型配置并重建索引。";
            case INVALID_RESPONSE -> modelName + " 返回了无效响应，请检查模型接口兼容性。";
            case INVALID_CONFIGURATION, CAPABILITY_MISMATCH ->
                    modelName + " 配置或能力不匹配，请检查模型中心配置。";
            default -> modelName + " 当前不可用，请检查模型服务地址和网络连接。";
        };
    }

    private void validate(RagRetrievalRequest request) {
        if (request == null || request.tenantId() == null) {
            throw new IllegalArgumentException("检索租户不能为空。");
        }
        if (request.query() == null || request.query().isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空。");
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String fingerprint(String query) {
        return Integer.toHexString(query.hashCode());
    }

    private record LanguageDecision(KnowledgeLanguage language, RetrievalLanguageSource source) {
    }

    private record SearchBatch(List<VectorSearchCandidate> vectorCandidates,
                               List<RetrievalCandidateScore> lexicalScores,
                               List<RetrievalChannel> channels,
                               List<RetrievalChannelExecution> channelExecutions,
                               long embeddingMs, long vectorSearchMs) {
    }

    private record ChannelRecallResult(List<LexicalSearchCandidate> candidates,
                                       RetrievalChannelExecution execution) {
        private ChannelRecallResult {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }

    private record RerankBatch(List<RetrievalCandidate> candidates, boolean degraded,
                               RerankMode mode, long elapsedMs, BigDecimal estimatedCost,
                               String degradeReason, boolean budgetExceeded) {
        private RerankBatch(List<RetrievalCandidate> candidates, boolean degraded,
                            RerankMode mode, long elapsedMs) {
            this(candidates, degraded, mode, elapsedMs, null, "", false);
        }
    }
}
