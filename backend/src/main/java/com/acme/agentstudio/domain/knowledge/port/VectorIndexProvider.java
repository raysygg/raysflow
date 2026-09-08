package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.LexicalSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.LexicalSearchRequest;
import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import com.acme.agentstudio.domain.knowledge.model.VectorCollectionInspection;
import com.acme.agentstudio.domain.knowledge.model.VectorIndexPoint;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchCandidate;
import com.acme.agentstudio.domain.knowledge.model.VectorSearchRequest;
import java.util.List;

/**
 * 向量数据库（如 Qdrant）存储、建表、向量与词法多路检索基础设施端口接口（Vector Index Provider）。
 * 集中管理向量 Collection 的初始化、插入更新 Upsert、稠密/稀疏/精确搜索及按文档物理删除等生命周期能力。
 */
public interface VectorIndexProvider {

    /**
     * 检查底层向量数据库集群连接与健康状态。
     */
    void checkHealth();

    /**
     * 确保指定的 Collection 集合表已建立，维度与度量标准与 Profile 匹配。
     *
     * @param collectionName Collection 集合表名
     * @param profile 向量配置 Profile 对象
     */
    void ensureCollection(String collectionName, RagEmbeddingProfile profile);

    /**
     * 检查并返回特定 Collection 的状态、容量与索引统计指标。
     *
     * @param collectionName Collection 集合表名
     * @param profile 向量配置 Profile 对象
     * @return 包含容量与指标摘要的 VectorCollectionInspection 对象
     */
    VectorCollectionInspection inspectCollection(String collectionName, RagEmbeddingProfile profile);

    /**
     * 批量插入或覆盖更新向量数据点 (Points)。
     *
     * @param collectionName Collection 集合表名
     * @param points 向量点元数据与 Embedding 列表 List&lt;VectorIndexPoint&gt;
     */
    void upsert(String collectionName, List<VectorIndexPoint> points);

    /**
     * 执行 KNN 稠密向量相似度检索。
     *
     * @param request 向量检索请求参数
     * @return 向量检索命中结果候选列表 List&lt;VectorSearchCandidate&gt;
     */
    List<VectorSearchCandidate> search(VectorSearchRequest request);

    /**
     * 执行 Sparse 稀疏向量/BM25 词法全文检索。
     *
     * @param request 词法检索请求参数
     * @return 词法检索命中候选列表 List&lt;LexicalSearchCandidate&gt;
     */
    List<LexicalSearchCandidate> searchSparse(LexicalSearchRequest request);

    /**
     * 执行关键字精确/前缀匹配检索。
     *
     * @param request 词法/精确检索请求参数
     * @return 命中候选列表 List&lt;LexicalSearchCandidate&gt;
     */
    List<LexicalSearchCandidate> searchExact(LexicalSearchRequest request);

    /**
     * 根据租户 ID 与文档 ID 批量物理删除该文档对应的所有向量点。
     *
     * @param collectionName Collection 集合表名
     * @param tenantId 租户 ID
     * @param documentId 文档 ID
     */
    void deleteDocument(String collectionName, Long tenantId, Long documentId);

    /**
     * 根据点 Point ID 列表批量物理删除向量。
     *
     * @param collectionName Collection 集合表名
     * @param pointIds Point UUID 列表
     */
    void deletePoints(String collectionName, List<String> pointIds);

    /**
     * 彻底删除指定的 Collection 集合表及其所有向量数据。
     *
     * @param collectionName Collection 集合表名
     */
    void deleteCollection(String collectionName);
}

