package com.acme.agentstudio.domain.knowledge.model;

import java.time.LocalDateTime;

/**
 * 知识库向量索引 Generation 构建进度与容量配置摘要实体 Record（Knowledge Index Generation Summary）。
 * 包含索引版本 ID generationId、构建状态 status (IndexGenerationStatus)、Qdrant 向量表名 collectionName、
 * Profile 代码 profileCode 与显示名称 profileName、使用的 Embedding/Reranker 模型 Key、
 * 关联的总文档数 totalDocuments、总切块数 totalChunks、索引完成切块数 indexedChunks、
 * 失败总数 failureCount、任务启动时间 startedAt、构建完成时间 completedAt 及激活为生产活动版本的时间 activatedAt。
 *
 * @param generationId 向量 Generation 版本 ID
 * @param status 索引版本生成状态
 * @param collectionName 向量 Collection 集合名称
 * @param profileCode Embedding Profile 编码
 * @param profileName Embedding Profile 名称
 * @param embeddingModelKey 使用的 Embedding 模型唯一 Key
 * @param rerankerModelKey 使用的 Reranker 重排模型唯一 Key
 * @param totalDocuments 关联的知识文档总数量
 * @param totalChunks 切分的 Chunk 总数量
 * @param indexedChunks 成功写入向量数据库的 Chunk 数量
 * @param failureCount 向量生成失败数量
 * @param startedAt 构建任务开始时刻
 * @param completedAt 构建任务完成时刻
 * @param activatedAt 切换为全量在线服务生效的激活时刻
 */
public record KnowledgeIndexGenerationSummary(
        Long generationId,
        String status,
        String collectionName,
        String profileCode,
        String profileName,
        String embeddingModelKey,
        String rerankerModelKey,
        Integer totalDocuments,
        Integer totalChunks,
        Integer indexedChunks,
        Integer failureCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime activatedAt
) {
}

