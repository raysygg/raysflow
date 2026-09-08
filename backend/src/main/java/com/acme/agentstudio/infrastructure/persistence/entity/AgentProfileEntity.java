package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("agent_profile")
/**
 * 数据库实体：映射表 `agent_profile`，承载 AgentProfile 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
public class AgentProfileEntity {

    @TableId(type = IdType.AUTO)
    /** Agent 主键。 */
    private Long id;
    /** 所属租户主键。 */
    private Long tenantId;
    /** Agent 稳定业务编码。 */
    private String agentCode;
    /** Agent 展示名称。 */
    private String agentName;
    /** Agent 类型编码，类型目录由数据库维护。 */
    private String agentType;
    /** 负责团队或组织标识。 */
    private String ownerTeam;
    /** Agent 使用的提示词模板。 */
    private String promptTemplate;
    /** 默认模型编码，实际模型配置从数据库解析。 */
    private String modelKey;
    /** 工具能力摘要。 */
    private String toolSummary;
    /** 绑定的工作流编码，可为空。 */
    private String workflowCode;
    /** Agent 生命周期状态。 */
    private String status;
    /** 数据创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

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
         * 获取getAgentCode 业务逻辑处理。
         * @return String 返回对象
         */
    public String getAgentCode() {
        return agentCode;
    }

        /**
         * 设置setAgentCode 业务逻辑处理。
         *
         * @param agentCode agentCode 参数
         */
    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

        /**
         * 获取getAgentName 业务逻辑处理。
         * @return String 返回对象
         */
    public String getAgentName() {
        return agentName;
    }

        /**
         * 设置setAgentName 业务逻辑处理。
         *
         * @param agentName agentName 参数
         */
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

        /**
         * 获取getAgentType 业务逻辑处理。
         * @return String 返回对象
         */
    public String getAgentType() {
        return agentType;
    }

        /**
         * 设置setAgentType 业务逻辑处理。
         *
         * @param agentType agentType 参数
         */
    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

        /**
         * 获取getOwnerTeam 业务逻辑处理。
         * @return String 返回对象
         */
    public String getOwnerTeam() {
        return ownerTeam;
    }

        /**
         * 设置setOwnerTeam 业务逻辑处理。
         *
         * @param ownerTeam ownerTeam 参数
         */
    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

        /**
         * 获取getPromptTemplate 业务逻辑处理。
         * @return String 返回对象
         */
    public String getPromptTemplate() {
        return promptTemplate;
    }

        /**
         * 设置setPromptTemplate 业务逻辑处理。
         *
         * @param promptTemplate promptTemplate 参数
         */
    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

        /**
         * 获取getModelKey 业务逻辑处理。
         * @return String 返回对象
         */
    public String getModelKey() {
        return modelKey;
    }

        /**
         * 设置setModelKey 业务逻辑处理。
         *
         * @param modelKey modelKey 参数
         */
    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

        /**
         * 获取getToolSummary 业务逻辑处理。
         * @return String 返回对象
         */
    public String getToolSummary() {
        return toolSummary;
    }

        /**
         * 设置setToolSummary 业务逻辑处理。
         *
         * @param toolSummary toolSummary 参数
         */
    public void setToolSummary(String toolSummary) {
        this.toolSummary = toolSummary;
    }

        /**
         * 获取getWorkflowCode 业务逻辑处理。
         * @return String 返回对象
         */
    public String getWorkflowCode() {
        return workflowCode;
    }

        /**
         * 设置setWorkflowCode 业务逻辑处理。
         *
         * @param workflowCode workflowCode 参数
         */
    public void setWorkflowCode(String workflowCode) {
        this.workflowCode = workflowCode;
    }

        /**
         * 获取getStatus 业务逻辑处理。
         * @return String 返回对象
         */
    public String getStatus() {
        return status;
    }

        /**
         * 设置setStatus 业务逻辑处理。
         *
         * @param status status 参数
         */
    public void setStatus(String status) {
        this.status = status;
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

        /**
         * 获取getUpdatedAt 业务逻辑处理。
         * @return LocalDateTime 返回对象
         */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

        /**
         * 设置setUpdatedAt 业务逻辑处理。
         *
         * @param updatedAt updatedAt 参数
         */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
