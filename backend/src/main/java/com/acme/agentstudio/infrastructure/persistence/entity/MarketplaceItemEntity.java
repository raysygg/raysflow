package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketplaceItem 数据库持久化实体对象。
 * 对应数据库中 MarketplaceItem 数据表的字段结构映射。
 */
@Data
@TableName("marketplace_item")
/**
 * 数据库实体：映射表 `marketplace_item`，承载 MarketplaceItem 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * MarketplaceItem 数据表持久化实体类。
 * 映射数据库对应的 MarketplaceItem 表结构。
 */
public class MarketplaceItemEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** item 展示名称 属性 */
    private String itemName;
    /** item 业务分类类型 属性 */
    private String itemType;
    /** publisher 属性 */
    private String publisher;
    /** item 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String itemStatus;
    /** manifest Json 属性 */
    private String manifestJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
