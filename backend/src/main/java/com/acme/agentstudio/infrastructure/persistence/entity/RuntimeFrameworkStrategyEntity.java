package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RuntimeFrameworkStrategy 数据库持久化实体对象。
 * 对应数据库中 RuntimeFrameworkStrategy 数据表的字段结构映射。
 */
/** 数据库实体：保存租户级原生框架策略注册信息。 */
@Data
@TableName("runtime_framework_strategy")
public class RuntimeFrameworkStrategyEntity {
    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** framework 业务编码 属性 */
    private String frameworkCode;
    /** adapter 运行版本标识 属性 */
    private String adapterVersion;
    /** enabled 属性 */
    private Boolean enabled;
    /** capability Json 属性 */
    private String capabilityJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
