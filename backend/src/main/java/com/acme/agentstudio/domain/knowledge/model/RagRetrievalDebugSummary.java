package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * RAG 多路混合检索调试全景明细与性能开销摘要 Record（RAG Retrieval Debug Summary）。
 * 包含模型来源说明 modelSource、检测实际语种 actualLanguage (KnowledgeLanguage)、
 * 启用的检索渠道 channels、稠密向量/稀疏向量/精确/融合候选数量、重排模式 rerankMode (RerankMode)、
 * 是否降级 degraded、最高得分 topScore、分数差 scoreGap、打分层摘要 scoreLayers (RagScoreLayerSummary)、
 * 各阶段耗时 timings (RagStageTimings)、引用文档路径 referenceLocations、多渠道执行耗时 channelExecutions
 * 以及被跳过的上下文节点 skippedContexts。
 *
 * @param modelSource 模型来源说明
 * @param actualLanguage 识别到的实际语种
 * @param channels 触发的物理检索渠道列表
 * @param denseCandidateCount 稠密向量召回候选数
 * @param sparseCandidateCount 词法/BM25 向量召回候选数
 * @param exactCandidateCount 关键字精确匹配候选数
 * @param fusionCandidateCount 融合去重后的候选总数
 * @param rerankMode 重排器生效模式（RerankMode）
 * @param degraded 检索流程中是否发生了自动降级
 * @param topScore 最终最高匹配得分
 * @param scoreGap 最高分与最低分的分数梯度差
 * @param scoreLayers 打分分布统计摘要（RagScoreLayerSummary）
 * @param timings 各阶段微秒/毫秒耗时分布（RagStageTimings）
 * @param referenceLocations 命中引用的物理文档定位路径
 * @param channelExecutions 多渠道异步执行明细
 * @param skippedContexts 被过滤或跳过的 Context 列表
 */
public record RagRetrievalDebugSummary(
        String modelSource,
        KnowledgeLanguage actualLanguage,
        List<RetrievalChannel> channels,
        int denseCandidateCount,
        int sparseCandidateCount,
        int exactCandidateCount,
        int fusionCandidateCount,
        RerankMode rerankMode,
        boolean degraded,
        double topScore,
        double scoreGap,
        RagScoreLayerSummary scoreLayers,
        RagStageTimings timings,
        List<String> referenceLocations,
        List<RetrievalChannelExecution> channelExecutions,
        List<SkippedContext> skippedContexts
) {
    /** 紧凑构造函数做输入防空保护与默认值灌入 */
    public RagRetrievalDebugSummary {
        modelSource = (modelSource == null) ? "" : modelSource;
        actualLanguage = (actualLanguage == null) ? KnowledgeLanguage.OTHER : actualLanguage;
        channels = (channels == null) ? List.of() : List.copyOf(channels);
        rerankMode = (rerankMode == null) ? RerankMode.STANDARD_HYBRID : rerankMode;
        scoreLayers = (scoreLayers == null) ? new RagScoreLayerSummary(0D, 0D, 0D, 0D, 0D) : scoreLayers;
        timings = (timings == null) ? RagStageTimings.empty() : timings;
        referenceLocations = (referenceLocations == null) ? List.of() : List.copyOf(referenceLocations);
        channelExecutions = (channelExecutions == null) ? List.of() : List.copyOf(channelExecutions);
        skippedContexts = (skippedContexts == null) ? List.of() : List.copyOf(skippedContexts);
    }
}

