package com.acme.agentstudio.domain.knowledge.model;

import java.time.LocalDateTime;

/**
 * 知识文档向量索引状态与模型配置明细实体 Record（Knowledge Document Index Detail）。
 * 集中关联文档元数据对象 document (KnowledgeDocumentSummary)、索引构建状态 indexStatus、
 * 最近一次错误日志 lastError、最近构建成功时刻 indexedAt、绑定的索引版本 ID indexGenerationId、
 * 向量 Collection 集合名 collectionName、Embedding Profile 编码与名称以及 Embedding/Reranker 模型 Key。
 *
 * @param document 知识文档摘要基础元数据（KnowledgeDocumentSummary）
 * @param indexStatus 向量索引构建状态说明
 * @param lastError 最近一次解析/切块/向量化失败的异常错误日志信息
 * @param indexedAt 最近一次向量构建完成时间
 * @param indexGenerationId 关联的向量 Generation 版本 ID
 * @param collectionName Qdrant 向量数据库中的集合表名
 * @param embeddingProfileCode 使用的 Embedding Profile 编码
 * @param embeddingProfileName 发生的 Embedding Profile 显示名称
 * @param embeddingModelKey 使用的 Embedding 模型唯一 Key
 * @param rerankerModelKey 使用的 Reranker 重排模型唯一 Key
 */
public record KnowledgeDocumentIndexDetail(
        KnowledgeDocumentSummary document,
        String indexStatus,
        String lastError,
        LocalDateTime indexedAt,
        Long indexGenerationId,
        String collectionName,
        String embeddingProfileCode,
        String embeddingProfileName,
        String embeddingModelKey,
        String rerankerModelKey
) {
}

