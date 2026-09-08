package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RagEmbeddingProfile 数据库持久化实体对象。
 * 对应数据库中 RagEmbeddingProfile 数据表的字段结构映射。
 */
@Data
@TableName("rag_embedding_profile")
/**
 * RagEmbeddingProfile 数据表持久化实体类。
 * 映射数据库对应的 RagEmbeddingProfile 表结构。
 */
public class RagEmbeddingProfileEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** profile 业务编码 属性 */
    private String profileCode;
    /** profile 展示名称 属性 */
    private String profileName;
    /** embedding Source 属性 */
    private String embeddingSource;
    /** embedding Model 主键 ID 标识 属性 */
    private Long embeddingModelId;
    /** embedding Model Key 属性 */
    private String embeddingModelKey;
    /** reranker Model Key 属性 */
    private String rerankerModelKey;
    /** query Rewrite Model Key 属性 */
    private String queryRewriteModelKey;
    /** vector 向量 Embeddings 维度大小 属性 */
    private Integer vectorDimension;
    /** distance Metric 属性 */
    private String distanceMetric;
    /** candidate Limit 属性 */
    private Integer candidateLimit;
    /** rerank Threshold 属性 */
    private Double rerankThreshold;
    /** no Hit Threshold 属性 */
    private Double noHitThreshold;
    /** score Gap Threshold 属性 */
    private Double scoreGapThreshold;
    /** max Context Tokens 属性 */
    private Integer maxContextTokens;
    /** max Documents 属性 */
    private Integer maxDocuments;
    /** max Parent Chunks 属性 */
    private Integer maxParentChunks;
    /** per Document Context Limit 属性 */
    private Integer perDocumentContextLimit;
    /** query Rewrite Enabled 属性 */
    private Boolean queryRewriteEnabled;
    /** late Interaction Enabled 属性 */
    private Boolean lateInteractionEnabled;
    /** degrade Policy 属性 */
    private String degradePolicy;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 运行版本标识 No 属性 */
    private Integer versionNo;
    /** supported Languages 属性 */
    private String supportedLanguages;
    /** supported Scripts 属性 */
    private String supportedScripts;
    /** cross Language Enabled 属性 */
    private Boolean crossLanguageEnabled;
    /** model 运行版本标识 属性 */
    private String modelVersion;
    /** query Instruction 属性 */
    private String queryInstruction;
    /** document Instruction 属性 */
    private String documentInstruction;
    /** reranker Enabled 属性 */
    private Boolean rerankerEnabled;
    /** reranker Candidate Limit 属性 */
    private Integer rerankerCandidateLimit;
    /** reranker Timeout Seconds 属性 */
    private Integer rerankerTimeoutSeconds;
    /** reranker Budget 属性 */
    private Double rerankerBudget;
    /** vector Recall Weight 属性 */
    private Double vectorRecallWeight;
    /** lexical Recall Weight 属性 */
    private Double lexicalRecallWeight;
    /** exact Recall Boost 属性 */
    private Double exactRecallBoost;
    /** active 属性 */
    @TableField("is_active")
    private Boolean active;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
