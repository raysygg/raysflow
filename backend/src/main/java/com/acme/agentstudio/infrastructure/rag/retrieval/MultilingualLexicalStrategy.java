package com.acme.agentstudio.infrastructure.rag.retrieval;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import java.util.List;

/**
 * 单一语言或脚本的 Sparse 词法分词与 Token 提取策略接口（Multilingual Lexical Strategy）。
 */
public interface MultilingualLexicalStrategy {

    /**
     * 获取当前策略对应的目标语种枚举。
     *
     * @return 语种枚举 KnowledgeLanguage
     */
    KnowledgeLanguage language();

    /**
     * 对规范化后的文本进行特定语种的分词与 Token 提取。
     *
     * @param normalizedText 归一化后的文本
     * @return 词项列表 List&lt;String&gt;
     */
    List<String> tokenize(String normalizedText);
}

