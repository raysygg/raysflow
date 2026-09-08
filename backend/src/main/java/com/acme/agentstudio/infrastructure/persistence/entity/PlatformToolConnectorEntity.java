package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * PlatformToolConnector 数据库持久化实体对象。
 * 对应数据库中 PlatformToolConnector 数据表的字段结构映射。
 */
@Data @TableName("platform_tool_connector")
/**
 * 数据库实体：映射表 `platform_tool_connector`，承载 PlatformToolConnector 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformToolConnector 数据表持久化实体类。
 * 映射数据库对应的 PlatformToolConnector 表结构。
 */
public class PlatformToolConnectorEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private String connectorCode; private String connectorType;
    private String endpoint; private Long credentialRefId; private Integer timeoutMs; private String retryPolicyJson; private String status;
    private Long createdBy; private LocalDateTime createdAt;
}
