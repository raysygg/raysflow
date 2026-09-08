package com.acme.agentstudio.infrastructure.rag.parser;

import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * TXT 与 Markdown 纯文本文档解析器实现类（Text Knowledge Document Parser）。
 * TXT 和 Markdown 文本处理器，保留原文结构交给后续切片器处理。
 */
@Component
public class TextKnowledgeDocumentParser implements KnowledgeDocumentParser {
    private static final Set<String> EXTENSIONS = Set.of("txt", "md", "markdown");

        /**
         * supportedExtensions 方法。
         * @return Set<String> 返回对象
         */
    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

        /**
         * 解析parse 业务逻辑处理。
         *
         * @param filePath filePath 参数
         * @param originalName originalName 参数
         * @return Document 返回对象
         */
    @Override
    public Document parse(Path filePath, String originalName) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        if (content.isBlank()) throw new IllegalArgumentException("文本文件为空，无法建立知识索引。");
        return Document.from(content);
    }
}
