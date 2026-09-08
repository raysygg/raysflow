package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 中文文本词法分析策略实现类（Chinese Lexical Strategy）。
 * 中文策略保留连续汉字词，并补充字和双字特征，兼顾短词与未登录词。
 */
@Component
public class ChineseLexicalStrategy implements MultilingualLexicalStrategy {
    private static final Set<String> STOP_WORDS = Set.of("的", "了", "在", "是", "我", "有", "和", "就", "不", "都", "请问", "帮我");

        /**
         * language 方法。
         * @return KnowledgeLanguage 返回对象
         */
    @Override
    public KnowledgeLanguage language() {
        return KnowledgeLanguage.ZH;
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
        for (String segment : normalizedText.split("[^\\p{IsHan}]+")) {
            if (segment.isBlank() || STOP_WORDS.contains(segment)) continue;
            tokens.add(segment);
            int[] codePoints = segment.codePoints().toArray();
            for (int codePoint : codePoints) tokens.add(new String(Character.toChars(codePoint)));
            for (int index = 0; index + 1 < codePoints.length; index++) {
                tokens.add(new String(codePoints, index, 2));
            }
        }
        return List.copyOf(tokens);
    }
}
