package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * SaasBillingNote 数据库持久化实体对象。
 * 对应数据库中 SaasBillingNote 数据表的字段结构映射。
 */
@Data @TableName("saas_billing_note")
/**
 * SaasBillingNote 数据表持久化实体类。
 * 映射数据库对应的 SaasBillingNote 表结构。
 */
public class SaasBillingNoteEntity {
    @TableId(type=IdType.AUTO) private Long id; private Long tenantId; private Long invoiceId;
    private String noteNumber; private String noteType; private BigDecimal amount; private String currency;
    private String reason; private Long createdBy; private LocalDateTime createdAt;
}
