package com.acme.agentstudio.domain.knowledge.model;

/**
 * 传输给 Reranker 模型二次打分与精排的文本候选对象 Record（Rerank Candidate）。
 * 包含切块物理 ID chunkId、切块正文内容 text 及第一阶段向量/词法召回打分 vectorScore。
 *
 * @param chunkId 关联切块数据库 ID
 * @param text 切块待重排文本
 * @param vectorScore 第一阶段初筛得分
 */
public record RerankCandidate(
        Long chunkId,
        String text,
        double vectorScore
) {
}


