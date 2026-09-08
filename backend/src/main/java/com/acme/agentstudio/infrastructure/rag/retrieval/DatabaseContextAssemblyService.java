package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.ContextAssemblyResult;
import com.acme.agentstudio.domain.knowledge.model.ContextSkipReason;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannel;
import com.acme.agentstudio.domain.knowledge.model.RetrievalHitReason;
import com.acme.agentstudio.domain.knowledge.model.SkippedContext;
import com.acme.agentstudio.domain.knowledge.port.ContextAssemblyService;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DatabaseContextAssembly 业务服务接口。
 * 定义 DatabaseContextAssembly 相关的核心业务契约与流程接口。
 */
@Component
/**
 * DatabaseContextAssembly 业务逻辑服务接口。
 * 负责 DatabaseContextAssembly 核心业务逻辑与流程编排。
 */
public class DatabaseContextAssemblyService implements ContextAssemblyService {
    private static final String PARENT_ROLE = "PARENT";
    private static final String DOCUMENT_PREFIX = "文档：";
    private static final String SECTION_PREFIX = "章节：";

    private final KnowledgeChunkMapper chunkMapper;

    public DatabaseContextAssemblyService(KnowledgeChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    @Override
    public ContextAssemblyResult assemble(RagEmbeddingProfile profile, Long indexGenerationId,
                                          List<RetrievalCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return new ContextAssemblyResult(List.of(), 0D);
        Map<Long, KnowledgeChunkEntity> parents = loadParents(indexGenerationId, candidates);
        List<RagSearchResult> results = new ArrayList<>();
        List<SkippedContext> skippedContexts = new ArrayList<>();
        int usedTokens = 0;
        int parentHits = 0;
        Map<ContextGroupKey, List<RetrievalCandidate>> groupedCandidates = groupByParent(candidates);
        for (List<RetrievalCandidate> group : groupedCandidates.values()) {
            RetrievalCandidate representative = group.get(0);
            KnowledgeChunkEntity parent = parents.get(representative.parentChunkId());
            String body = parent == null ? representative.text() : parent.getChunkText();
            int tokenCount = parent == null ? representative.tokenCount() : safeTokens(parent);
            List<Integer> chunkNumbers = chunkNumbers(group);
            if (body == null || body.isBlank()) {
                skippedContexts.add(skipped(representative, chunkNumbers, ContextSkipReason.EMPTY_CONTENT));
                continue;
            }
            if (usedTokens + tokenCount > profile.maxContextTokens()) {
                skippedContexts.add(skipped(representative, chunkNumbers, ContextSkipReason.TOKEN_BUDGET));
                continue;
            }
            if (parent != null) {
                parentHits++;
            }
            usedTokens += tokenCount;
            int chunkStart = chunkNumbers.isEmpty() ? 0 : chunkNumbers.get(0);
            int chunkEnd = chunkNumbers.isEmpty() ? 0 : chunkNumbers.get(chunkNumbers.size() - 1);
            List<RetrievalChannel> channels = mergedChannels(group);
            List<RetrievalHitReason> hitReasons = mergedHitReasons(group);
            String text = DOCUMENT_PREFIX + representative.source() + "\n"
                    + SECTION_PREFIX + representative.sectionPath() + "\n\n" + body;
            results.add(new RagSearchResult(text, maximumScore(group), representative.source(),
                    representative.documentId(), chunkStart, chunkEnd, chunkNumbers,
                    mergedLanguage(group), primaryChannel(channels), channels, hitReasons));
        }
        double coverage = groupedCandidates.isEmpty() ? 0D : (double) parentHits / groupedCandidates.size();
        return new ContextAssemblyResult(results, coverage, skippedContexts);
    }

    /** 同一父块的多个子块只形成一份正文，仍保留所有命中位置和解释信息。 */
    private Map<ContextGroupKey, List<RetrievalCandidate>> groupByParent(List<RetrievalCandidate> candidates) {
        Map<ContextGroupKey, List<RetrievalCandidate>> grouped = new LinkedHashMap<>();
        for (RetrievalCandidate candidate : candidates) {
            ContextGroupKey key = candidate.parentChunkId() == null
                    ? new ContextGroupKey(false, candidate.chunkId())
                    : new ContextGroupKey(true, candidate.parentChunkId());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
        }
        return grouped;
    }

    private List<Integer> chunkNumbers(List<RetrievalCandidate> candidates) {
        return candidates.stream().map(RetrievalCandidate::chunkNo).filter(Objects::nonNull)
                .filter(chunkNo -> chunkNo > 0).distinct().sorted().toList();
    }

    private List<RetrievalChannel> mergedChannels(List<RetrievalCandidate> candidates) {
        LinkedHashSet<RetrievalChannel> channels = new LinkedHashSet<>();
        candidates.stream().flatMap(candidate -> candidate.channels().stream()).forEach(channels::add);
        return List.copyOf(channels);
    }

    private List<RetrievalHitReason> mergedHitReasons(List<RetrievalCandidate> candidates) {
        LinkedHashSet<RetrievalHitReason> reasons = new LinkedHashSet<>();
        candidates.stream().flatMap(candidate -> candidate.hitReasons().stream()).forEach(reasons::add);
        return List.copyOf(reasons);
    }

    private KnowledgeLanguage mergedLanguage(List<RetrievalCandidate> candidates) {
        KnowledgeLanguage first = candidates.get(0).language();
        return candidates.stream().allMatch(candidate -> candidate.language() == first)
                ? first : KnowledgeLanguage.MIXED;
    }

    private RetrievalChannel primaryChannel(List<RetrievalChannel> channels) {
        if (channels.contains(RetrievalChannel.EXACT)) return RetrievalChannel.EXACT;
        return channels.isEmpty() ? RetrievalChannel.VECTOR : channels.get(0);
    }

    private double maximumScore(List<RetrievalCandidate> candidates) {
        return candidates.stream().max(Comparator.comparingDouble(RetrievalCandidate::relevanceScore))
                .map(RetrievalCandidate::relevanceScore).orElse(0D);
    }

    private SkippedContext skipped(RetrievalCandidate candidate, List<Integer> chunkNumbers,
                                   ContextSkipReason reason) {
        return new SkippedContext(candidate.documentId(), candidate.parentChunkId(), chunkNumbers, reason);
    }

    private Map<Long, KnowledgeChunkEntity> loadParents(Long generationId, List<RetrievalCandidate> candidates) {
        List<Long> parentIds = candidates.stream().map(RetrievalCandidate::parentChunkId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        if (parentIds.isEmpty()) return Map.of();
        List<KnowledgeChunkEntity> entities = chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getIndexGenerationId, generationId)
                .eq(KnowledgeChunkEntity::getChunkRole, PARENT_ROLE)
                .in(KnowledgeChunkEntity::getId, parentIds));
        Map<Long, KnowledgeChunkEntity> result = new LinkedHashMap<>();
        entities.forEach(entity -> result.put(entity.getId(), entity));
        return result;
    }

    private int safeTokens(KnowledgeChunkEntity chunk) {
        return chunk.getTokenCount() == null ? 0 : Math.max(0, chunk.getTokenCount());
    }

    private record ContextGroupKey(boolean parent, Long id) {
    }
}
