package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * WebhookNonce 数据库持久化实体对象。
 * 对应数据库中 WebhookNonce 数据表的字段结构映射。
 */
@Data
@TableName("application_webhook_nonce")
/**
 * WebhookNonce 数据表持久化实体类。
 * 映射数据库对应的 WebhookNonce 表结构。
 */
public class WebhookNonceEntity {
    /** entrypoint 主键 ID 标识 属性 */
    @TableId(type = IdType.AUTO) private Long id;
    private Long entrypointId;
    /** nonce Hash 属性 */
    private String nonceHash;
    /** expires At 属性 */
    private LocalDateTime expiresAt;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
