package com.acme.agentstudio.domain.runtime.model;

/**
 * 知识库 RAG 召回结果与上下文注入控制策略 Record（Retrieval Policy）。
 * 控制召回 Top-K 数量 maxResults、注入 Prompt 的最大上下文字符预算 maxContextCharacters、
 * 去重去冗余标志 deduplicateSources、在回答尾部附带文献引用出处 includeCitations 及未命中降级提示语 noHitMessage。
 *
 * @param maxResults 最大召回 Top-K 段落数量
 * @param maxContextCharacters 允许注入模型的最大字符数限制
 * @param deduplicateSources 是否剔除重复来源
 * @param includeCitations 是否自动附带可追溯的中文 Citation 引用
 * @param noHitMessage 未命中相关知识库内容时的降级回复文案
 */
public record RetrievalPolicy(
        int maxResults,
        int maxContextCharacters,
        boolean deduplicateSources,
        boolean includeCitations,
        String noHitMessage
) {
    /** 默认最大召回结果条数：8 */
    public static final int DEFAULT_MAX_RESULTS = 8;

    /** 默认最大上下文注入字符数：12,000 字符 */
    public static final int DEFAULT_MAX_CONTEXT_CHARACTERS = 12_000;

    /** 默认未命中提示语 */
    public static final String DEFAULT_NO_HIT_MESSAGE = "未找到足够的知识依据，暂不生成结论。";

    /** 紧凑构造函数做输入限额断言校验 */
    public RetrievalPolicy {
        if (maxResults < 1 || maxContextCharacters < 1) {
            throw new IllegalArgumentException("检索结果数量和上下文预算必须大于零。");
        }
        noHitMessage = (noHitMessage == null || noHitMessage.isBlank())
                ? DEFAULT_NO_HIT_MESSAGE : noHitMessage;
    }

    /**
     * 构建默认 RAG 检索策略。
     *
     * @return 默认的 RetrievalPolicy 实例
     */
    public static RetrievalPolicy defaults() {
        return new RetrievalPolicy(DEFAULT_MAX_RESULTS, DEFAULT_MAX_CONTEXT_CHARACTERS,
                true, true, DEFAULT_NO_HIT_MESSAGE);
    }
}

