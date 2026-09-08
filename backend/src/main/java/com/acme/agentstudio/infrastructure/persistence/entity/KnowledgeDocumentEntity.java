package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * KnowledgeDocument 数据库持久化实体对象。
 * 对应数据库中 KnowledgeDocument 数据表的字段结构映射。
 */
@TableName("knowledge_document")
/**
 * 数据库实体：映射表 `knowledge_document`，承载 KnowledgeDocument 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * KnowledgeDocument 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeDocument 表结构。
 */
public class KnowledgeDocumentEntity {

    @TableId(type = IdType.AUTO)
    /** 文档主键。 */
    private Long id;
    /** 所属租户主键。 */
    private Long tenantId;
    /** 文档标题。 */
    private String title;
    /** 文档语言；历史未确认文档迁移为 OTHER。 */
    private String language;
    /** 语言是否经过用户或导入流程确认。 */
    private Boolean languageConfirmed;
    /** 文档来源类型，例如上传文件或外部连接器。 */
    private String sourceType;
    /** 原始文件或对象存储路径。 */
    private String filePath;
    /** 文档索引状态。 */
    private String documentStatus;
    /** 文档版本号。 */
    private Integer versionNo;
    /** 当前文档切分后的分块数量。 */
    private Integer chunkCount;
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
         * 获取getTitle 业务逻辑处理。
         * @return String 返回对象
         */
    public String getTitle() {
        return title;
    }

        /**
         * 设置setTitle 业务逻辑处理。
         *
         * @param title title 参数
         */
    public void setTitle(String title) {
        this.title = title;
    }

        /**
         * 获取getLanguage 业务逻辑处理。
         * @return String 返回对象
         */
    public String getLanguage() {
        return language;
    }

        /**
         * 设置setLanguage 业务逻辑处理。
         *
         * @param language language 参数
         */
    public void setLanguage(String language) {
        this.language = language;
    }

        /**
         * 获取getLanguageConfirmed 业务逻辑处理。
         * @return Boolean 返回对象
         */
    public Boolean getLanguageConfirmed() {
        return languageConfirmed;
    }

        /**
         * 设置setLanguageConfirmed 业务逻辑处理。
         *
         * @param languageConfirmed languageConfirmed 参数
         */
    public void setLanguageConfirmed(Boolean languageConfirmed) {
        this.languageConfirmed = languageConfirmed;
    }

        /**
         * 获取getSourceType 业务逻辑处理。
         * @return String 返回对象
         */
    public String getSourceType() {
        return sourceType;
    }

        /**
         * 设置setSourceType 业务逻辑处理。
         *
         * @param sourceType sourceType 参数
         */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

        /**
         * 获取getFilePath 业务逻辑处理。
         * @return String 返回对象
         */
    public String getFilePath() {
        return filePath;
    }

        /**
         * 设置setFilePath 业务逻辑处理。
         *
         * @param filePath filePath 参数
         */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

        /**
         * 获取getDocumentStatus 业务逻辑处理。
         * @return String 返回对象
         */
    public String getDocumentStatus() {
        return documentStatus;
    }

        /**
         * 设置setDocumentStatus 业务逻辑处理。
         *
         * @param documentStatus documentStatus 参数
         */
    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
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
         * 获取getChunkCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getChunkCount() {
        return chunkCount;
    }

        /**
         * 设置setChunkCount 业务逻辑处理。
         *
         * @param chunkCount chunkCount 参数
         */
    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
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
