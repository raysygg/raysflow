package com.acme.agentstudio.domain.knowledge.model;

/**
 * RAG 管道各处理阶段耗时（单位：毫秒）统计实体 Record（RAG Stage Timings）。
 * 包含查询改写耗时 queryRewriteMs、向量化计算耗时 embeddingMs、向量数据库检索耗时 vectorSearchMs、
 * 重排模型打分耗时 rerankMs 及上下文裁剪组装耗时 contextAssemblyMs。
 *
 * @param queryRewriteMs 查询改写耗时（ms）
 * @param embeddingMs Query 向量化计算耗时（ms）
 * @param vectorSearchMs 向量/词法多路检索耗时（ms）
 * @param rerankMs Reranker 重排打分耗时（ms）
 * @param contextAssemblyMs 最终 Context 裁剪拼接耗时（ms）
 */
public record RagStageTimings(
        long queryRewriteMs,
        long embeddingMs,
        long vectorSearchMs,
        long rerankMs,
        long contextAssemblyMs
) {
    /**
     * 构建零耗时的全空阶段耗时实例。
     *
     * @return 耗时全为 0 的 RagStageTimings 对象
     */
    public static RagStageTimings empty() {
        return new RagStageTimings(0, 0, 0, 0, 0);
    }
}


