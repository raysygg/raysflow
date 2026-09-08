package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
/**
 * PlatformToolInvocation 数据库持久化实体对象。
 * 对应数据库中 PlatformToolInvocation 数据表的字段结构映射。
 */
@Data @TableName("platform_tool_invocation")
/**
 * 数据库实体：映射表 `platform_tool_invocation`，承载 PlatformToolInvocation 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformToolInvocation 数据表持久化实体类。
 * 映射数据库对应的 PlatformToolInvocation 表结构。
 */
public class PlatformToolInvocationEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private String executionId; private Long connectorId;
    private String requestSummary; private String responseSummary; private Integer statusCode; private String resultStatus;
    private Long latencyMs; private Integer attemptCount; private String errorMessage; private LocalDateTime createdAt;
}
