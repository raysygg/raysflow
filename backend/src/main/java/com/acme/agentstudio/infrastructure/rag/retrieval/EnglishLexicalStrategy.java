package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 英文文本词法分析策略实现类（English Lexical Strategy）。
 * 英文策略负责规范化后的单词、数字和业务术语提取。
 */
@Component
public class EnglishLexicalStrategy implements MultilingualLexicalStrategy {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "for", "on", "is", "are");

        /**
         * language 方法。
         * @return KnowledgeLanguage 返回对象
         */
    @Override
    public KnowledgeLanguage language() {
        return KnowledgeLanguage.EN;
    }

        /**
         * tokenize 方法。
         *
         * @param normalizedText normalizedText 参数
         * @return List<String> 返回对象
         */
    @Override
    public List<String> tokenize(String normalizedText) {
        List<String> tokens = new ArrayList<>();
        for (String token : normalizedText.split("[^\\p{IsLatin}\\p{Nd}]+")) {
            if (!token.isBlank() && !STOP_WORDS.contains(token)) tokens.add(token);
        }
        return List.copyOf(tokens);
    }
}
