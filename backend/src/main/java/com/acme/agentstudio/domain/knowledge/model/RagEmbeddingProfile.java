package com.acme.agentstudio.domain.knowledge.model;

/**
 * 租户在线生效的 RAG 向量索引与多路召回重排模型全量配置 Profile 实体 Record（Rag Embedding Profile）。
 * 包含物理配置 ID id、租户物理 ID tenantId、标识编码 code、配置名称 name、模型来源 embeddingSource、
 * 向量模型 ID/Key、重排模型 Key、改写模型 Key、向量维度 vectorDimension、距离度量 distanceMetric、
 * 各阶段召回与截断 Top-K、重排与无命中分数阈值、Token 预算上限、多语言策略及降级保护策略等。
 */
public record RagEmbeddingProfile(
        Long id,
        Long tenantId,
        String code,
        String name,
        RagModelSource embeddingSource,
        Long embeddingModelId,
        String embeddingModelKey,
        String rerankerModelKey,
        String queryRewriteModelKey,
        int vectorDimension,
        String distanceMetric,
        int candidateLimit,
        double rerankThreshold,
        double noHitThreshold,
        double scoreGapThreshold,
        int maxContextTokens,
        int maxDocuments,
        int maxParentChunks,
        int perDocumentContextLimit,
        boolean queryRewriteEnabled,
        boolean lateInteractionEnabled,
        RetrievalDegradePolicy degradePolicy,
        int versionNo,
        String supportedLanguages,
        String supportedScripts,
        boolean crossLanguageEnabled,
        String modelVersion,
        String queryInstruction,
        String documentInstruction,
        boolean rerankerEnabled,
        int rerankerCandidateLimit,
        int rerankerTimeoutSeconds,
        double rerankerBudget,
        double vectorRecallWeight,
        double lexicalRecallWeight,
        double exactRecallBoost
) {
    /** 紧凑构造函数进行极值校准与默认值兜底设置 */
    public RagEmbeddingProfile {
        embeddingSource = (embeddingSource == null) ? RagModelSource.LOCAL : embeddingSource;
        candidateLimit = Math.max(1, candidateLimit);
        maxContextTokens = Math.max(1, maxContextTokens);
        maxDocuments = Math.max(1, maxDocuments);
        maxParentChunks = Math.max(1, maxParentChunks);
        perDocumentContextLimit = Math.max(1, perDocumentContextLimit);
        supportedLanguages = (supportedLanguages == null || supportedLanguages.isBlank()) ? "ZH,EN,OTHER" : supportedLanguages;
        supportedScripts = (supportedScripts == null || supportedScripts.isBlank()) ? "HAN,LATIN,OTHER" : supportedScripts;
        modelVersion = (modelVersion == null) ? "" : modelVersion;
        queryInstruction = (queryInstruction == null) ? "" : queryInstruction;
        documentInstruction = (documentInstruction == null) ? "" : documentInstruction;
        rerankerCandidateLimit = Math.max(1, rerankerCandidateLimit);
        rerankerTimeoutSeconds = Math.max(1, rerankerTimeoutSeconds);
        rerankerBudget = Math.max(0D, rerankerBudget);
        vectorRecallWeight = Math.max(0D, vectorRecallWeight);
        lexicalRecallWeight = Math.max(0D, lexicalRecallWeight);
        exactRecallBoost = Math.max(0D, exactRecallBoost);
        degradePolicy = (degradePolicy == null) ? RetrievalDegradePolicy.FAIL_OPEN : normalizePolicy(degradePolicy);
    }

    /** 归一化降级策略，将已废弃的枚举映射到标准枚举 */
    private static RetrievalDegradePolicy normalizePolicy(RetrievalDegradePolicy policy) {
        if (policy == RetrievalDegradePolicy.VECTOR_ONLY) {
            return RetrievalDegradePolicy.FAIL_OPEN;
        }
        if (policy == RetrievalDegradePolicy.FAIL) {
            return RetrievalDegradePolicy.FAIL_CLOSED;
        }
        return policy;
    }
}

