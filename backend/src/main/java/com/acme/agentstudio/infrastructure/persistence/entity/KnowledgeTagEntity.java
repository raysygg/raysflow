package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * KnowledgeTag 数据库持久化实体对象。
 * 对应数据库中 KnowledgeTag 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_tag")
/**
 * 数据库实体：映射表 `knowledge_tag`，承载 KnowledgeTag 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * KnowledgeTag 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeTag 表结构。
 */
public class KnowledgeTagEntity {
    @TableId(type = IdType.AUTO) private Long id;
    /** 标签只能在当前租户内使用。 */
    private Long tenantId;
    /** tag 展示名称 属性 */
    private String tagName;
    /** 前端展示颜色，不参与权限判断。 */
    private String tagColor;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
