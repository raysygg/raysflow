package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 一个切块候选点在多路召回通道中的分值拆解与可解释性数据实体 Record（Retrieval Candidate Score）。
 * 包含切块物理 ID chunkId、词法分 sparseScore、精确分 exactScore、RRF 融合分 fusionScore、
 * 命中的通道列表 channels (List&lt;RetrievalChannel&gt;) 及命中原因列表 hitReasons (List&lt;RetrievalHitReason&gt;)。
 *
 * @param chunkId 归属切块 ID
 * @param sparseScore 词法召回得分
 * @param exactScore 精确匹配得分
 * @param fusionScore RRF 融合得分 [0.0, 1.0]
 * @param channels 命中的通道列表
 * @param hitReasons 命中的原因分类
 */
public record RetrievalCandidateScore(
        Long chunkId,
        double sparseScore,
        double exactScore,
        double fusionScore,
        List<RetrievalChannel> channels,
        List<RetrievalHitReason> hitReasons
) {
    /** 紧凑构造函数进行数值防负校准与 List 防空保护 */
    public RetrievalCandidateScore {
        sparseScore = Math.max(0D, sparseScore);
        exactScore = Math.max(0D, exactScore);
        fusionScore = Math.max(0D, Math.min(1D, fusionScore));
        channels = (channels == null) ? List.of() : List.copyOf(channels);
        hitReasons = (hitReasons == null) ? List.of() : List.copyOf(hitReasons);
    }
}

