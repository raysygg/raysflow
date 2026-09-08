package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ApplicationEntrypoint 数据库持久化实体对象。
 * 对应数据库中 ApplicationEntrypoint 数据表的字段结构映射。
 */
@Data
@TableName("application_entrypoint")
/**
 * ApplicationEntrypoint 数据表持久化实体类。
 * 映射数据库对应的 ApplicationEntrypoint 表结构。
 */
public class ApplicationEntrypointEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** invoke 业务编码 属性 */
    private String invokeCode;
    /** 展示名称 */
    private String name;
    /** entrypoint 业务分类类型 属性 */
    private String entrypointType;
    /** 运行版本标识 Policy 属性 */
    private String versionPolicy;
    /** pinned 运行版本标识 主键 ID 标识 属性 */
    private String pinnedVersionId;
    /** delivery Mode 属性 */
    private String deliveryMode;
    /** input Schema Json 属性 */
    private String inputSchemaJson;
    /** cron Expression 属性 */
    private String cronExpression;
    /** timezone 属性 */
    private String timezone;
    /** next Fire At 属性 */
    private LocalDateTime nextFireAt;
    /** schedule Lease Owner 属性 */
    private String scheduleLeaseOwner;
    /** schedule Lease Until 属性 */
    private LocalDateTime scheduleLeaseUntil;
    /** api Credential Hash 属性 */
    private String apiCredentialHash;
    /** webhook Secret Ciphertext 属性 */
    private String webhookSecretCiphertext;
    /** credential Mask 属性 */
    private String credentialMask;
    /** enabled 属性 */
    private Boolean enabled;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
