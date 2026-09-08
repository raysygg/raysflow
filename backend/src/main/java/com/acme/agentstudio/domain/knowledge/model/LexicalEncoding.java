package com.acme.agentstudio.domain.knowledge.model;

import java.util.List;

/**
 * 文本词法编码结果实体 Record（Lexical Encoding）。
 * 包含 BM25 稀疏向量 sparseVector (SparseVectorData)
 * 与不可逆精确哈希匹配键列表 exactKeys (List&lt;String&gt;)。
 *
 * @param sparseVector 词频稀疏向量表示
 * @param exactKeys 关键词哈希键列表
 */
public record LexicalEncoding(
        SparseVectorData sparseVector,
        List<String> exactKeys
) {
    /** 紧凑构造函数做输入防空处理 */
    public LexicalEncoding {
        sparseVector = (sparseVector == null) ? SparseVectorData.empty() : sparseVector;
        exactKeys = (exactKeys == null) ? List.of() : List.copyOf(exactKeys);
    }
}

