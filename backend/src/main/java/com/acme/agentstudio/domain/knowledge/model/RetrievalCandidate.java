package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 检索流水线全流程传递的类型化候选块实体 Record（Retrieval Candidate）。
 * 分别精确保留向量分 denseScore、词法分 sparseScore、精确分 exactScore、RRF 融合分 fusionScore
 * 及 Reranker 重排分 rerankScore，避免混用不同评分空间。
 *
 * @param chunkId 切块物理 ID
 * @param documentId 归属文档 ID
 * @param parentChunkId 父段落切块 ID
 * @param chunkNo 切块序号
 * @param sectionPath 标题层级路径
 * @param text 切块纯文本内容
 * @param source 知识来源标识
 * @param language 语种枚举 (KnowledgeLanguage)
 * @param contentHash 内容哈希指纹
 * @param tokenCount Token 数量
 * @param vectorScore 向量分数
 * @param relevanceScore 综合相关性最终得分
 * @param denseScore 稠密向量分数
 * @param sparseScore 稀疏词法分数
 * @param exactScore 关键词精确匹配分数
 * @param fusionScore 多路融合分数
 * @param rerankScore 重排模型精排分数
 * @param channels 命中的检索通道列表 (List&lt;RetrievalChannel&gt;)
 * @param hitReasons 命中原因列表 (List&lt;RetrievalHitReason&gt;)
 */
public record RetrievalCandidate(
        Long chunkId,
        Long documentId,
        Long parentChunkId,
        Integer chunkNo,
        String sectionPath,
        String text,
        String source,
        KnowledgeLanguage language,
        String contentHash,
        int tokenCount,
        double vectorScore,
        double relevanceScore,
        double denseScore,
        double sparseScore,
        double exactScore,
        double fusionScore,
        double rerankScore,
        List<RetrievalChannel> channels,
        List<RetrievalHitReason> hitReasons
) {
    /** 快捷兼容构造函数 */
    public RetrievalCandidate(
            Long chunkId, Long documentId, Long parentChunkId, Integer chunkNo,
            String sectionPath, String text, String source, KnowledgeLanguage language,
            String contentHash, int tokenCount, double vectorScore, double relevanceScore
    ) {
        this(chunkId, documentId, parentChunkId, chunkNo, sectionPath, text, source, language, contentHash,
                tokenCount, vectorScore, relevanceScore, vectorScore, 0D, 0D, relevanceScore, 0D,
                List.of(RetrievalChannel.VECTOR), List.of(RetrievalHitReason.VECTOR_SIMILARITY));
    }

    /** 紧凑构造函数做输入文本与 List 防空校验 */
    public RetrievalCandidate {
        language = (language == null) ? KnowledgeLanguage.OTHER : language;
        sectionPath = (sectionPath == null) ? "" : sectionPath;
        text = (text == null) ? "" : text;
        source = (source == null) ? "" : source;
        contentHash = (contentHash == null) ? "" : contentHash;
        tokenCount = Math.max(0, tokenCount);
        channels = (channels == null) ? List.of() : List.copyOf(channels);
        hitReasons = (hitReasons == null) ? List.of() : List.copyOf(hitReasons);
    }

    /**
     * 更新相关性最终得分。
     *
     * @param score 新的相关性分数
     * @return 包含新相关性得分的全新 RetrievalCandidate 实例
     */
    public RetrievalCandidate withRelevanceScore(double score) {
        return new RetrievalCandidate(chunkId, documentId, parentChunkId, chunkNo, sectionPath, text, source,
                language, contentHash, tokenCount, vectorScore, score, denseScore, sparseScore, exactScore,
                fusionScore, rerankScore, channels, hitReasons);
    }

    /**
     * 更新多通道打分与命中原因。
     *
     * @param dense 稠密向量得分
     * @param sparse 稀疏词法得分
     * @param exact 精确匹配得分
     * @param fusion 融合得分
     * @param rerank 重排得分
     * @param actualChannels 生效通道列表
     * @param actualHitReasons 命中原因列表
     * @return 更新各通道打分后的 RetrievalCandidate 实例
     */
    public RetrievalCandidate withChannelScores(
            double dense, double sparse, double exact, double fusion,
            double rerank, List<RetrievalChannel> actualChannels,
            List<RetrievalHitReason> actualHitReasons
    ) {
        double effectiveRelevance = (rerank > 0D) ? rerank : fusion;
        return new RetrievalCandidate(chunkId, documentId, parentChunkId, chunkNo, sectionPath, text, source,
                language, contentHash, tokenCount, dense, effectiveRelevance,
                dense, sparse, exact, fusion, rerank, actualChannels, actualHitReasons);
    }

    /**
     * 更新重排打分。
     *
     * @param score 重排得分
     * @return 更新重排打分后的 RetrievalCandidate 实例
     */
    public RetrievalCandidate withRerankScore(double score) {
        return new RetrievalCandidate(chunkId, documentId, parentChunkId, chunkNo, sectionPath, text, source,
                language, contentHash, tokenCount, vectorScore, score, denseScore, sparseScore, exactScore,
                fusionScore, score, channels, hitReasons);
    }
}

