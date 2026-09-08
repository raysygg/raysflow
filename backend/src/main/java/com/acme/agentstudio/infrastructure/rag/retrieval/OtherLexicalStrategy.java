package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 其他/兜底语种 N-Gram 词法分析策略实现类（Other Lexical Strategy）。
 * 未覆盖脚本使用有界字符 N-Gram，不隐式套用中文规则。
 */
@Component
public class OtherLexicalStrategy implements MultilingualLexicalStrategy {
    private static final int NGRAM_SIZE = 2;

        /**
         * language 方法。
         * @return KnowledgeLanguage 返回对象
         */
    @Override
    public KnowledgeLanguage language() {
        return KnowledgeLanguage.OTHER;
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
        for (String segment : normalizedText.split("[^\\p{L}\\p{Nd}]+")) {
            if (segment.isBlank()) continue;
            tokens.add(segment);
            int[] codePoints = segment.codePoints().toArray();
            for (int index = 0; index + NGRAM_SIZE <= codePoints.length; index++) {
                tokens.add(new String(codePoints, index, NGRAM_SIZE));
            }
        }
        return List.copyOf(tokens);
    }
}
