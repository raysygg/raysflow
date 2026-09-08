package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * SaasInvoiceLine 数据库持久化实体对象。
 * 对应数据库中 SaasInvoiceLine 数据表的字段结构映射。
 */
@Data @TableName("saas_invoice_line")
/**
 * SaasInvoiceLine 数据表持久化实体类。
 * 映射数据库对应的 SaasInvoiceLine 表结构。
 */
public class SaasInvoiceLineEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long invoiceId;
    private String featureCode; private String costCenter; private BigDecimal quantity; private String unit;
    private BigDecimal unitPrice; private BigDecimal amount; private String sourceSummary; private LocalDateTime createdAt;
}
