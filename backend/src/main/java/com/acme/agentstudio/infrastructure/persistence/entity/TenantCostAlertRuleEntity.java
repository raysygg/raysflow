package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
/**
 * TenantCostAlertRule 数据库持久化实体对象。
 * 对应数据库中 TenantCostAlertRule 数据表的字段结构映射。
 */
@Data @TableName("tenant_cost_alert_rule")
/**
 * 数据库实体：映射表 `tenant_cost_alert_rule`，承载 TenantCostAlertRule 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * TenantCostAlertRule 数据表持久化实体类。
 * 映射数据库对应的 TenantCostAlertRule 表结构。
 */
public class TenantCostAlertRuleEntity { @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private BigDecimal monthlyCostLimit; private BigDecimal alertPercent; private String status; private LocalDateTime updatedAt; }
