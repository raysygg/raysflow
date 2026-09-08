package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;
import java.util.Set;

/**
 * VectorSearch 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 VectorSearch 操作参数。
 */
/** 向量索引查询合同，权限范围在进入基础设施前已经解析完成。 */
public record VectorSearchRequest(
        Long tenantId,
        Long indexGenerationId,
        String collectionName,
        List<Float> queryVector,
        Set<Long> documentIds,
        int limit
) {
    public VectorSearchRequest {
        queryVector = queryVector == null ? List.of() : List.copyOf(queryVector);
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        limit = Math.max(1, limit);
    }
}

