package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SaasInvoice 数据库持久化实体对象。
 * 对应数据库中 SaasInvoice 数据表的字段结构映射。
 */
@Data
@TableName("saas_invoice")
/**
 * SaasInvoice 数据表持久化实体类。
 * 映射数据库对应的 SaasInvoice 表结构。
 */
public class SaasInvoiceEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long billingPeriodId; private String invoiceNumber; private String status;
    private String currency; private BigDecimal subtotal; private BigDecimal total; private String snapshotHash;
    private LocalDateTime finalizedAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
