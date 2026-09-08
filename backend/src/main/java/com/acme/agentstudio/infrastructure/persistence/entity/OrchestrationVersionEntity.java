package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OrchestrationVersion 数据库持久化实体对象。
 * 对应数据库中 OrchestrationVersion 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_version")
/**
 * 数据库实体：映射表 `orchestration_version`，承载 OrchestrationVersion 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationVersion 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationVersion 表结构。
 */
public class OrchestrationVersionEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用全局唯一 ID */
    private Long appId;
    /** candidate 主键 ID 标识 属性 */
    private Long candidateId;
    /** candidate Fingerprint 属性 */
    private String candidateFingerprint;
    /** 运行版本标识 主键 ID 标识 属性 */
    private String versionId;
    /** 运行版本标识 No 属性 */
    private Integer versionNo;
    /** 画布节点连线结构 JSON 字符串 */
    private String graphJson;
    /** 发布时冻结的应用、流程和资源绑定快照，历史版本禁止回写。 */
    private String releaseBundleJson;
    /** Release Bundle 内容摘要，用于运行前校验快照未被篡改。 */
    private String releaseBundleHash;
    /** gate Report 主键 ID 标识 属性 */
    private Long gateReportId;
    /** override Granted 属性 */
    private Boolean overrideGranted;
    /** change Summary 属性 */
    private String changeSummary;
    /** released By 属性 */
    private Long releasedBy;
    /** released At 属性 */
    private LocalDateTime releasedAt;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
}
