package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * ChatSession 数据库持久化实体对象。
 * 对应数据库中 ChatSession 数据表的字段结构映射。
 */
@TableName("chat_session")
/**
 * 数据库实体：映射表 `chat_session`，承载 ChatSession 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * ChatSession 数据表持久化实体类。
 * 映射数据库对应的 ChatSession 表结构。
 */
public class ChatSessionEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** agent 主键 ID 标识 属性 */
    private Long agentId;
    /** session 展示名称 属性 */
    private String sessionName;
    /** 创建人唯一标识 */
    private String createdBy;
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
         * 获取getTenantId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getTenantId() {
        return tenantId;
    }

        /**
         * 设置setTenantId 业务逻辑处理。
         *
         * @param tenantId tenantId 参数
         */
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

        /**
         * 获取getAgentId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getAgentId() {
        return agentId;
    }

        /**
         * 设置setAgentId 业务逻辑处理。
         *
         * @param agentId agentId 参数
         */
    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

        /**
         * 获取getSessionName 业务逻辑处理。
         * @return String 返回对象
         */
    public String getSessionName() {
        return sessionName;
    }

        /**
         * 设置setSessionName 业务逻辑处理。
         *
         * @param sessionName sessionName 参数
         */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

        /**
         * 获取getCreatedBy 业务逻辑处理。
         * @return String 返回对象
         */
    public String getCreatedBy() {
        return createdBy;
    }

        /**
         * 设置setCreatedBy 业务逻辑处理。
         *
         * @param createdBy createdBy 参数
         */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
