package com.acme.agentstudio.domain.knowledge.model;

/**
 * Qdrant Sparse/BM25 词法全文与精确关键字检索命中候选实体 Record（Lexical Search Candidate）。
 * 包含向量点 ID pointId、文档 ID documentId、切块 ID chunkId、父切块 ID parentChunkId、
 * 序号 chunkNo、标题章节路径 sectionPath、稀疏向量得分 sparseScore、精确匹配得分 exactScore 及命中原因 hitReason (RetrievalHitReason)。
 *
 * @param pointId Qdrant Vector Point UUID 唯一标识
 * @param documentId 关联文档 ID
 * @param chunkId 切块 Chunk 数据库物理 ID
 * @param parentChunkId 父切块 Chunk 物理 ID
 * @param chunkNo 文档内切块次序索引号
 * @param sectionPath 文档层级目录与章节路径
 * @param sparseScore BM25 / Sparse 稀疏向量检索得分
 * @param exactScore 关键词精确/前缀匹配得分
 * @param hitReason 检索命中原因分类（RetrievalHitReason）
 */
public record LexicalSearchCandidate(
        String pointId,
        Long documentId,
        Long chunkId,
        Long parentChunkId,
        Integer chunkNo,
        String sectionPath,
        double sparseScore,
        double exactScore,
        RetrievalHitReason hitReason
) {
    /** 紧凑构造函数做打分负值裁剪与默认命中原因归一 */
    public LexicalSearchCandidate {
        sparseScore = Math.max(0D, sparseScore);
        exactScore = Math.max(0D, exactScore);
        hitReason = (hitReason == null) ? RetrievalHitReason.SPARSE_TERM_MATCH : hitReason;
    }
}

