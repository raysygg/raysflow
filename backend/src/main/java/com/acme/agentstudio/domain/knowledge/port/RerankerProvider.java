package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankerExecutionResult;
import java.util.List;

/**
 * 文本重排模型 (Reranker Model) 服务基础设施端口接口（Reranker Provider）。
 * 对初筛召回的 Candidate 列表在二次交互场景下重新打分与精排。
 */
public interface RerankerProvider {

    /**
     * 调用外部/本地 Reranker 模型对多路召回候选点进行精排。
     *
     * @param profile 向量与重排配置 Profile
     * @param query 用户检索 Query
     * @param candidates 待重排的候选切片列表 List&lt;RerankCandidate&gt;
     * @return 包含重排打分结果与用量微元信息的 RerankerExecutionResult 对象
     */
    RerankerExecutionResult rerank(RagEmbeddingProfile profile, String query, List<RerankCandidate> candidates);
}

