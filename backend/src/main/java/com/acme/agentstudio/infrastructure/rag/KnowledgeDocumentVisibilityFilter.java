package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentIndexStateEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentIndexStateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * KnowledgeDocumentVisibility HTTP 请求过滤器。
 * 负责请求链路的鉴权、日志追踪与安全拦截。
 */
/** 检索前唯一的文档范围过滤器，未来组织权限只在这里扩展。 */
@Component
public class KnowledgeDocumentVisibilityFilter {
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeDocumentIndexStateMapper indexStateMapper;

    public KnowledgeDocumentVisibilityFilter(KnowledgeDocumentMapper documentMapper,
                                             KnowledgeDocumentIndexStateMapper indexStateMapper) {
        this.documentMapper = documentMapper;
        this.indexStateMapper = indexStateMapper;
    }

        /**
         * filter 方法。
         *
         * @param request request 参数
         * @param profileId profileId 参数
         * @param generationId generationId 参数
         * @return List<KnowledgeDocumentEntity> 返回对象
         */
    public List<KnowledgeDocumentEntity> filter(RagRetrievalRequest request, Long profileId, Long generationId) {
        if (request.scope() == RetrievalScopeType.EXPLICIT_DOCUMENTS) {
            return explicitDocuments(request, profileId, generationId);
        }
        return visibleDocuments(request);
    }

    private List<KnowledgeDocumentEntity> visibleDocuments(RagRetrievalRequest request) {
        return documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getTenantId, request.tenantId())
                .eq(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.INDEXED));
    }

    private List<KnowledgeDocumentEntity> explicitDocuments(RagRetrievalRequest request,
                                                            Long profileId, Long generationId) {
        if (request.documentIds().isEmpty()) {
            throw new IllegalArgumentException("指定文档范围不能为空，请至少选择一个已完成索引的文档。");
        }
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getTenantId, request.tenantId())
                .eq(KnowledgeDocumentEntity::getDocumentStatus, BusinessStatus.INDEXED)
                .in(KnowledgeDocumentEntity::getId, request.documentIds()));
        if (documents.size() != request.documentIds().stream().distinct().count()) {
            throw new IllegalArgumentException("指定文档不存在、尚未完成索引或不属于当前租户。");
        }
        validateModelIndex(request, profileId, generationId);
        return documents;
    }

    /** 指定文档必须已经写入当前模型 Profile 和索引版本，避免跨向量空间查询后被误判为普通无命中。 */
    private void validateModelIndex(RagRetrievalRequest request, Long profileId, Long generationId) {
        long expected = request.documentIds().stream().distinct().count();
        long actual = indexStateMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocumentIndexStateEntity>()
                        .eq(KnowledgeDocumentIndexStateEntity::getTenantId, request.tenantId())
                        .eq(KnowledgeDocumentIndexStateEntity::getProfileId, profileId)
                        .eq(KnowledgeDocumentIndexStateEntity::getActiveGenerationId, generationId)
                        .eq(KnowledgeDocumentIndexStateEntity::getIndexStatus, BusinessStatus.INDEXED)
                        .in(KnowledgeDocumentIndexStateEntity::getDocumentId, request.documentIds()));
        if (actual != expected) {
            throw new IllegalArgumentException("指定文档尚未使用当前 Embedding 模型建立索引，请先重建这些文档。");
        }
    }
}
