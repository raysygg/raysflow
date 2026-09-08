package com.acme.agentstudio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG（检索增强生成）系统配置属性映射类。
 * 绑定配置文件中前缀为 `app.rag` 的参数，包含文档切片大小、向量混合召回权重、Qdrant 向量数据库地址以及重排阈值。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /** 子块目标字符数（默认 450 字符） */
    private int childChunkSize = 450;

    /** 子块重叠长度，保持边界语义连续（默认 60 字符） */
    private int childChunkOverlap = 60;

    /** 父级段落目标大小，用于命中后的上下文还原（默认 1800 字符） */
    private int parentChunkSize = 1800;

    /** 单次 Embedding 批处理大小（默认 32） */
    private int embeddingBatchSize = 32;

    /** 单次 MySQL 批量插入切片数量（默认 100） */
    private int mysqlChunkBatchSize = 100;

    /** 模型超时时间（单位：秒，默认 60s） */
    private int modelTimeoutSeconds = 60;

    /** 初始向量/文本候选召回最大数量上限（默认 80） */
    private int defaultCandidateLimit = 80;

    /** Rerank 重排相关性分数过滤阈值（默认 0.35） */
    private double defaultRerankThreshold = 0.35D;

    /** 未命中或无效相似度截断线（默认 0.20） */
    private double defaultNoHitThreshold = 0.20D;

    /** 相似度分差离散阈值（默认 0.03） */
    private double defaultScoreGapThreshold = 0.03D;

    /** 上下文填充的最大 Token 限制（默认 6000 Tokens） */
    private int defaultMaxContextTokens = 6000;

    /** 单次问答引用的最多文档条数（默认 5） */
    private int defaultMaxDocuments = 5;

    /** 单次问答引用的最多父块段落数（默认 8） */
    private int defaultMaxParentChunks = 8;

    /** 单篇文档中提取切片上限（默认 3） */
    private int defaultPerDocumentContextLimit = 3;

    /** 稠密向量召回得分权重（默认 0.6） */
    private double vectorRecallWeight = 0.6D;

    /** 稀疏词频/文本召回得分权重（默认 0.4） */
    private double lexicalRecallWeight = 0.4D;

    /** 精确匹配命中的加分系数（默认 0.25） */
    private double exactRecallBoost = 0.25D;

    /** 失效 Generation 索引版本的硬保留天数（默认 7 天） */
    private int generationRetentionDays = 7;

    /** Qdrant 向量数据库配置组 */
    private Qdrant qdrant = new Qdrant();

    /** 本地兜底模型配置组 */
    private Local local = new Local();

    /**
     * Qdrant 向量数据库配置属性。
     */
    @Getter
    @Setter
    public static class Qdrant {
        /** Qdrant REST/gRPC 服务连接基准 URL */
        private String baseUrl = "http://127.0.0.1:6333";

        /** Qdrant 访问 API Key（选填） */
        private String apiKey = "";

        /** 向量集合（Collection）名称前缀 */
        private String collectionPrefix = "agent_studio_rag";

        /** 客户端通信超时时间（秒） */
        private int timeoutSeconds = 30;

        /** Upsert 写入批次大小 */
        private int upsertBatchSize = 128;
    }

    /**
     * 本地兜底 Embedding 模型配置属性。
     */
    @Getter
    @Setter
    public static class Local {
        /** 本地兜底 Embedding 模型标识 */
        private String defaultModel = "local-all-minilm-l6-v2";
    }
}

