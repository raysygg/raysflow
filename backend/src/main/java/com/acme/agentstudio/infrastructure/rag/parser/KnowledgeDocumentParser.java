package com.acme.agentstudio.infrastructure.rag.parser;

import dev.langchain4j.data.document.Document;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * 知识文档物理文件解析器统一 SPI 协议接口（Knowledge Document Parser）。
 * 每种不同物理文件格式（Word, Excel, PDF, CSV, HTML, Text 等）由独立解析器实现负责文本提取与格式规范化。
 */
public interface KnowledgeDocumentParser {

    /**
     * 返回该解析器支持的文件扩展名集合（全小写，不含点号）。
     *
     * @return 扩展名集合 Set&lt;String&gt;
     */
    Set<String> supportedExtensions();

    /**
     * 将物理路径文件解析提取为统一的 Document 文本与元数据对象。
     *
     * @param filePath 文件物理路径
     * @param originalName 原始上传文件名
     * @return 解析后的 Document 对象
     * @throws IOException 当文件读取或解析抛错时抛出
     */
    Document parse(Path filePath, String originalName) throws IOException;

    /**
     * 判断当前解析器是否支持处理指定的扩展名。
     *
     * @param extension 文件扩展名
     * @return 若支持返回 true
     */
    default boolean supports(String extension) {
        return supportedExtensions().contains(extension);
    }
}

