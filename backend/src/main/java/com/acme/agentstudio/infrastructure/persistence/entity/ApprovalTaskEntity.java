package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ApprovalTask 数据库持久化实体对象。
 * 对应数据库中 ApprovalTask 数据表的字段结构映射。
 */
@Data
@TableName("approval_task")
/**
 * 数据库实体：映射表 `approval_task`，承载 ApprovalTask 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * ApprovalTask 数据表持久化实体类。
 * 映射数据库对应的 ApprovalTask 表结构。
 */
public class ApprovalTaskEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 标题名称 */
    private String title;
    /** owner Team 属性 */
    private String ownerTeam;
    /** risk Level 属性 */
    private String riskLevel;
    /** approval 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String approvalStatus;
    /** payload Json 属性 */
    private String payloadJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
