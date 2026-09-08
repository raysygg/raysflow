package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeIndexGeneration 数据库持久化实体对象。
 * 对应数据库中 KnowledgeIndexGeneration 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_index_generation")
/**
 * KnowledgeIndexGeneration 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeIndexGeneration 表结构。
 */
public class KnowledgeIndexGenerationEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** profile 主键 ID 标识 属性 */
    private Long profileId;
    /** Embedding 契约指纹，确保不同模型/版本不会共用向量空间。 */
    private String embeddingFingerprint;
    /** collection 展示名称 属性 */
    private String collectionName;
    /** generation 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String generationStatus;
    /** 符合条件的总记录条数 Documents 属性 */
    private Integer totalDocuments;
    /** 符合条件的总记录条数 Chunks 属性 */
    private Integer totalChunks;
    /** indexed Chunks 属性 */
    private Integer indexedChunks;
    /** failure Count 属性 */
    private Integer failureCount;
    /** error Message 属性 */
    private String errorMessage;
    /** started At 属性 */
    private LocalDateTime startedAt;
    /** completed At 属性 */
    private LocalDateTime completedAt;
    /** activated At 属性 */
    private LocalDateTime activatedAt;
}
