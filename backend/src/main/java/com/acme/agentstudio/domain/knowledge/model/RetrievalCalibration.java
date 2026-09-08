package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 检索结果相关性校准与断言防编造判定结果 Record（Retrieval Calibration）。
 * 包含接受保留的候选列表 acceptedCandidates (List&lt;RetrievalCandidate&gt;)、
 * 无命中判定标志 noHit、最高召回得分 topScore 及 Top-1/Top-2 分值极差 scoreGap。
 *
 * @param acceptedCandidates 校验过滤后保留的合格候选点
 * @param noHit 是否因未达到相似度下限阈值触发 No Hit 拒答
 * @param topScore 最高相似度得分
 * @param scoreGap 最高得分与次高得分差值
 */
public record RetrievalCalibration(
        List<RetrievalCandidate> acceptedCandidates,
        boolean noHit,
        double topScore,
        double scoreGap
) {
    /** 紧凑构造函数做候选 List 防空保护 */
    public RetrievalCalibration {
        acceptedCandidates = (acceptedCandidates == null) ? List.of() : List.copyOf(acceptedCandidates);
    }
}

