package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeDocumentVersion 数据库持久化实体对象。
 * 对应数据库中 KnowledgeDocumentVersion 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_document_version")
/**
 * KnowledgeDocumentVersion 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeDocumentVersion 表结构。
 */
public class KnowledgeDocumentVersionEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** source 主键 ID 标识 属性 */
    private Long sourceId;
    /** 知识文档 ID */
    private Long documentId;
    /** external Document 主键 ID 标识 属性 */
    private String externalDocumentId;
    /** 运行版本标识 No 属性 */
    private Integer versionNo;
    /** 正文内容 Fingerprint 属性 */
    private String contentFingerprint;
    /** normalized Length 属性 */
    private Integer normalizedLength;
    /** 运行版本标识 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String versionStatus;
    /** permission Snapshot Json 属性 */
    private String permissionSnapshotJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
