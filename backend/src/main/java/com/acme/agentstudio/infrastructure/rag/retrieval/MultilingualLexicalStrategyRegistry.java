package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 多语言分词与词法分析策略注册表组件（Multilingual Lexical Strategy Registry）。
 * 根据实际脚本和语种自动路由与选择对应的 Sparse 词法分析 Strategy 实例。
 */
@Component
public class MultilingualLexicalStrategyRegistry {
    private final Map<KnowledgeLanguage, MultilingualLexicalStrategy> strategies;

    @Autowired
    public MultilingualLexicalStrategyRegistry(List<MultilingualLexicalStrategy> strategies) {
        EnumMap<KnowledgeLanguage, MultilingualLexicalStrategy> registry = new EnumMap<>(KnowledgeLanguage.class);
        strategies.forEach(strategy -> registry.put(strategy.language(), strategy));
        if (!registry.containsKey(KnowledgeLanguage.OTHER)) {
            throw new IllegalStateException("Sparse 策略注册表必须提供 OTHER 兜底策略。");
        }
        this.strategies = Map.copyOf(registry);
    }

    public MultilingualLexicalStrategyRegistry() {
        this(defaultStrategies());
    }

        /**
         * tokenize 方法。
         *
         * @param value value 参数
         * @return List<String> 返回对象
         */
    public List<String> tokenize(String value) {
        if (value == null || value.isBlank()) return List.of();
        String normalized = normalize(value);
        KnowledgeLanguage language = detect(normalized);
        return strategies.getOrDefault(language, strategies.get(KnowledgeLanguage.OTHER)).tokenize(normalized);
    }

        /**
         * detect 方法。
         *
         * @param value value 参数
         * @return KnowledgeLanguage 返回对象
         */
    public KnowledgeLanguage detect(String value) {
        long han = value.codePoints().filter(codePoint -> Character.UnicodeScript.of(codePoint)
                == Character.UnicodeScript.HAN).count();
        long latin = value.codePoints().filter(codePoint -> Character.UnicodeScript.of(codePoint)
                == Character.UnicodeScript.LATIN).count();
        if (han > 0 && latin > 0) return KnowledgeLanguage.MIXED;
        if (han > 0) return KnowledgeLanguage.ZH;
        if (latin > 0) return KnowledgeLanguage.EN;
        return KnowledgeLanguage.OTHER;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static List<MultilingualLexicalStrategy> defaultStrategies() {
        ChineseLexicalStrategy chinese = new ChineseLexicalStrategy();
        EnglishLexicalStrategy english = new EnglishLexicalStrategy();
        return List.of(chinese, english, new MixedLexicalStrategy(chinese, english), new OtherLexicalStrategy());
    }
}
