package com.acme.agentstudio.infrastructure.rag.parser;

import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * CSV 表格数据文档解析器实现类（CSV Knowledge Document Parser）。
 * CSV 表格处理器，将列名和单元格值转换为保留字段语义的文本。
 */
@Component
public class CsvKnowledgeDocumentParser implements KnowledgeDocumentParser {
    private static final Set<String> EXTENSIONS = Set.of("csv");
    private static final char SEPARATOR = ',';
    private static final String EMPTY_VALUE = "（空）";
    private static final String UNNAMED_COLUMN_PREFIX = "列";

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
        List<List<String>> rows = parseRows(content);
        if (rows.isEmpty()) throw new IllegalArgumentException("CSV 文件为空，无法建立知识索引。");

        List<String> headers = rows.get(0);
        StringBuilder text = new StringBuilder();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            text.append("第").append(rowIndex).append("行：");
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String header = columnIndex < headers.size() && !headers.get(columnIndex).isBlank()
                        ? headers.get(columnIndex).trim() : UNNAMED_COLUMN_PREFIX + (columnIndex + 1);
                String value = row.get(columnIndex).isBlank() ? EMPTY_VALUE : row.get(columnIndex).trim();
                text.append(header).append("：").append(value);
                if (columnIndex < row.size() - 1) text.append("；");
            }
            text.append('\n');
        }
        if (text.length() == 0) throw new IllegalArgumentException("CSV 文件只有表头，没有可索引的数据行。");
        return Document.from(text.toString());
    }

    /** 解析常见 RFC 4180 格式，支持字段内逗号、换行和双引号转义。 */
    private List<List<String>> parseRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == SEPARATOR && !quoted) {
                row.add(value.toString());
                value.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') index++;
                row.add(value.toString());
                value.setLength(0);
                if (row.stream().anyMatch(item -> !item.isBlank())) rows.add(row);
                row = new ArrayList<>();
            } else {
                value.append(current);
            }
        }
        if (quoted) throw new IllegalArgumentException("CSV 文件存在未闭合的引号，无法解析。");
        if (value.length() > 0 || !row.isEmpty()) {
            row.add(value.toString());
            rows.add(row);
        }
        return rows;
    }
}
