package com.acme.agentstudio.domain.knowledge.model;

/**
 * 向量数据库（如 Qdrant）KNN 纯向量打分初筛召回点实体 Record（Vector Search Candidate）。
 * 包含物理点 UUID pointId、文档物理 ID documentId、切块物理 ID chunkId、
 * 父切块物理 ID parentChunkId、切块序号 chunkNo、标题层级路径 sectionPath 及向量余弦相似度打分 vectorScore。
 *
 * @param pointId 向量点 UUID
 * @param documentId 归属文档 ID
 * @param chunkId 归属切块物理 ID
 * @param parentChunkId 归属父切块物理 ID
 * @param chunkNo 切块在文档内的序号
 * @param sectionPath Markdown 标题层级路径
 * @param vectorScore 第一阶段 KNN 向量相似度得分
 */
public record VectorSearchCandidate(
        String pointId,
        Long documentId,
        Long chunkId,
        Long parentChunkId,
        Integer chunkNo,
        String sectionPath,
        double vectorScore
) {
}


