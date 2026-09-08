package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;
import java.util.Set;

/**
 * LexicalSearch 请求数据传输对象 (DTO)。
 * 封装前端或外部传入的 LexicalSearch 操作参数。
 */
/** Qdrant Sparse/Exact 查询合同，沿用 Dense 查询的租户、Generation 和文档范围。 */
public record LexicalSearchRequest(
        Long tenantId,
        Long indexGenerationId,
        String collectionName,
        SparseVectorData queryVector,
        List<String> exactKeys,
        Set<Long> documentIds,
        int limit
) {
    public LexicalSearchRequest {
        queryVector = queryVector == null ? SparseVectorData.empty() : queryVector;
        exactKeys = exactKeys == null ? List.of() : List.copyOf(exactKeys);
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        limit = Math.max(1, limit);
    }
}
