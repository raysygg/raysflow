package com.acme.agentstudio.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * KnowledgeChunk 数据库持久化实体对象。
 * 对应数据库中 KnowledgeChunk 数据表的字段结构映射。
 */
@TableName("knowledge_chunk")
/**
 * 数据库实体：映射表 `knowledge_chunk`，承载 KnowledgeChunk 的持久化数据。
 * <p>该对象只表示数据库记录，不承载跨实体业务流程；租户字段由应用层查询边界统一校验。</p>
 */
/**
 * KnowledgeChunk 数据表持久化实体类。
 * 映射数据库对应的 KnowledgeChunk 表结构。
 */
public class KnowledgeChunkEntity {

    /** 主键 ID 标识 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 知识文档 ID */
    private Long documentId;
    /** index Generation 主键 ID 标识 属性 */
    private Long indexGenerationId;
    /** parent Chunk 主键 ID 标识 属性 */
    private Long parentChunkId;
    /** chunk Role 属性 */
    private String chunkRole;
    /** chunk No 属性 */
    private Integer chunkNo;
    /** chunk Text 属性 */
    private String chunkText;
    /** vector Key 属性 */
    private String vectorKey;
    /** section Path 属性 */
    private String sectionPath;
    /** 当前查询页码 No 属性 */
    private Integer pageNo;
    /** token Count 属性 */
    private Integer tokenCount;
    /** 正文内容 Hash 属性 */
    private String contentHash;
    /** embedding 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String embeddingStatus;
    /** late Vector 状态标识（如 ACTIVE, DISABLED） 属性 */
    private String lateVectorStatus;
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
         * 获取getDocumentId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getDocumentId() {
        return documentId;
    }

        /**
         * 设置setDocumentId 业务逻辑处理。
         *
         * @param documentId documentId 参数
         */
    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

        /**
         * 获取getIndexGenerationId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getIndexGenerationId() { return indexGenerationId; }
        /**
         * 设置setIndexGenerationId 业务逻辑处理。
         *
         * @param indexGenerationId indexGenerationId 参数
         */
    public void setIndexGenerationId(Long indexGenerationId) { this.indexGenerationId = indexGenerationId; }
        /**
         * 获取getParentChunkId 业务逻辑处理。
         * @return Long 返回对象
         */
    public Long getParentChunkId() { return parentChunkId; }
        /**
         * 设置setParentChunkId 业务逻辑处理。
         *
         * @param parentChunkId parentChunkId 参数
         */
    public void setParentChunkId(Long parentChunkId) { this.parentChunkId = parentChunkId; }
        /**
         * 获取getChunkRole 业务逻辑处理。
         * @return String 返回对象
         */
    public String getChunkRole() { return chunkRole; }
        /**
         * 设置setChunkRole 业务逻辑处理。
         *
         * @param chunkRole chunkRole 参数
         */
    public void setChunkRole(String chunkRole) { this.chunkRole = chunkRole; }

        /**
         * 获取getChunkNo 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getChunkNo() {
        return chunkNo;
    }

        /**
         * 设置setChunkNo 业务逻辑处理。
         *
         * @param chunkNo chunkNo 参数
         */
    public void setChunkNo(Integer chunkNo) {
        this.chunkNo = chunkNo;
    }

        /**
         * 获取getChunkText 业务逻辑处理。
         * @return String 返回对象
         */
    public String getChunkText() {
        return chunkText;
    }

        /**
         * 设置setChunkText 业务逻辑处理。
         *
         * @param chunkText chunkText 参数
         */
    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

        /**
         * 获取getVectorKey 业务逻辑处理。
         * @return String 返回对象
         */
    public String getVectorKey() {
        return vectorKey;
    }

        /**
         * 设置setVectorKey 业务逻辑处理。
         *
         * @param vectorKey vectorKey 参数
         */
    public void setVectorKey(String vectorKey) {
        this.vectorKey = vectorKey;
    }

        /**
         * 获取getSectionPath 业务逻辑处理。
         * @return String 返回对象
         */
    public String getSectionPath() { return sectionPath; }
        /**
         * 设置setSectionPath 业务逻辑处理。
         *
         * @param sectionPath sectionPath 参数
         */
    public void setSectionPath(String sectionPath) { this.sectionPath = sectionPath; }
        /**
         * 获取getPageNo 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getPageNo() { return pageNo; }
        /**
         * 设置setPageNo 业务逻辑处理。
         *
         * @param pageNo pageNo 参数
         */
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
        /**
         * 获取getTokenCount 业务逻辑处理。
         * @return Integer 返回对象
         */
    public Integer getTokenCount() { return tokenCount; }
        /**
         * 设置setTokenCount 业务逻辑处理。
         *
         * @param tokenCount tokenCount 参数
         */
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
        /**
         * 获取getContentHash 业务逻辑处理。
         * @return String 返回对象
         */
    public String getContentHash() { return contentHash; }
        /**
         * 设置setContentHash 业务逻辑处理。
         *
         * @param contentHash contentHash 参数
         */
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
        /**
         * 获取getEmbeddingStatus 业务逻辑处理。
         * @return String 返回对象
         */
    public String getEmbeddingStatus() { return embeddingStatus; }
        /**
         * 设置setEmbeddingStatus 业务逻辑处理。
         *
         * @param embeddingStatus embeddingStatus 参数
         */
    public void setEmbeddingStatus(String embeddingStatus) { this.embeddingStatus = embeddingStatus; }
        /**
         * 获取getLateVectorStatus 业务逻辑处理。
         * @return String 返回对象
         */
    public String getLateVectorStatus() { return lateVectorStatus; }
        /**
         * 设置setLateVectorStatus 业务逻辑处理。
         *
         * @param lateVectorStatus lateVectorStatus 参数
         */
    public void setLateVectorStatus(String lateVectorStatus) { this.lateVectorStatus = lateVectorStatus; }

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
