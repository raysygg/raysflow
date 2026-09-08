package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OrchestrationDraftRevision 数据库持久化实体对象。
 * 对应数据库中 OrchestrationDraftRevision 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_draft_revision")
/**
 * 数据库实体：映射表 `orchestration_draft_revision`，承载 OrchestrationDraftRevision 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationDraftRevision 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationDraftRevision 表结构。
 */
public class OrchestrationDraftRevisionEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用全局唯一 ID */
    private Long appId;
    /** 草稿修订版本号 */
    private Integer revisionNo;
    /** 画布节点连线结构 JSON 字符串 */
    private String graphJson;
    /** base 运行版本标识 主键 ID 标识 属性 */
    private String baseVersionId;
    /** change Summary 属性 */
    private String changeSummary;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
