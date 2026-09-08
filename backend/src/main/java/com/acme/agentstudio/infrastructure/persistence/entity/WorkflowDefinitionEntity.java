package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * WorkflowDefinition 数据库持久化实体对象。
 * 对应数据库中 WorkflowDefinition 数据表的字段结构映射。
 */
@TableName("workflow_definition")
/**
 * 数据库实体：映射表 `workflow_definition`，承载 WorkflowDefinition 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * WorkflowDefinition 数据表持久化实体类。
 * 映射数据库对应的 WorkflowDefinition 表结构。
 */
public class WorkflowDefinitionEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户全局唯一标识 ID */
    private Long tenantId;
    /** 工作流编码 */
    private String workflowCode;
    /** 工作流名称 */
    private String workflowName;
    /** 运行版本标识 No 属性 */
    private Integer versionNo;
    /** 状态标识（如 ACTIVE, DISABLED） */
    private String status;
    /** 画布节点连线结构 JSON 字符串 */
    private String graphJson;
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
         * 获取getWorkflowName 业务逻辑处理。
         * @return String 返回对象
         */
    public String getWorkflowName() {
        return workflowName;
    }

        /**
         * 设置setWorkflowName 业务逻辑处理。
         *
         * @param workflowName workflowName 参数
         */
    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

        /**
         * 获取getVersionNo 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getVersionNo() {
        return versionNo;
    }

        /**
         * 设置setVersionNo 业务逻辑处理。
         *
         * @param versionNo versionNo 参数
         */
    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
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
         * 获取getGraphJson 业务逻辑处理。
         * @return String 返回对象
         */
    public String getGraphJson() {
        return graphJson;
    }

        /**
         * 设置setGraphJson 业务逻辑处理。
         *
         * @param graphJson graphJson 参数
         */
    public void setGraphJson(String graphJson) {
        this.graphJson = graphJson;
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
