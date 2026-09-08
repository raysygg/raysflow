package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 写入向量数据库（如 Qdrant）的物理数据点 Point 结构实体 Record（Vector Index Point）。
 * 包含点 UUID pointId、租户物理 ID tenantId、文档 ID documentId、切块物理 ID chunkId、
 * 索引代次 ID indexGenerationId、父切块 ID parentChunkId、语种 language、切块序号 chunkNo、
 * 标题路径 sectionPath、稠密向量 denseVector、稀疏向量 lexicalVector (SparseVectorData)、
 * 精确哈希键 exactKeys (List&lt;String&gt;) 及多向量延迟交互向量 lateInteractionVectors。
 *
 * @param pointId 向量点 UUID 标识
 * @param tenantId 租户 ID
 * @param documentId 归属文档 ID
 * @param chunkId 归属切块物理 ID
 * @param indexGenerationId 生效的 Generation 索引代次 ID
 * @param parentChunkId 归属父切块物理 ID
 * @param language 识别/确认的语种 (KnowledgeLanguage)
 * @param chunkNo 切块在文档内的序号
 * @param sectionPath Markdown 标题层级路径
 * @param denseVector 稠密 Embedding 浮点向量数组
 * @param lexicalVector 稀疏词法向量表示
 * @param exactKeys 关键词精确匹配 Key 数组
 * @param lateInteractionVectors ColBERT 风格多向量延迟交互张量数组
 */
public record VectorIndexPoint(
        String pointId,
        Long tenantId,
        Long documentId,
        Long chunkId,
        Long indexGenerationId,
        Long parentChunkId,
        KnowledgeLanguage language,
        Integer chunkNo,
        String sectionPath,
        List<Float> denseVector,
        SparseVectorData lexicalVector,
        List<String> exactKeys,
        List<List<Float>> lateInteractionVectors
) {
    /** 紧凑构造函数做输入数组与 List 防空保护 */
    public VectorIndexPoint {
        denseVector = (denseVector == null) ? List.of() : List.copyOf(denseVector);
        lexicalVector = (lexicalVector == null) ? SparseVectorData.empty() : lexicalVector;
        exactKeys = (exactKeys == null) ? List.of() : List.copyOf(exactKeys);
        lateInteractionVectors = (lateInteractionVectors == null) ? List.of() : List.copyOf(lateInteractionVectors);
    }
}

