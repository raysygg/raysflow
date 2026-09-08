package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
/**
 * PlatformPriceVersion 数据库持久化实体对象。
 * 对应数据库中 PlatformPriceVersion 数据表的字段结构映射。
 */
@Data @TableName("platform_price_version")
/**
 * 数据库实体：映射表 `platform_price_version`，承载 PlatformPriceVersion 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformPriceVersion 数据表持久化实体类。
 * 映射数据库对应的 PlatformPriceVersion 表结构。
 */
public class PlatformPriceVersionEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private String modelCode; private BigDecimal inputPrice; private BigDecimal outputPrice;
    private String currency; private LocalDateTime validFrom; private LocalDateTime validUntil; private String status;
}
