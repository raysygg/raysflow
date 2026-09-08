package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OrchestrationEnvironment 数据库持久化实体对象。
 * 对应数据库中 OrchestrationEnvironment 数据表的字段结构映射。
 */
@Data
@TableName("orchestration_environment")
/**
 * 数据库实体：映射表 `orchestration_environment`，承载 OrchestrationEnvironment 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * OrchestrationEnvironment 数据表持久化实体类。
 * 映射数据库对应的 OrchestrationEnvironment 表结构。
 */
public class OrchestrationEnvironmentEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 应用全局唯一 ID */
    private Long appId;
    /** environment 业务编码 属性 */
    private String environmentCode;
    /** current 运行版本标识 主键 ID 标识 属性 */
    private String currentVersionId;
    /** 最后修改人标识 */
    private Long updatedBy;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
