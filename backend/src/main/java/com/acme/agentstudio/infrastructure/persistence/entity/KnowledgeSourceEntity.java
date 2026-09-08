package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeSource 数据库持久化实体对象。
 * 对应数据库中 KnowledgeSource 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_source")
/**
 * KnowledgeSource 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeSource 表结构。
 */
public class KnowledgeSourceEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** knowledge Base 主键 ID 标识 属性 */
    private Long knowledgeBaseId;
    /** source 业务编码 属性 */
    private String sourceCode;
    /** source 业务分类类型 属性 */
    private String sourceType;
    /** display 展示名称 属性 */
    private String displayName;
    /** connector 业务分类类型 属性 */
    private String connectorType;
    /** credential Reference 属性 */
    private String credentialReference;
    /** source 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String sourceStatus;
    /** sync Cursor 属性 */
    private String syncCursor;
    /** freshness Policy Json 属性 */
    private String freshnessPolicyJson;
    /** permission Policy Json 属性 */
    private String permissionPolicyJson;
    /** last Synced At 属性 */
    private LocalDateTime lastSyncedAt;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
