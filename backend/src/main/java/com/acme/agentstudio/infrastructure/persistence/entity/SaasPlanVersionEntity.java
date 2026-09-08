package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaasPlanVersion 数据库持久化实体对象。
 * 对应数据库中 SaasPlanVersion 数据表的字段结构映射。
 */
@Data
@TableName("saas_plan_version")
/**
 * SaasPlanVersion 数据表持久化实体类。
 * 映射数据库对应的 SaasPlanVersion 表结构。
 */
public class SaasPlanVersionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String planCode; private Integer versionNo; private String planName; private String status;
    private String currency; private BigDecimal monthlyBasePrice; private String featureJson; private String entitlementJson;
    private LocalDateTime effectiveAt; private LocalDateTime retiredAt; private LocalDateTime createdAt;
}
