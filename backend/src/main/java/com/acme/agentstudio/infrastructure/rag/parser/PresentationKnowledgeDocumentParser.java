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
 * PowerPoint 演示文稿解析器实现类（Presentation Knowledge Document Parser）。
 * 演示文稿处理器，支持 PPT/PPTX 并提取幻灯片文本。
 */
@Component
public class PresentationKnowledgeDocumentParser implements KnowledgeDocumentParser {
    private static final Set<String> EXTENSIONS = Set.of("ppt", "pptx");

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
