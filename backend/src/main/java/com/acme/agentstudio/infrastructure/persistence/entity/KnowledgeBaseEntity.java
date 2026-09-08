package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * KnowledgeBase 数据库持久化实体对象。
 * 对应数据库中 KnowledgeBase 数据表的字段结构映射。
 */
@Data
@TableName("knowledge_base")
/**
 * KnowledgeBase 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeBase 表结构。
 */
public class KnowledgeBaseEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** base 业务编码 属性 */
    private String baseCode;
    /** base 展示名称 属性 */
    private String baseName;
    /** base 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String baseStatus;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
