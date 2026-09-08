package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * RuntimeDeadLetterDisposition 数据库持久化实体对象。
 * 对应数据库中 RuntimeDeadLetterDisposition 数据表的字段结构映射。
 */
@Data
@TableName("runtime_dead_letter_disposition")
/**
 * RuntimeDeadLetterDisposition 数据表持久化实体类。
 * 映射数据库对应的 RuntimeDeadLetterDisposition 表结构。
 */
public class RuntimeDeadLetterDispositionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long tenantId; private Long deadLetterId; private String dispositionType;
    private String previousStatus; private String nextStatus; private String reason; private Long operatorId;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
}
