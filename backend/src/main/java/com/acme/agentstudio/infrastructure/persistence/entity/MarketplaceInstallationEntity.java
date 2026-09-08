package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * MarketplaceInstallation 数据库持久化实体对象。
 * 对应数据库中 MarketplaceInstallation 数据表的字段结构映射。
 */
@Data
@TableName("marketplace_installation")
/**
 * MarketplaceInstallation 数据表持久化实体类。
 * 映射数据库对应的 MarketplaceInstallation 表结构。
 */
public class MarketplaceInstallationEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** draft Revision 主键 ID 标识 属性 */
    private Long draftRevisionId;
    /** marketplace Item 主键 ID 标识 属性 */
    private Long marketplaceItemId;
    /** template 运行版本标识 属性 */
    private String templateVersion;
    /** source 业务分类类型 属性 */
    private String sourceType;
    /** manifest Fingerprint 属性 */
    private String manifestFingerprint;
    /** installation 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String installationStatus;
    /** installed By 属性 */
    private String installedBy;
    /** installed At 属性 */
    private LocalDateTime installedAt;
}
