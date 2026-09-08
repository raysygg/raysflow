package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RagProfileVersion 数据库持久化实体对象。
 * 对应数据库中 RagProfileVersion 数据表的字段结构映射。
 */
@Data
@TableName("rag_profile_version")
/**
 * RagProfileVersion 数据表持久化实体类。
 * 映射数据库对应的 RagProfileVersion 表结构。
 */
public class RagProfileVersionEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** profile 主键 ID 标识 属性 */
    private Long profileId;
    /** 运行版本标识 No 属性 */
    private Integer versionNo;
    /** embedding Fingerprint 属性 */
    private String embeddingFingerprint;
    /** configuration Json 属性 */
    private String configurationJson;
    /** 运行版本标识 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String versionStatus;
    /** activated At 属性 */
    private LocalDateTime activatedAt;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
