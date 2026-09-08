package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * KnowledgeDocumentTag 数据库持久化实体对象。
 * 对应数据库中 KnowledgeDocumentTag 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_document_tag")
/**
 * 数据库实体：映射表 `knowledge_document_tag`，承载 KnowledgeDocumentTag 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * KnowledgeDocumentTag 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeDocumentTag 表结构。
 */
public class KnowledgeDocumentTagEntity {
    @TableId(type = IdType.AUTO) private Long id;
    /** 绑定关系所属租户，防止跨租户绑定文档或标签。 */
    private Long tenantId;
    /** 知识文档 ID */
    private Long documentId;
    /** tag 主键 ID 标识 属性 */
    private Long tagId;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
