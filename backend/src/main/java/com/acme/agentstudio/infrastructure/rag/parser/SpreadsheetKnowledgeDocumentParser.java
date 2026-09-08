package com.acme.agentstudio.infrastructure.rag.parser;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Excel 电子表格文档解析器实现类（Spreadsheet Knowledge Document Parser）。
 * Excel 表格处理器，支持 XLS/XLSX 并交给 Tika 提取工作表文本。
 */
@Component
public class SpreadsheetKnowledgeDocumentParser implements KnowledgeDocumentParser {
    private static final Set<String> EXTENSIONS = Set.of("xls", "xlsx");

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
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return new ApacheTikaDocumentParser().parse(inputStream);
        }
    }
}
