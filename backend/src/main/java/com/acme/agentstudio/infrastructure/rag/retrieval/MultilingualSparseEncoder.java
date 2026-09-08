package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.LexicalEncoding;
import com.acme.agentstudio.domain.knowledge.model.SparseVectorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 多语言稀疏向量编码器实现类（Multilingual Sparse Encoder）。
 * 将多语言词项编码为可持久化的 Qdrant Sparse vector。
 */
@Component
public class MultilingualSparseEncoder {
    public static final String INDEX_CONTRACT_VERSION = "qdrant-sparse-v2-lang-registry";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final double BM25_K1 = 1.2D;
    private static final String EXACT_KEY_PREFIX = "exact:";

    private final MultilingualLexicalStrategyRegistry strategyRegistry;
    private final ExactFeatureExtractor exactFeatureExtractor;

    @Autowired
    public MultilingualSparseEncoder(MultilingualLexicalStrategyRegistry strategyRegistry,
                                     ExactFeatureExtractor exactFeatureExtractor) {
        this.strategyRegistry = strategyRegistry;
        this.exactFeatureExtractor = exactFeatureExtractor;
    }

    public MultilingualSparseEncoder() {
        this(new MultilingualLexicalStrategyRegistry(), new ExactFeatureExtractor());
    }

        /**
         * encodeDocument 方法。
         *
         * @param text text 参数
         * @return LexicalEncoding 返回对象
         */
    public LexicalEncoding encodeDocument(String text) {
        return encode(text, true);
    }

        /**
         * encodeQuery 方法。
         *
         * @param text text 参数
         * @return LexicalEncoding 返回对象
         */
    public LexicalEncoding encodeQuery(String text) {
        return encode(text, false);
    }

    private LexicalEncoding encode(String text, boolean document) {
        List<String> tokens = strategyRegistry.tokenize(text);
        Map<String, Integer> frequencies = new LinkedHashMap<>();
        tokens.forEach(token -> frequencies.merge(token, 1, Integer::sum));
        TreeMap<Long, Double> sparseValues = new TreeMap<>();
        frequencies.forEach((token, frequency) -> sparseValues.merge(termIndex(token),
                document ? saturatedTermFrequency(frequency) : (double) frequency, Double::sum));

        List<Long> indices = new ArrayList<>(sparseValues.size());
        List<Float> values = new ArrayList<>(sparseValues.size());
        sparseValues.forEach((index, value) -> {
            indices.add(index);
            values.add(value.floatValue());
        });
        List<String> exactKeys = exactFeatureExtractor.extract(text).stream()
                .map(this::exactKey)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
        return new LexicalEncoding(new SparseVectorData(indices, values), exactKeys);
    }

    private double saturatedTermFrequency(int frequency) {
        return frequency * (BM25_K1 + 1D) / (frequency + BM25_K1);
    }

    private long termIndex(String token) {
        byte[] digest = digest(token);
        return Integer.toUnsignedLong(ByteBuffer.wrap(digest).getInt());
    }

    private String exactKey(String feature) {
        return EXACT_KEY_PREFIX + HexFormat.of().formatHex(digest(feature));
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM)
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持词法索引摘要算法。", exception);
        }
    }
}
