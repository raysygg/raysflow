package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 中英混合语言词法分析策略实现类（Mixed Lexical Strategy）。
 * 混合策略并行保留中文和英文特征，避免只选择一种 tokenizer。
 */
@Component
public class MixedLexicalStrategy implements MultilingualLexicalStrategy {
    private final ChineseLexicalStrategy chineseStrategy;
    private final EnglishLexicalStrategy englishStrategy;

    public MixedLexicalStrategy(ChineseLexicalStrategy chineseStrategy,
                                EnglishLexicalStrategy englishStrategy) {
        this.chineseStrategy = chineseStrategy;
        this.englishStrategy = englishStrategy;
    }

        /**
         * language 方法。
         * @return KnowledgeLanguage 返回对象
         */
    @Override
    public KnowledgeLanguage language() {
        return KnowledgeLanguage.MIXED;
    }

        /**
         * tokenize 方法。
         *
         * @param normalizedText normalizedText 参数
         * @return List<String> 返回对象
         */
    @Override
    public List<String> tokenize(String normalizedText) {
        List<String> tokens = new ArrayList<>(chineseStrategy.tokenize(normalizedText));
        tokens.addAll(englishStrategy.tokenize(normalizedText));
        return List.copyOf(tokens);
    }
}
