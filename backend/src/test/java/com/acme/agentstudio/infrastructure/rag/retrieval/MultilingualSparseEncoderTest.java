package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.LexicalEncoding;
import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultilingualSparseEncoderTest {
    private final MultilingualSparseEncoder encoder = new MultilingualSparseEncoder();

    @Test
    void shouldPreserveTermFrequencyAndDigestExactFeatures() {
        LexicalEncoding single = encoder.encodeDocument("alpha");
        LexicalEncoding repeated = encoder.encodeDocument("alpha alpha ERR-42");
        LexicalEncoding query = encoder.encodeQuery("ERR-42");
        float singleMax = single.sparseVector().values().stream()
                .max(Float::compareTo)
                .orElseThrow();
        float repeatedMax = repeated.sparseVector().values().stream()
                .max(Float::compareTo)
                .orElseThrow();

        assertThat(repeatedMax).isGreaterThan(singleMax);
        assertThat(repeated.sparseVector().indices()).isSorted();
        assertThat(repeated.exactKeys()).isNotEmpty().allMatch(key -> key.startsWith("exact:"));
        assertThat(repeated.exactKeys()).noneMatch(key -> key.contains("err-42"));
        assertThat(repeated.exactKeys()).containsAnyElementsOf(query.exactKeys());
    }

    @Test
    void shouldGenerateStableMixedLanguageEncoding() {
        LexicalEncoding first = encoder.encodeDocument("支付 API v2.1 返回 ERR-42");
        LexicalEncoding second = encoder.encodeDocument("支付 API v2.1 返回 ERR-42");

        assertThat(first).isEqualTo(second);
        assertThat(first.sparseVector().indices()).isNotEmpty();
        assertThat(first.exactKeys()).isNotEmpty();
    }

    @Test
    void shouldSelectLanguageSpecificAndOtherFallbackStrategies() {
        MultilingualLexicalStrategyRegistry registry = new MultilingualLexicalStrategyRegistry();

        assertThat(registry.detect("支付失败")).isEqualTo(KnowledgeLanguage.ZH);
        assertThat(registry.detect("payment failed")).isEqualTo(KnowledgeLanguage.EN);
        assertThat(registry.detect("支付 API failed")).isEqualTo(KnowledgeLanguage.MIXED);
        assertThat(registry.detect("ошибка сервера")).isEqualTo(KnowledgeLanguage.OTHER);
        assertThat(registry.tokenize("支付 API failed")).contains("支付", "api", "failed");
        assertThat(registry.tokenize("ошибка")).contains("ошибка");
    }
}
