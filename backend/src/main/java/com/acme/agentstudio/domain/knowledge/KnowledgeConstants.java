package com.acme.agentstudio.domain.knowledge;

import java.util.Set;

/**
 * 企业知识库领域核心常量定义类（Knowledge Constants）。
 * 集中约定上传文件名、支持的文件后缀类型、Qdrant Payload 元数据属性 Key 等。
 */
public final class KnowledgeConstants {

    /** 默认上传文件名："document.txt" */
    public static final String DEFAULT_UPLOAD_FILENAME = "document.txt";

    /** 默认文档标题："未命名文档" */
    public static final String DEFAULT_DOCUMENT_TITLE = "未命名文档";

    /** 纯文本知识源类型标识："TEXT" */
    public static final String SOURCE_TYPE_TEXT = "TEXT";

    /** CSV 表格知识源类型标识："CSV" */
    public static final String SOURCE_TYPE_CSV = "CSV";

    /** CSV 文件后缀名："csv" */
    public static final String CSV_EXTENSION = "csv";

    /** 平台支持解析的文档扩展名集合 */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", CSV_EXTENSION, "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm"
    );

    /** 支持的文件格式中文说明提示 */
    public static final String SUPPORTED_FORMAT_LABEL = "TXT、Markdown、CSV、PDF、Word、Excel、PPT 或 HTML";

    /** 手动调试预览知识源："manual-preview" */
    public static final String MANUAL_PREVIEW_SOURCE = "manual-preview";

    /** Qdrant Payload 字段：文档 ID ("document_id") */
    public static final String METADATA_DOCUMENT_ID = "document_id";

    /** Qdrant Payload 字段：知识来源 ("source") */
    public static final String METADATA_SOURCE = "source";

    /** Qdrant Payload 字段：切块序号 ("chunk_no") */
    public static final String METADATA_CHUNK_NO = "chunk_no";

    /** Qdrant Payload 字段：租户 ID ("tenant_id") */
    public static final String METADATA_TENANT_ID = "tenant_id";

    /** Qdrant Payload 字段：识别语种 ("language") */
    public static final String METADATA_LANGUAGE = "language";

    /** Qdrant Payload 字段：语种人工确认标志 ("language_confirmed") */
    public static final String METADATA_LANGUAGE_CONFIRMED = "language_confirmed";

    /** 私有构造函数，防止工具类被实例化 */
    private KnowledgeConstants() {
    }
}

