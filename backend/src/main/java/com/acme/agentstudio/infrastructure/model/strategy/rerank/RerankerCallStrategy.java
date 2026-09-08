package com.acme.agentstudio.infrastructure.model.strategy.rerank;

import com.acme.agentstudio.domain.knowledge.model.RerankCandidate;
import com.acme.agentstudio.domain.knowledge.model.RerankResult;
import com.acme.agentstudio.domain.model.ModelProvider;
import com.acme.agentstudio.infrastructure.rag.model.RagModelConnectionResolver;

import java.util.List;

/**
 * RAG 交叉注意力机制 Reranker 精排重排策略接口（Reranker Call Strategy）。
 */
public interface RerankerCallStrategy {

    /**
     * 执行交叉注意力计算，对候选召回文档片段基于 Query 进行精排打分重排。
     *
     * @param connection 连接配置契约对象
     * @param query 用户的检索 Query 文本
     * @param candidates 粗排召回的候选文档段落列表
     * @return 精排计算结果列表 List&lt;RerankResult&gt;
     */
    List<RerankResult> rerank(RagModelConnectionResolver.ModelConnection connection, String query, List<RerankCandidate> candidates);

    /**
     * 判断当前策略是否匹配特定的大模型供应商（默认策略返回 false，特化类重写返回 true）。
     *
     * @param provider 模型供应商枚举 ModelProvider
     * @return true 表示支持该供应商特化逻辑
     */
    default boolean supports(ModelProvider provider) {
        return false;
    }
}

