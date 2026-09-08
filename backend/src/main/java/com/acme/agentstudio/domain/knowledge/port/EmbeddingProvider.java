package com.acme.agentstudio.domain.knowledge.port;

import com.acme.agentstudio.domain.knowledge.model.RagEmbeddingProfile;
import java.util.List;

/**
 * 文本向量化 Embedding 模型服务基础设施端口接口（Embedding Provider）。
 * 屏蔽 OpenAI, DashScope, Ollama 等底座模型的物理调用细节。
 */
public interface EmbeddingProvider {

    /**
     * 批量计算文档切片的向量表达 (Embedding Vectors)。
     *
     * @param profile 向量配置 Profile 对象
     * @param texts 待向量化的纯文本列表
     * @return 浮点向量二维数组 List&lt;List&lt;Float&gt;&gt;
     */
    List<List<Float>> embedDocuments(RagEmbeddingProfile profile, List<String> texts);

    /**
     * 计算检索 Query 的单条向量表达。
     *
     * @param profile 向量配置 Profile 对象
     * @param query 查询 Query 文本
     * @return 浮点向量数组 List&lt;Float&gt;
     */
    List<Float> embedQuery(RagEmbeddingProfile profile, String query);
}


