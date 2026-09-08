package com.acme.agentstudio.infrastructure.rag.retrieval;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 精确特征（订单号、标识符、URL、固定短语）抽取器实现类（Exact Feature Extractor）。
 * 与语言策略无关的业务编号、代码、URL 和完整短语提取器。
 */
@Component
public class ExactFeatureExtractor {
    private static final Pattern BUSINESS_ID = Pattern.compile("(?i)[A-Z0-9]{2,}(?:[-_/.:][A-Z0-9]+)+");
    private static final int MIN_FEATURE_LENGTH = 2;
    private static final int MAX_PHRASE_LENGTH = 80;

    /**
     * 抽取文本中的精确关键词与业务编号列表。
     *
     * @param value 原始文本
     * @return 提取出的特征 Key 字符串列表
     */
    public List<String> extract(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        List<String> features = new ArrayList<>();
        Matcher identifiers = BUSINESS_ID.matcher(normalized);
        while (identifiers.find()) features.add(identifiers.group());
        for (String token : normalized.split("[^\\p{L}\\p{Nd}._:/-]+")) {
            if (token.length() >= MIN_FEATURE_LENGTH && isExactToken(token)) features.add(token);
        }
        if (normalized.length() >= MIN_FEATURE_LENGTH && normalized.length() <= MAX_PHRASE_LENGTH) {
            features.add(normalized);
        }
        return List.copyOf(new LinkedHashSet<>(features));
    }

    private boolean isExactToken(String token) {
        return token.matches(".*\\d.*") || token.contains("_") || token.contains(".")
                || token.contains("/") || token.contains(":");
    }
}
