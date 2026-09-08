package com.acme.agentstudio.domain.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.List;

/**
 * RAG 混合检索最终收敛产出的结果与过程度量明细实体 Record（RAG Retrieval Outcome）。
 * 包含最终选定的 RAG 结果列表 results (List&lt;RagSearchResult&gt;)、识别语种 actualLanguage、
 * 语种来源 languageSource、作用域 scope、触发渠道 channels、初筛/重排/命中候选数、
 * 得分 topScore/scoreGap、是否改写 queryRewritten、是否降级 degraded、未命中 noHit、
 * 重排模式 rerankMode、向量 Profile embeddingProfile、Generation ID indexGenerationId、
 * 父段落覆盖比率 parentCoverage、各阶段耗时 stageTimings、总耗时 elapsedMs、模型来源 modelSource、
 * 降级原因 degradeReason、Reranker 预估成本 rerankerEstimatedCost、预算超限标记 rerankerBudgetExceeded
 * 以及完整的调优调试摘要 debugSummary (RagRetrievalDebugSummary)。
 *
 * @param results 最终组合选取的 RAG 切片结果
 * @param actualLanguage 实际检测或设定的语种
 * @param languageSource 语种推导来源分类
 * @param scope 检索权限与作用域类型
 * @param channels 生效的物理检索管道列表
 * @param initialCandidateCount 初始召回去重后的候选总数
 * @param rerankedCandidateCount 进入 Reranker 重排的候选数量
 * @param hitCount 最终有效命中的切片数
 * @param topScore 命中切片的最高相关度分
 * @param scoreGap 最高分与最低分的分差
 * @param queryRewritten 是否对 Query 进行了改写扩展
 * @param degraded 是否触发了降级策略
 * @param noHit 是否未能检索命中任何相关结果
 * @param rerankMode 生效的 Reranker 模式
 * @param embeddingProfile 向量 Profile 编码
 * @param indexGenerationId 向量 Gen ID
 * @param parentCoverage 父段落上下文覆盖率
 * @param stageTimings 各阶段微秒耗时统计
 * @param elapsedMs 全链路总耗时（毫秒）
 * @param modelSource 模型来源
 * @param degradeReason 降级触发原因
 * @param rerankerEstimatedCost Reranker 模型消费预估金额
 * @param rerankerBudgetExceeded Reranker 预算是否超限
 * @param debugSummary 调优调试全景摘要
 */
public record RagRetrievalOutcome(
        List<RagSearchResult> results,
        KnowledgeLanguage actualLanguage,
        RetrievalLanguageSource languageSource,
        RetrievalScopeType scope,
        List<RetrievalChannel> channels,
        int initialCandidateCount,
        int rerankedCandidateCount,
        int hitCount,
        double topScore,
        double scoreGap,
        boolean queryRewritten,
        boolean degraded,
        boolean noHit,
        RerankMode rerankMode,
        @JsonIgnore String embeddingProfile,
        @JsonIgnore Long indexGenerationId,
        double parentCoverage,
        RagStageTimings stageTimings,
        long elapsedMs,
        String modelSource,
        String degradeReason,
        BigDecimal rerankerEstimatedCost,
        boolean rerankerBudgetExceeded,
        RagRetrievalDebugSummary debugSummary
) {
    /**
     * 基础构造函数，默认不提供运维与调试明细元数据。
     *
     * @param results 检索结果
     * @param actualLanguage 实际语种
     * @param languageSource 语种来源
     * @param scope 作用域
     * @param channels 触发渠道
     * @param initialCandidateCount 初始候选数
     * @param rerankedCandidateCount 重排候选数
     * @param hitCount 命中数
     * @param topScore 最高分
     * @param scoreGap 分数差
     * @param queryRewritten 是否改写
     * @param degraded 是否降级
     * @param noHit 是否未命中
     * @param rerankMode 重排模式
     * @param embeddingProfile 向量 Profile
     * @param indexGenerationId 索引 Gen ID
     * @param parentCoverage 父段落覆盖率
     * @param stageTimings 阶段耗时
     * @param elapsedMs 总耗时
     */
    public RagRetrievalOutcome(
            List<RagSearchResult> results,
            KnowledgeLanguage actualLanguage,
            RetrievalLanguageSource languageSource,
            RetrievalScopeType scope,
            List<RetrievalChannel> channels,
            int initialCandidateCount,
            int rerankedCandidateCount,
            int hitCount,
            double topScore,
            double scoreGap,
            boolean queryRewritten,
            boolean degraded,
            boolean noHit,
            RerankMode rerankMode,
            String embeddingProfile,
            Long indexGenerationId,
            double parentCoverage,
            RagStageTimings stageTimings,
            long elapsedMs
    ) {
        this(results, actualLanguage, languageSource, scope, channels, initialCandidateCount,
                rerankedCandidateCount, hitCount, topScore, scoreGap, queryRewritten, degraded, noHit,
                rerankMode, embeddingProfile, indexGenerationId, parentCoverage, stageTimings, elapsedMs,
                "", (degraded ? "RERANKER_FAILURE" : ""), null, false, null);
    }

    /** 紧凑构造函数做输入防空与数值范围收敛 */
    public RagRetrievalOutcome {
        results = (results == null) ? List.of() : List.copyOf(results);
        channels = (channels == null) ? List.of() : List.copyOf(channels);
        actualLanguage = (actualLanguage == null) ? KnowledgeLanguage.OTHER : actualLanguage;
        languageSource = (languageSource == null) ? RetrievalLanguageSource.FALLBACK : languageSource;
        rerankMode = (rerankMode == null) ? RerankMode.STANDARD_HYBRID : rerankMode;
        embeddingProfile = (embeddingProfile == null) ? "" : embeddingProfile;
        modelSource = (modelSource == null) ? "" : modelSource;
        degradeReason = (degradeReason == null) ? "" : degradeReason;
        stageTimings = (stageTimings == null) ? RagStageTimings.empty() : stageTimings;
        parentCoverage = Math.max(0D, Math.min(1D, parentCoverage));
    }

    /**
     * 获取初始召回候选数量。
     *
     * @return 初始候选数量
     */
    public int candidateCount() {
        return initialCandidateCount;
    }

    /**
     * 附加上报运维与调优调试元数据，生成新的 Outcome 对象。
     *
     * @param source 模型来源
     * @param reason 降级原因
     * @param cost 预估重排成本
     * @param budgetExceeded 预算是否超限
     * @param debug 调试全景摘要
     * @return 带有运维数据的 RagRetrievalOutcome 实例
     */
    public RagRetrievalOutcome withOperationalMetadata(
            String source,
            String reason,
            BigDecimal cost,
            boolean budgetExceeded,
            RagRetrievalDebugSummary debug
    ) {
        return new RagRetrievalOutcome(results, actualLanguage, languageSource, scope, channels,
                initialCandidateCount, rerankedCandidateCount, hitCount, topScore, scoreGap, queryRewritten,
                degraded, noHit, rerankMode, embeddingProfile, indexGenerationId, parentCoverage, stageTimings,
                elapsedMs, source, reason, cost, budgetExceeded, debug);
    }
}

