package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * ChatMessage 数据库持久化实体对象。
 * 对应数据库中 ChatMessage 数据表的字段结构映射。
 */
@TableName("chat_message")
/**
 * 数据库实体：映射表 `chat_message`，承载 ChatMessage 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * ChatMessage 数据表持久化实体类。
 * 映射数据库对应的 ChatMessage 表结构。
 */
public class ChatMessageEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** session 主键 ID 标识 属性 */
    private Long sessionId;
    /** message Role 属性 */
    private String messageRole;
    /** message Text 属性 */
    private String messageText;
    /** reference Json 属性 */
    private String referenceJson;
    /** tool Trace Json 属性 */
    private String toolTraceJson;
    /** 数据创建时间 */
    private LocalDateTime createdAt;

        /**
         * 获取getId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getId() {
        return id;
    }

        /**
         * 设置setId 业务逻辑处理。
         *
         * @param id id 参数
         */
    public void setId(Long id) {
        this.id = id;
    }

        /**
         * 获取getSessionId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getSessionId() {
        return sessionId;
    }

        /**
         * 设置setSessionId 业务逻辑处理。
         *
         * @param sessionId sessionId 参数
         */
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

        /**
         * 获取getMessageRole 业务逻辑处理。
         * @return String 返回对象
         */
    public String getMessageRole() {
        return messageRole;
    }

        /**
         * 设置setMessageRole 业务逻辑处理。
         *
         * @param messageRole messageRole 参数
         */
    public void setMessageRole(String messageRole) {
        this.messageRole = messageRole;
    }

        /**
         * 获取getMessageText 业务逻辑处理。
         * @return String 返回对象
         */
    public String getMessageText() {
        return messageText;
    }

        /**
         * 设置setMessageText 业务逻辑处理。
         *
         * @param messageText messageText 参数
         */
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

        /**
         * 获取getReferenceJson 业务逻辑处理。
         * @return String 返回对象
         */
    public String getReferenceJson() {
        return referenceJson;
    }

        /**
         * 设置setReferenceJson 业务逻辑处理。
         *
         * @param referenceJson referenceJson 参数
         */
    public void setReferenceJson(String referenceJson) {
        this.referenceJson = referenceJson;
    }

        /**
         * 获取getToolTraceJson 业务逻辑处理。
         * @return String 返回对象
         */
    public String getToolTraceJson() {
        return toolTraceJson;
    }

        /**
         * 设置setToolTraceJson 业务逻辑处理。
         *
         * @param toolTraceJson toolTraceJson 参数
         */
    public void setToolTraceJson(String toolTraceJson) {
        this.toolTraceJson = toolTraceJson;
    }

        /**
         * 获取getCreatedAt 业务逻辑处理。
         * @return LocalDateTime 返回对象
         */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

        /**
         * 设置setCreatedAt 业务逻辑处理。
         *
         * @param createdAt createdAt 参数
         */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
