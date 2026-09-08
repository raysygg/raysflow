package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RetrievalCandidate;
import java.util.List;

/**
 * 检索结果多打散与去重多样性选择器端口接口（Retrieval Diversity Selector）。
 * 防止单文档/单父段落垄断 Top-K 结果，提升 RAG 上下文的多样性与包含面。
 */
public interface RetrievalDiversitySelector {

    /**
     * 对候选 List 实施多样性打散、单一文档上限拦截与 MMR 去重过滤。
     *
     * @param profile 向量与重排配置 Profile
     * @param candidates 候选集合 List&lt;RetrievalCandidate&gt;
     * @param topK 目标提取保留的数量 Top-K
     * @return 过滤打散后的候选列表 List&lt;RetrievalCandidate&gt;
     */
    List<RetrievalCandidate> select(RagEmbeddingProfile profile, List<RetrievalCandidate> candidates, int topK);
}

