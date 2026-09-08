package com.acme.agentstudio.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * PlatformConversationMessage 数据库持久化实体对象。
 * 对应数据库中 PlatformConversationMessage 数据表的字段结构映射。
 */
@Data @TableName("platform_conversation_message")
/**
 * 数据库实体：映射表 `platform_conversation_message`，承载 PlatformConversationMessage 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * PlatformConversationMessage 数据表持久化实体类。
 * 映射数据库对应的 PlatformConversationMessage 表结构。
 */
public class PlatformConversationMessageEntity {
    @TableId(type = IdType.AUTO) private Long id; private Long tenantId; private String conversationId; private String messageId;
    private String executionId; private String roleCode; private String contentJson; private String status; private LocalDateTime createdAt;
}
