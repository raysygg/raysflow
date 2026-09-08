package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * RagRetrievalMetric 数据库持久化实体对象。
 * 对应数据库中 RagRetrievalMetric 数据表的字段结构映射。
 */
@TableName("rag_retrieval_metric")
/**
 * 数据库实体：映射表 `rag_retrieval_metric`，记录每次 RAG 检索的命中与 grounded 结果。
 */
/**
 * RagRetrievalMetric 数据表持久化实体类。
 * 映射数据库对应的 RagRetrievalMetric 表结构。
 */
public class RagRetrievalMetricEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** call 业务分类类型 属性 */
    private String callType;
    /** hit Count 属性 */
    private Integer hitCount;
    /** top Score 属性 */
    private Double topScore;
    /** hit 属性 */
    private Boolean hit;
    /** grounded 属性 */
    private Boolean grounded;
    /** actual Language 属性 */
    private String actualLanguage;
    /** language Source 属性 */
    private String languageSource;
    /** retrieval Scope 属性 */
    private String retrievalScope;
    /** retrieval Channels 属性 */
    private String retrievalChannels;
    /** candidate Count 属性 */
    private Integer candidateCount;
    /** embedding Profile 属性 */
    private String embeddingProfile;
    /** index Generation 主键 ID 标识 属性 */
    private Long indexGenerationId;
    /** initial Candidate Count 属性 */
    private Integer initialCandidateCount;
    /** reranked Candidate Count 属性 */
    private Integer rerankedCandidateCount;
    /** score Gap 属性 */
    private Double scoreGap;
    /** query Rewritten 属性 */
    private Boolean queryRewritten;
    /** degraded 属性 */
    private Boolean degraded;
    /** parent Coverage 属性 */
    private Double parentCoverage;
    /** embedding Elapsed Ms 属性 */
    private Long embeddingElapsedMs;
    /** vector Search Elapsed Ms 属性 */
    private Long vectorSearchElapsedMs;
    /** rerank Elapsed Ms 属性 */
    private Long rerankElapsedMs;
    /** context Elapsed Ms 属性 */
    private Long contextElapsedMs;
    /** elapsed Ms 属性 */
    private Long elapsedMs;
    /** model Source 属性 */
    private String modelSource;
    /** degrade Reason 属性 */
    private String degradeReason;
    /** reranker Estimated Cost 属性 */
    private BigDecimal rerankerEstimatedCost;
    /** reranker Budget Exceeded 属性 */
    private Boolean rerankerBudgetExceeded;
    /** dense Candidate Count 属性 */
    private Integer denseCandidateCount;
    /** sparse Candidate Count 属性 */
    private Integer sparseCandidateCount;
    /** exact Candidate Count 属性 */
    private Integer exactCandidateCount;
    /** fusion Candidate Count 属性 */
    private Integer fusionCandidateCount;
    /** 数据创建时间 */
    private LocalDateTime createdAt;

        /**
         * 获取getId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getId() {
        return id;
    }

        /**
         * 设置setId 业务逻辑处理。
         *
         * @param id id 参数
         */
    public void setId(Long id) {
        this.id = id;
    }

        /**
         * 获取getTenantId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getTenantId() {
        return tenantId;
    }

        /**
         * 设置setTenantId 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

        /**
         * 获取getCallType 业务逻辑处理。
         * @return String 返回对象
         */
    public String getCallType() {
        return callType;
    }

        /**
         * 设置setCallType 业务逻辑处理。
         *
         * @param callType callType 参数
         */
    public void setCallType(String callType) {
        this.callType = callType;
    }

        /**
         * 获取getHitCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getHitCount() {
        return hitCount;
    }

        /**
         * 设置setHitCount 业务逻辑处理。
         *
         * @param hitCount hitCount 参数
         */
    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

        /**
         * 获取getTopScore 业务逻辑处理。
         * @return Double 返回对象
         */
    public Double getTopScore() {
        return topScore;
    }

        /**
         * 设置setTopScore 业务逻辑处理。
         *
         * @param topScore topScore 参数
         */
    public void setTopScore(Double topScore) {
        this.topScore = topScore;
    }

        /**
         * 获取getHit 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getHit() {
        return hit;
    }

        /**
         * 设置setHit 业务逻辑处理。
         *
         * @param hit hit 参数
         */
    public void setHit(Boolean hit) {
        this.hit = hit;
    }

        /**
         * 获取getGrounded 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getGrounded() {
        return grounded;
    }

        /**
         * 设置setGrounded 业务逻辑处理。
         *
         * @param grounded grounded 参数
         */
    public void setGrounded(Boolean grounded) {
        this.grounded = grounded;
    }

        /**
         * 获取getActualLanguage 业务逻辑处理。
         * @return String 返回对象
         */
    public String getActualLanguage() { return actualLanguage; }
        /**
         * 设置setActualLanguage 业务逻辑处理。
         *
         * @param actualLanguage actualLanguage 参数
         */
    public void setActualLanguage(String actualLanguage) { this.actualLanguage = actualLanguage; }
        /**
         * 获取getLanguageSource 业务逻辑处理。
         * @return String 返回对象
         */
    public String getLanguageSource() { return languageSource; }
        /**
         * 设置setLanguageSource 业务逻辑处理。
         *
         * @param languageSource languageSource 参数
         */
    public void setLanguageSource(String languageSource) { this.languageSource = languageSource; }
        /**
         * 获取getRetrievalScope 业务逻辑处理。
         * @return String 返回对象
         */
    public String getRetrievalScope() { return retrievalScope; }
        /**
         * 设置setRetrievalScope 业务逻辑处理。
         *
         * @param retrievalScope retrievalScope 参数
         */
    public void setRetrievalScope(String retrievalScope) { this.retrievalScope = retrievalScope; }
        /**
         * 获取getRetrievalChannels 业务逻辑处理。
         * @return String 返回对象
         */
    public String getRetrievalChannels() { return retrievalChannels; }
        /**
         * 设置setRetrievalChannels 业务逻辑处理。
         *
         * @param retrievalChannels retrievalChannels 参数
         */
    public void setRetrievalChannels(String retrievalChannels) { this.retrievalChannels = retrievalChannels; }
        /**
         * 获取getCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getCandidateCount() { return candidateCount; }
        /**
         * 设置setCandidateCount 业务逻辑处理。
         *
         * @param candidateCount candidateCount 参数
         */
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
        /**
         * 获取getEmbeddingProfile 业务逻辑处理。
         * @return String 返回对象
         */
    public String getEmbeddingProfile() { return embeddingProfile; }
        /**
         * 设置setEmbeddingProfile 业务逻辑处理。
         *
         * @param embeddingProfile embeddingProfile 参数
         */
    public void setEmbeddingProfile(String embeddingProfile) { this.embeddingProfile = embeddingProfile; }
        /**
         * 获取getIndexGenerationId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getIndexGenerationId() { return indexGenerationId; }
        /**
         * 设置setIndexGenerationId 业务逻辑处理。
         *
         * @param indexGenerationId indexGenerationId 参数
         */
    public void setIndexGenerationId(Long indexGenerationId) { this.indexGenerationId = indexGenerationId; }
        /**
         * 获取getInitialCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getInitialCandidateCount() { return initialCandidateCount; }
        /**
         * 设置setInitialCandidateCount 业务逻辑处理。
         *
         * @param initialCandidateCount initialCandidateCount 参数
         */
    public void setInitialCandidateCount(Integer initialCandidateCount) { this.initialCandidateCount = initialCandidateCount; }
        /**
         * 获取getRerankedCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getRerankedCandidateCount() { return rerankedCandidateCount; }
        /**
         * 设置setRerankedCandidateCount 业务逻辑处理。
         *
         * @param rerankedCandidateCount rerankedCandidateCount 参数
         */
    public void setRerankedCandidateCount(Integer rerankedCandidateCount) { this.rerankedCandidateCount = rerankedCandidateCount; }
        /**
         * 获取getScoreGap 业务逻辑处理。
         * @return Double 返回对象
         */
    public Double getScoreGap() { return scoreGap; }
        /**
         * 设置setScoreGap 业务逻辑处理。
         *
         * @param scoreGap scoreGap 参数
         */
    public void setScoreGap(Double scoreGap) { this.scoreGap = scoreGap; }
        /**
         * 获取getQueryRewritten 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getQueryRewritten() { return queryRewritten; }
        /**
         * 设置setQueryRewritten 业务逻辑处理。
         *
         * @param queryRewritten queryRewritten 参数
         */
    public void setQueryRewritten(Boolean queryRewritten) { this.queryRewritten = queryRewritten; }
        /**
         * 获取getDegraded 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getDegraded() { return degraded; }
        /**
         * 设置setDegraded 业务逻辑处理。
         *
         * @param degraded degraded 参数
         */
    public void setDegraded(Boolean degraded) { this.degraded = degraded; }
        /**
         * 获取getParentCoverage 业务逻辑处理。
         * @return Double 返回对象
         */
    public Double getParentCoverage() { return parentCoverage; }
        /**
         * 设置setParentCoverage 业务逻辑处理。
         *
         * @param parentCoverage parentCoverage 参数
         */
    public void setParentCoverage(Double parentCoverage) { this.parentCoverage = parentCoverage; }
        /**
         * 获取getEmbeddingElapsedMs 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getEmbeddingElapsedMs() { return embeddingElapsedMs; }
        /**
         * 设置setEmbeddingElapsedMs 业务逻辑处理。
         *
         * @param embeddingElapsedMs embeddingElapsedMs 参数
         */
    public void setEmbeddingElapsedMs(Long embeddingElapsedMs) { this.embeddingElapsedMs = embeddingElapsedMs; }
        /**
         * 获取getVectorSearchElapsedMs 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getVectorSearchElapsedMs() { return vectorSearchElapsedMs; }
        /**
         * 设置setVectorSearchElapsedMs 业务逻辑处理。
         *
         * @param vectorSearchElapsedMs vectorSearchElapsedMs 参数
         */
    public void setVectorSearchElapsedMs(Long vectorSearchElapsedMs) { this.vectorSearchElapsedMs = vectorSearchElapsedMs; }
        /**
         * 获取getRerankElapsedMs 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getRerankElapsedMs() { return rerankElapsedMs; }
        /**
         * 设置setRerankElapsedMs 业务逻辑处理。
         *
         * @param rerankElapsedMs rerankElapsedMs 参数
         */
    public void setRerankElapsedMs(Long rerankElapsedMs) { this.rerankElapsedMs = rerankElapsedMs; }
        /**
         * 获取getContextElapsedMs 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getContextElapsedMs() { return contextElapsedMs; }
        /**
         * 设置setContextElapsedMs 业务逻辑处理。
         *
         * @param contextElapsedMs contextElapsedMs 参数
         */
    public void setContextElapsedMs(Long contextElapsedMs) { this.contextElapsedMs = contextElapsedMs; }
        /**
         * 获取getElapsedMs 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getElapsedMs() { return elapsedMs; }
        /**
         * 设置setElapsedMs 业务逻辑处理。
         *
         * @param elapsedMs elapsedMs 参数
         */
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
        /**
         * 获取getModelSource 业务逻辑处理。
         * @return String 返回对象
         */
    public String getModelSource() { return modelSource; }
        /**
         * 设置setModelSource 业务逻辑处理。
         *
         * @param modelSource modelSource 参数
         */
    public void setModelSource(String modelSource) { this.modelSource = modelSource; }
        /**
         * 获取getDegradeReason 业务逻辑处理。
         * @return String 返回对象
         */
    public String getDegradeReason() { return degradeReason; }
        /**
         * 设置setDegradeReason 业务逻辑处理。
         *
         * @param degradeReason degradeReason 参数
         */
    public void setDegradeReason(String degradeReason) { this.degradeReason = degradeReason; }
        /**
         * 获取getRerankerEstimatedCost 业务逻辑处理。
         * @return BigDecimal 返回对象
         */
    public BigDecimal getRerankerEstimatedCost() { return rerankerEstimatedCost; }
        /**
         * 设置setRerankerEstimatedCost 业务逻辑处理。
         *
         * @param rerankerEstimatedCost rerankerEstimatedCost 参数
         */
    public void setRerankerEstimatedCost(BigDecimal rerankerEstimatedCost) { this.rerankerEstimatedCost = rerankerEstimatedCost; }
        /**
         * 获取getRerankerBudgetExceeded 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getRerankerBudgetExceeded() { return rerankerBudgetExceeded; }
        /**
         * 设置setRerankerBudgetExceeded 业务逻辑处理。
         *
         * @param rerankerBudgetExceeded rerankerBudgetExceeded 参数
         */
    public void setRerankerBudgetExceeded(Boolean rerankerBudgetExceeded) { this.rerankerBudgetExceeded = rerankerBudgetExceeded; }
        /**
         * 获取getDenseCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getDenseCandidateCount() { return denseCandidateCount; }
        /**
         * 设置setDenseCandidateCount 业务逻辑处理。
         *
         * @param denseCandidateCount denseCandidateCount 参数
         */
    public void setDenseCandidateCount(Integer denseCandidateCount) { this.denseCandidateCount = denseCandidateCount; }
        /**
         * 获取getSparseCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getSparseCandidateCount() { return sparseCandidateCount; }
        /**
         * 设置setSparseCandidateCount 业务逻辑处理。
         *
         * @param sparseCandidateCount sparseCandidateCount 参数
         */
    public void setSparseCandidateCount(Integer sparseCandidateCount) { this.sparseCandidateCount = sparseCandidateCount; }
        /**
         * 获取getExactCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getExactCandidateCount() { return exactCandidateCount; }
        /**
         * 设置setExactCandidateCount 业务逻辑处理。
         *
         * @param exactCandidateCount exactCandidateCount 参数
         */
    public void setExactCandidateCount(Integer exactCandidateCount) { this.exactCandidateCount = exactCandidateCount; }
        /**
         * 获取getFusionCandidateCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getFusionCandidateCount() { return fusionCandidateCount; }
        /**
         * 设置setFusionCandidateCount 业务逻辑处理。
         *
         * @param fusionCandidateCount fusionCandidateCount 参数
         */
    public void setFusionCandidateCount(Integer fusionCandidateCount) { this.fusionCandidateCount = fusionCandidateCount; }

        /**
         * 获取getCreatedAt 业务逻辑处理。
         * @return LocalDateTime 返回对象
         */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

        /**
         * 设置setCreatedAt 业务逻辑处理。
         *
         * @param createdAt createdAt 参数
         */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
