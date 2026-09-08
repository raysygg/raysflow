package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.IndexGenerationStatus;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.VectorCollectionInspection;
import com.acme.agentstudio.domain.knowledge.port.VectorIndexProvider;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeIndexGenerationEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentIndexStateMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeIndexGenerationMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeIndexGenerationServiceTest {
    @BeforeAll
    static void initializeMybatisPlusMetadata() {
        // 纯 Mockito 测试没有 Spring 上下文，需要显式注册实体元数据才能解析 Lambda 字段引用。
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, KnowledgeIndexGenerationEntity.class);
    }

    @Test
    void shouldKeepFullFingerprintButUseBoundedPhysicalCollectionName() {
        KnowledgeIndexGenerationMapper generationMapper = mock(KnowledgeIndexGenerationMapper.class);
        KnowledgeDocumentIndexStateMapper stateMapper = mock(KnowledgeDocumentIndexStateMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        VectorIndexProvider vectorProvider = mock(VectorIndexProvider.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.id()).thenReturn(2L);
        when(profile.versionNo()).thenReturn(1);
        when(profile.vectorDimension()).thenReturn(768);
        when(profile.embeddingModelKey()).thenReturn("customer-multilingual-embedding");
        doAnswer(invocation -> {
            KnowledgeIndexGenerationEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        }).when(generationMapper).insert(any(KnowledgeIndexGenerationEntity.class));
        KnowledgeIndexGenerationService service = new KnowledgeIndexGenerationService(
                generationMapper, stateMapper, chunkMapper, vectorProvider, new RagProperties());

        service.beginRebuild(123456L, profile);

        ArgumentCaptor<KnowledgeIndexGenerationEntity> captor =
                ArgumentCaptor.forClass(KnowledgeIndexGenerationEntity.class);
        verify(generationMapper).insert(captor.capture());
        assertThat(captor.getValue().getEmbeddingFingerprint()).hasSize(64);
        assertThat(captor.getValue().getCollectionName()).hasSizeLessThanOrEqualTo(64);
    }

    @Test
    void shouldFailBuildingGenerationWhenVectorDimensionMismatch() {
        KnowledgeIndexGenerationMapper generationMapper = mock(KnowledgeIndexGenerationMapper.class);
        KnowledgeDocumentIndexStateMapper stateMapper = mock(KnowledgeDocumentIndexStateMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        VectorIndexProvider vectorProvider = mock(VectorIndexProvider.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.id()).thenReturn(2L);
        when(profile.vectorDimension()).thenReturn(768);
        KnowledgeIndexGenerationEntity building = new KnowledgeIndexGenerationEntity();
        building.setId(9L);
        building.setTenantId(1L);
        building.setProfileId(2L);
        building.setCollectionName("test_collection");
        building.setGenerationStatus(IndexGenerationStatus.BUILDING.name());
        building.setIndexedChunks(3);
        when(generationMapper.selectOne(any())).thenReturn(building);
        when(vectorProvider.inspectCollection(eq("test_collection"), eq(profile)))
                .thenReturn(new VectorCollectionInspection(true, 384, 3, true, true));
        KnowledgeIndexGenerationService service = new KnowledgeIndexGenerationService(
                generationMapper, stateMapper, chunkMapper, vectorProvider, new RagProperties());

        assertThatThrownBy(() -> service.validateAndActivate(1L, profile, 9L, 1, 3))
                .isInstanceOf(IllegalStateException.class);
        verify(generationMapper).update(eq(null), any());
    }

    @Test
    void shouldKeepActiveDocumentPointersWhenNewCollectionCreationFails() {
        KnowledgeIndexGenerationMapper generationMapper = mock(KnowledgeIndexGenerationMapper.class);
        KnowledgeDocumentIndexStateMapper stateMapper = mock(KnowledgeDocumentIndexStateMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        VectorIndexProvider vectorProvider = mock(VectorIndexProvider.class);
        RagEmbeddingProfile profile = mock(RagEmbeddingProfile.class);
        when(profile.id()).thenReturn(2L);
        when(profile.versionNo()).thenReturn(1);
        when(profile.vectorDimension()).thenReturn(768);
        when(profile.embeddingModelKey()).thenReturn("customer-multilingual-embedding");
        doAnswer(invocation -> {
            KnowledgeIndexGenerationEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return 1;
        }).when(generationMapper).insert(any(KnowledgeIndexGenerationEntity.class));
        doThrow(new IllegalStateException("模拟 Qdrant Gridstore 创建失败"))
                .when(vectorProvider).ensureCollection(any(String.class), eq(profile));
        KnowledgeIndexGenerationService service = new KnowledgeIndexGenerationService(
                generationMapper, stateMapper, chunkMapper, vectorProvider, new RagProperties());

        assertThatThrownBy(() -> service.beginRebuild(123456L, profile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gridstore");

        // 创建失败只把本次 BUILDING Generation 标为 FAILED，旧文档活动指针保持不变。
        verify(generationMapper, times(1)).update(eq(null), any());
        verifyNoInteractions(stateMapper);
    }
}
