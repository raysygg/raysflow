package com.acme.agentstudio.domain.common;

/**
 * 平台各业务实体（文档、Agent、工作流、消息）通用生命周期状态常量集中定义类（Business Status）。
 */
public final class BusinessStatus {

    /** 在线正常活跃状态："ACTIVE" */
    public static final String ACTIVE = "ACTIVE";

    /** 草稿编辑中状态："DRAFT" */
    public static final String DRAFT = "DRAFT";

    /** 挂起排队等待状态："PENDING" */
    public static final String PENDING = "PENDING";

    /** 向量索引生成构建中："INDEXING" */
    public static final String INDEXING = "INDEXING";

    /** 向量索引构建完成就绪："INDEXED" */
    public static final String INDEXED = "INDEXED";

    /** 向量索引构建失败："INDEX_FAILED" */
    public static final String INDEX_FAILED = "INDEX_FAILED";

    /** 需要重建索引："REINDEX_REQUIRED" */
    public static final String REINDEX_REQUIRED = "REINDEX_REQUIRED";

    /** 已逻辑删除："DELETED" */
    public static final String DELETED = "DELETED";

    /** 已发布生效："PUBLISHED" */
    public static final String PUBLISHED = "PUBLISHED";

    /** 已停用："DISABLED" */
    public static final String DISABLED = "DISABLED";

    /** 已归档历史记录："ARCHIVED" */
    public static final String ARCHIVED = "ARCHIVED";

    /** 私有构造函数，防止工具类被实例化 */
    private BusinessStatus() {
    }
}

