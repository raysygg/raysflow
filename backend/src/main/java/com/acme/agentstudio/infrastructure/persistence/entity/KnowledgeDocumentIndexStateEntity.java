package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeDocumentIndexState 数据库持久化实体对象。
 * 对应数据库中 KnowledgeDocumentIndexState 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_document_index_state")
/**
 * KnowledgeDocumentIndexState 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeDocumentIndexState 表结构。
 */
public class KnowledgeDocumentIndexStateEntity {
    /** 知识文档 ID */
    @TableId
    private Long documentId;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** profile 主键 ID 标识 属性 */
    private Long profileId;
    /** active Generation 主键 ID 标识 属性 */
    private Long activeGenerationId;
    /** index 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String indexStatus;
    /** last Error 属性 */
    private String lastError;
    /** indexed At 属性 */
    private LocalDateTime indexedAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}

