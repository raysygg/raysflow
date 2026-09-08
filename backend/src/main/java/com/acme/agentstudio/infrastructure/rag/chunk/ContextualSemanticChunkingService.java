package com.acme.agentstudio.infrastructure.rag.chunk;

import com.acme.agentstudio.config.RagProperties;
import com.acme.agentstudio.domain.knowledge.model.SemanticChunkPlan;
import com.acme.agentstudio.domain.knowledge.port.SemanticChunkingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * ContextualSemanticChunking 业务服务接口。
 * 定义 ContextualSemanticChunking 相关的核心业务契约与流程接口。
 */
@Component
/**
 * ContextualSemanticChunking 业务逻辑服务接口。
 * 负责 ContextualSemanticChunking 核心业务逻辑与流程编排。
 */
public class ContextualSemanticChunkingService implements SemanticChunkingService {
    private static final String DEFAULT_SECTION = "正文";
    private final RagProperties properties;

    public ContextualSemanticChunkingService(RagProperties properties) {
        this.properties = properties;
    }

        /**
         * split 方法。
         *
         * @param document document 参数
         * @param documentTitle documentTitle 参数
         * @return SemanticChunkPlan 返回对象
         */
    @Override
    public SemanticChunkPlan split(Document document, String documentTitle) {
        if (document == null || document.text() == null || document.text().isBlank()) {
            throw new IllegalArgumentException("知识文档没有可索引的文本内容。");
        }
        List<ParentDraft> parentDrafts = parentDrafts(document.text());
        List<SemanticChunkPlan.ParentChunk> parents = new ArrayList<>();
        List<SemanticChunkPlan.ChildChunk> children = new ArrayList<>();
        int childOrdinal = 1;
        for (int parentIndex = 0; parentIndex < parentDrafts.size(); parentIndex++) {
            ParentDraft parent = parentDrafts.get(parentIndex);
            int parentOrdinal = parentIndex + 1;
            parents.add(new SemanticChunkPlan.ParentChunk(parentOrdinal, parent.text(), parent.sectionPath(), null,
                    estimateTokens(parent.text()), hash(parent.text())));
            Document parentDocument = Document.from(parent.text());
            List<dev.langchain4j.data.segment.TextSegment> segments = DocumentSplitters.recursive(
                    properties.getChildChunkSize(), properties.getChildChunkOverlap()).split(parentDocument);
            for (dev.langchain4j.data.segment.TextSegment segment : segments) {
                String text = segment.text().trim();
                if (text.isBlank()) continue;
                String embeddingText = contextualText(documentTitle, parent.sectionPath(), text);
                children.add(new SemanticChunkPlan.ChildChunk(childOrdinal++, parentOrdinal, text, embeddingText,
                        parent.sectionPath(), null, estimateTokens(text), hash(text)));
            }
        }
        return new SemanticChunkPlan(parents, children);
    }

    private List<ParentDraft> parentDrafts(String text) {
        String[] blocks = text.replace("\r\n", "\n").split("\n\\s*\n");
        List<ParentDraft> parents = new ArrayList<>();
        String currentSection = DEFAULT_SECTION;
        String bufferSection = currentSection;
        StringBuilder buffer = new StringBuilder();
        for (String raw : blocks) {
            String block = raw.trim();
            if (block.isBlank()) continue;
            if (isHeading(block)) {
                if (!buffer.isEmpty()) {
                    parents.add(new ParentDraft(buffer.toString(), bufferSection));
                    buffer.setLength(0);
                }
                currentSection = headingText(block);
                bufferSection = currentSection;
            }
            if (!buffer.isEmpty() && buffer.length() + block.length() + 2 > properties.getParentChunkSize()) {
                parents.add(new ParentDraft(buffer.toString(), bufferSection));
                buffer.setLength(0);
                bufferSection = currentSection;
            }
            if (!buffer.isEmpty()) buffer.append("\n\n");
            buffer.append(block);
        }
        if (!buffer.isEmpty()) parents.add(new ParentDraft(buffer.toString(), bufferSection));
        return parents;
    }

    private boolean isHeading(String block) {
        return block.lines().count() == 1 && block.startsWith("#") && block.length() <= 200;
    }

    private String headingText(String heading) {
        String value = heading.replaceFirst("^#+\\s*", "").trim();
        return value.isBlank() ? DEFAULT_SECTION : value;
    }

    private String contextualText(String title, String sectionPath, String text) {
        return "文档：" + safe(title) + "\n章节：" + safe(sectionPath) + "\n内容：" + text;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未命名" : value.trim();
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 3.0D));
    }

    private String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("知识切块内容哈希计算失败。", exception);
        }
    }

    private record ParentDraft(String text, String sectionPath) {
    }
}
