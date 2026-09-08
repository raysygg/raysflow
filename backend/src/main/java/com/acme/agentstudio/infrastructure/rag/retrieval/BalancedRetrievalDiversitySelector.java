package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import com.acme.agentstudio.domain.knowledge.port.RetrievalDiversitySelector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 均衡打散与多样性选择器实现类（Balanced Retrieval Diversity Selector）。
 * 防止单个父切块或单文档垄断 Top-K 结果。
 */
@Component
public class BalancedRetrievalDiversitySelector implements RetrievalDiversitySelector {

        /**
         * select 方法。
         *
         * @param profile profile 参数
         * @param candidates candidates 参数
         * @param topK topK 参数
         * @return List<RetrievalCandidate> 返回对象
         */
    @Override
    public List<RetrievalCandidate> select(RagEmbeddingProfile profile, List<RetrievalCandidate> candidates, int topK) {
        int resultLimit = Math.min(Math.max(1, topK), profile.maxParentChunks());
        List<RetrievalCandidate> ranked = candidates.stream()
                .sorted(Comparator.comparingDouble(RetrievalCandidate::relevanceScore).reversed())
                .toList();
        List<RetrievalCandidate> selected = new ArrayList<>();
        Map<Long, Integer> documentCounts = new HashMap<>();
        Map<Long, Integer> parentCounts = new HashMap<>();
        Set<String> contentHashes = new HashSet<>();
        Set<Long> documentIds = new HashSet<>();
        for (RetrievalCandidate candidate : ranked) {
            if (!accept(candidate, profile, documentCounts, parentCounts, contentHashes, documentIds)) continue;
            selected.add(candidate);
            documentCounts.merge(candidate.documentId(), 1, Integer::sum);
            documentIds.add(candidate.documentId());
            if (candidate.parentChunkId() != null) parentCounts.merge(candidate.parentChunkId(), 1, Integer::sum);
            if (!candidate.contentHash().isBlank()) contentHashes.add(candidate.contentHash());
            if (selected.size() >= resultLimit) break;
        }
        return List.copyOf(selected);
    }

    private boolean accept(RetrievalCandidate candidate, RagEmbeddingProfile profile,
                           Map<Long, Integer> documentCounts, Map<Long, Integer> parentCounts,
                           Set<String> contentHashes, Set<Long> documentIds) {
        if (candidate == null || candidate.documentId() == null || candidate.text().isBlank()) return false;
        if (candidate.parentChunkId() != null
                && parentCounts.getOrDefault(candidate.parentChunkId(), 0) >= profile.perDocumentContextLimit()) return false;
        if (!candidate.contentHash().isBlank() && contentHashes.contains(candidate.contentHash())) return false;
        if (!documentIds.contains(candidate.documentId()) && documentIds.size() >= profile.maxDocuments()) return false;
        return documentCounts.getOrDefault(candidate.documentId(), 0) < profile.perDocumentContextLimit();
    }
}
