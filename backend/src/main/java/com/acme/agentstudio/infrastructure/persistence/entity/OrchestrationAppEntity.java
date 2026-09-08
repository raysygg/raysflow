package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OrchestrationApp 数据库持久化实体对象。
 * 对应数据库中 OrchestrationApp 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_app")
/**
 * 数据库实体：映射表 `orchestration_app`，承载 OrchestrationApp 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationApp 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationApp 表结构。
 */
public class OrchestrationAppEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用业务编码 */
    private String appCode;
    /** 应用名称 */
    private String appName;
    /** graph 业务分类类型 属性 */
    private String graphType;
    /** current Revision 主键 ID 标识 属性 */
    private Long currentRevisionId;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 创建人唯一标识 */
    private Long createdBy;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
