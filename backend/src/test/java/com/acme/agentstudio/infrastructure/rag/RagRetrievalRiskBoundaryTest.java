package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.common.exception.RagEmbeddingInvocationException;
import com.acme.agentstudio.domain.knowledge.model.ActiveIndexGeneration;
import com.acme.agentstudio.domain.knowledge.model.ContextAssemblyResult;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.QueryUnderstandingResult;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCalibration;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannel;
import com.acme.agentstudio.domain.knowledge.model.RetrievalChannelExecutionStatus;
import com.acme.agentstudio.domain.knowledge.model.RetrievalHitReason;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchCandidate;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagRetrievalRiskBoundaryTest {

    @Test
    void shouldFailTenantEmbeddingWithoutQueryingAnotherVectorSpace() {
        Fixture fixture = fixture();
        when(fixture.embeddingProvider.embedQuery(any(), anyString()))
                .thenThrow(new CompletionException(new TimeoutException("timeout")));

        assertThatThrownBy(() -> fixture.service.retrieve(request("普通问题")))
                .isInstanceOfSatisfying(RagEmbeddingInvocationException.class, exception -> {
                    assertThat(exception.category()).isEqualTo(ModelInvocationErrorCategory.TIMEOUT);
                    assertThat(exception.modelSource()).isEqualTo(RagModelSource.TENANT_PRIVATE);
                    assertThat(exception.getMessage()).doesNotContain("timeout");
                });

        verify(fixture.embeddingProvider).embedQuery(fixture.profile, "普通问题");
        verifyNoInteractions(fixture.vectorIndexProvider);
    }

    @Test
    void shouldKeepDenseAndExactWhenSparseChannelFails() {
        Fixture fixture = fixture();
        when(fixture.embeddingProvider.embedQuery(any(), anyString())).thenReturn(List.of(0.1F, 0.2F));
        when(fixture.vectorIndexProvider.search(any())).thenReturn(List.of(
                new VectorSearchCandidate("point-100", 10L, 100L, 900L, 1, "安装", 0.8D)));
        when(fixture.vectorIndexProvider.searchSparse(any())).thenThrow(new IllegalStateException("sparse unavailable"));
        when(fixture.vectorIndexProvider.searchExact(any())).thenReturn(List.of(
                new LexicalSearchCandidate("point-100", 10L, 100L, 900L, 1, "安装",
                        0D, 1D, RetrievalHitReason.EXACT_IDENTIFIER)));
        when(fixture.chunkMapper.selectList(any())).thenReturn(List.of(child()));
        when(fixture.scoreCalibrator.calibrate(any(), any())).thenAnswer(invocation -> {
            List<?> values = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate> candidates = (List<com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate>) values;
            return new RetrievalCalibration(candidates, false, 0.9D, 0.3D);
        });
        when(fixture.diversitySelector.select(any(), any(), any(Integer.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(fixture.contextAssemblyService.assemble(any(), any(), any()))
                .thenReturn(new ContextAssemblyResult(List.of(), 0D));

        var outcome = fixture.service.retrieve(request("ERR-42"));

        assertThat(outcome.degraded()).isTrue();
        assertThat(outcome.degradeReason()).isEqualTo("RETRIEVAL_CHANNEL_FAILURE");
        assertThat(outcome.channels()).contains(RetrievalChannel.VECTOR, RetrievalChannel.EXACT);
        assertThat(outcome.debugSummary().channelExecutions())
                .anySatisfy(execution -> {
                    assertThat(execution.channel()).isEqualTo(RetrievalChannel.LEXICAL_EN);
                    assertThat(execution.status()).isEqualTo(RetrievalChannelExecutionStatus.FAILED);
                })
                .anySatisfy(execution -> {
                    assertThat(execution.channel()).isEqualTo(RetrievalChannel.EXACT);
                    assertThat(execution.status()).isEqualTo(RetrievalChannelExecutionStatus.SUCCESS);
                });
        verify(fixture.rerankerProvider, never()).rerank(any(), anyString(), any());
    }

    private Fixture fixture() {
        ModelProfileResolver profileResolver = mock(ModelProfileResolver.class);
        KnowledgeIndexGenerationService generationService = mock(KnowledgeIndexGenerationService.class);
        KnowledgeDocumentVisibilityFilter visibilityFilter = mock(KnowledgeDocumentVisibilityFilter.class);
        QueryUnderstandingService queryUnderstandingService = mock(QueryUnderstandingService.class);
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        VectorIndexProvider vectorIndexProvider = mock(VectorIndexProvider.class);
        RerankerProvider rerankerProvider = mock(RerankerProvider.class);
        RetrievalScoreCalibrator scoreCalibrator = mock(RetrievalScoreCalibrator.class);
        RetrievalDiversitySelector diversitySelector = mock(RetrievalDiversitySelector.class);
        ContextAssemblyService contextAssemblyService = mock(ContextAssemblyService.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        RerankerBudgetGuard budgetGuard = mock(RerankerBudgetGuard.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.id()).thenReturn(2L);
        when(profile.code()).thenReturn("tenant-profile");
        when(profile.embeddingSource()).thenReturn(RagModelSource.TENANT_PRIVATE);
        when(profile.vectorDimension()).thenReturn(2);
        when(profile.candidateLimit()).thenReturn(20);
        when(profile.vectorRecallWeight()).thenReturn(0.6D);
        when(profile.lexicalRecallWeight()).thenReturn(0.4D);
        when(profile.exactRecallBoost()).thenReturn(0.2D);
        when(profile.rerankerEnabled()).thenReturn(false);
        when(profile.maxContextTokens()).thenReturn(100);
        ActiveIndexGeneration generation = new ActiveIndexGeneration(7L, 1L, 2L, "collection-7");
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setId(10L);
        document.setTenantId(1L);
        document.setTitle("安装手册");
        document.setLanguage(KnowledgeLanguage.ZH.name());
        when(profileResolver.resolve(any(), any())).thenReturn(profile);
        when(generationService.requireSearchable(1L, profile)).thenReturn(generation);
        when(visibilityFilter.filter(any(), any(), any())).thenReturn(List.of(document));
        when(queryUnderstandingService.understand(any(), any(), anyString()))
                .thenAnswer(invocation -> new QueryUnderstandingResult(invocation.getArgument(2), "", false));
        RagRetrievalService service = new RagRetrievalService(profileResolver, generationService,
                visibilityFilter, queryUnderstandingService, embeddingProvider, vectorIndexProvider,
                rerankerProvider, scoreCalibrator, diversitySelector, contextAssemblyService, chunkMapper,
                new MultilingualSparseEncoder(), budgetGuard, new ModelInvocationErrorClassifier());
        return new Fixture(service, profile, embeddingProvider, vectorIndexProvider, rerankerProvider,
                scoreCalibrator, diversitySelector, contextAssemblyService, chunkMapper);
    }

    private RagRetrievalRequest request(String query) {
        return new RagRetrievalRequest(1L, 5L, query, RetrievalLanguageStrategy.AUTO, null,
                RetrievalScopeType.VISIBLE_DOCUMENTS, List.of(), 3,
                new RagModelSelection(RagModelSource.TENANT_PRIVATE, 8L, null));
    }

    private KnowledgeChunkEntity child() {
        KnowledgeChunkEntity child = new KnowledgeChunkEntity();
        child.setId(100L);
        child.setDocumentId(10L);
        child.setIndexGenerationId(7L);
        child.setParentChunkId(900L);
        child.setChunkRole("CHILD");
        child.setChunkNo(1);
        child.setChunkText("ERR-42 安装失败");
        child.setSectionPath("安装");
        child.setTokenCount(8);
        child.setContentHash("hash-100");
        return child;
    }

    private record Fixture(RagRetrievalService service,
                           RagEmbeddingProfile profile,
                           EmbeddingProvider embeddingProvider,
                           VectorIndexProvider vectorIndexProvider,
                           RerankerProvider rerankerProvider,
                           RetrievalScoreCalibrator scoreCalibrator,
                           RetrievalDiversitySelector diversitySelector,
                           ContextAssemblyService contextAssemblyService,
                           KnowledgeChunkMapper chunkMapper) {
    }
}
