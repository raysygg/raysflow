package com.acme.agentstudio.domain.runtime.model;

/**
 * 会话内多轮对话短期记忆（Short-term Memory）滑窗截断与自动摘要总结策略 Record（Short-term Memory Policy）。
 * 包含最大允许保留的历史消息数 maxMessages、最大总字符长度 maxCharacters、超出时是否触发大模型自动摘要 enableSummary、
 * 是否自动过滤或掩码敏感对话内容 excludeSensitive 以及排序优先字段 priorityField。
 *
 * @param maxMessages 最多保留的上文消息条数
 * @param maxCharacters 最多保留的文本字符总数
 * @param enableSummary 滚动溢出时是否自动触发中间摘要总结
 * @param excludeSensitive 是否剥离或掩码敏感隐私字段
 * @param priorityField 在保留特定重要消息时的优先字段 key
 */
public record ShortTermMemoryPolicy(
        int maxMessages,
        int maxCharacters,
        boolean enableSummary,
        boolean excludeSensitive,
        String priorityField
) {
    /** 默认最大消息数：20 条 */
    public static final int DEFAULT_MAX_MESSAGES = 20;

    /** 默认最大字符限制：12,000 字符 */
    public static final int DEFAULT_MAX_CHARACTERS = 12_000;

    /** 默认优先保留字段 key */
    public static final String DEFAULT_PRIORITY_FIELD = "priority";

    /** 紧凑构造函数做输入校验 */
    public ShortTermMemoryPolicy {
        if (maxMessages < 1 || maxCharacters < 1) {
            throw new IllegalArgumentException("短期记忆窗口必须大于零。");
        }
        priorityField = (priorityField == null || priorityField.isBlank()) ? DEFAULT_PRIORITY_FIELD : priorityField;
    }

    /**
     * 构建默认短期记忆滑窗策略。
     *
     * @return 默认的 ShortTermMemoryPolicy 实例
     */
    public static ShortTermMemoryPolicy defaults() {
        return new ShortTermMemoryPolicy(DEFAULT_MAX_MESSAGES, DEFAULT_MAX_CHARACTERS, true, true, DEFAULT_PRIORITY_FIELD);
    }
}

