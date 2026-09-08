package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.IdType; import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
/**
 * CostAlertEvent 数据库持久化实体对象。
 * 对应数据库中 CostAlertEvent 数据表的字段结构映射。
 */
@Data @TableName("cost_alert_event")
/**
 * 数据库实体：映射表 `cost_alert_event`，承载 CostAlertEvent 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * CostAlertEvent 数据表持久化实体类。
 * 映射数据库对应的 CostAlertEvent 表结构。
 */
public class CostAlertEventEntity { @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private String alertType; private BigDecimal currentCost; private BigDecimal limitCost; private String status; private LocalDateTime createdAt; }
