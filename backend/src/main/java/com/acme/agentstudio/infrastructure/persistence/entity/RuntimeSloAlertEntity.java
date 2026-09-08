package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeSloAlert 数据库持久化实体对象。
 * 对应数据库中 RuntimeSloAlert 数据表的字段结构映射。
 */
/** SLO 告警持久化记录，alertKey 在租户范围内用于去重。 */
@Data
@TableName("runtime_slo_alert")
public class RuntimeSloAlertEntity {
    /** 租户全局唯一标识 ID */
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId;
    /** application 主键 ID 标识 属性 */
    private Long applicationId;
    /** release 主键 ID 标识 属性 */
    private String releaseId;
    /** alert Key 属性 */
    private String alertKey;
    /** sli 业务分类类型 属性 */
    private String sliType;
    /** alert 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String alertStatus;
    /** observed Value 属性 */
    private Double observedValue;
    /** target Value 属性 */
    private Double targetValue;
    /** safe Summary 属性 */
    private String safeSummary;
    /** opened At 属性 */
    private LocalDateTime openedAt;
    /** acknowledged At 属性 */
    private LocalDateTime acknowledgedAt;
    /** acknowledged By 属性 */
    private Long acknowledgedBy;
    /** recovered At 属性 */
    private LocalDateTime recoveredAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
