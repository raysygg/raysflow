package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.ContextSkipReason;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannel;
import com.acme.agentstudio.domain.knowledge.model.RetrievalHitReason;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseContextAssemblyServiceTest {

    @Test
    void shouldAssembleParentOnceAndMergeChildReferences() {
        KnowledgeChunkMapper mapper = mock(KnowledgeChunkMapper.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.maxContextTokens()).thenReturn(100);
        when(mapper.selectList(any())).thenReturn(List.of(parent(900L, 20)));
        DatabaseContextAssemblyService service = new DatabaseContextAssemblyService(mapper);

        var result = service.assemble(profile, 7L, List.of(
                candidate(101L, 900L, 1, RetrievalChannel.VECTOR,
                        RetrievalHitReason.VECTOR_SIMILARITY),
                candidate(102L, 900L, 2, RetrievalChannel.EXACT,
                        RetrievalHitReason.EXACT_IDENTIFIER)));

        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).chunkNumbers()).containsExactly(1, 2);
        assertThat(result.results().get(0).channels())
                .containsExactly(RetrievalChannel.VECTOR, RetrievalChannel.EXACT);
        assertThat(result.results().get(0).hitReasons())
                .containsExactly(RetrievalHitReason.VECTOR_SIMILARITY, RetrievalHitReason.EXACT_IDENTIFIER);
        assertThat(result.parentCoverage()).isEqualTo(1D);
        assertThat(result.skippedContexts()).isEmpty();
    }

    @Test
    void shouldReturnTypedReasonWhenParentExceedsTokenBudget() {
        KnowledgeChunkMapper mapper = mock(KnowledgeChunkMapper.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.maxContextTokens()).thenReturn(5);
        when(mapper.selectList(any())).thenReturn(List.of(parent(900L, 20)));
        DatabaseContextAssemblyService service = new DatabaseContextAssemblyService(mapper);

        var result = service.assemble(profile, 7L, List.of(
                candidate(101L, 900L, 1, RetrievalChannel.EXACT,
                        RetrievalHitReason.EXACT_IDENTIFIER)));

        assertThat(result.results()).isEmpty();
        assertThat(result.skippedContexts()).singleElement()
                .satisfies(skipped -> assertThat(skipped.reason()).isEqualTo(ContextSkipReason.TOKEN_BUDGET));
    }

    private RetrievalCandidate candidate(Long chunkId, Long parentId, int chunkNo,
                                         RetrievalChannel channel, RetrievalHitReason reason) {
        return new RetrievalCandidate(chunkId, 10L, parentId, chunkNo, "安装", "子块", "手册",
                KnowledgeLanguage.ZH, "hash-" + chunkId, 5, 0.8D, 0.8D,
                0.8D, 0D, channel == RetrievalChannel.EXACT ? 1D : 0D,
                0.8D, 0D, List.of(channel), List.of(reason));
    }

    private KnowledgeChunkEntity parent(Long id, int tokenCount) {
        KnowledgeChunkEntity parent = new KnowledgeChunkEntity();
        parent.setId(id);
        parent.setChunkRole("PARENT");
        parent.setChunkText("这是只应输出一次的父块正文。");
        parent.setTokenCount(tokenCount);
        return parent;
    }
}
