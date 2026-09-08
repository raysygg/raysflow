package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 稀疏向量（Sparse Vector / BM25 词频统计）数据结构实体 Record（Sparse Vector Data）。
 * 包含索引位置数组 indices (List&lt;Long&gt;) 与对应位置权重值数组 values (List&lt;Float&gt;)，两者长度必须一致。
 *
 * @param indices 维度 Token 索引数组
 * @param values 对应维度的 TF-IDF/BM25 权重浮点数组
 */
public record SparseVectorData(
        List<Long> indices,
        List<Float> values
) {
    /** 紧凑构造函数做防空保护与维度等长强校验 */
    public SparseVectorData {
        indices = (indices == null) ? List.of() : List.copyOf(indices);
        values = (values == null) ? List.of() : List.copyOf(values);
        if (indices.size() != values.size()) {
            throw new IllegalArgumentException("Sparse vector 的索引数量和值数量不一致。");
        }
    }

    /**
     * 构建全空的稀疏向量对象。
     *
     * @return 空的 SparseVectorData 对象
     */
    public static SparseVectorData empty() {
        return new SparseVectorData(List.of(), List.of());
    }

    /**
     * 判断当前稀疏向量是否为空（无任何词频维度）。
     *
     * @return 若为空返回 true
     */
    public boolean isEmpty() {
        return indices.isEmpty();
    }
}

