package com.acme.agentstudio.domain.runtime.model;

/**
 * 平台日志、系统事件、全链路 Trace、用户记忆、对话历史与知识库文档的数据保存天数及治理清理策略 Record（Data Retention Policy）。
 *
 * @param eventDays 事件保存天数
 * @param traceDays Trace 追踪日志保存天数
 * @param memoryDays 长期/短期记忆保存天数
 * @param conversationDays 对话会话历史保存天数
 * @param documentDays 原始知识库文档保存天数
 * @param exportEnabled 是否允许数据合规导出
 * @param deletionEnabled 是否允许合规擦除与彻底删除
 */
public record DataRetentionPolicy(
        long eventDays,
        long traceDays,
        long memoryDays,
        long conversationDays,
        long documentDays,
        boolean exportEnabled,
        boolean deletionEnabled
) {
    /** 紧凑构造函数做输入保存天数断言校验 */
    public DataRetentionPolicy {
        if (eventDays < 1 || traceDays < 1 || memoryDays < 1 || conversationDays < 1 || documentDays < 1) {
            throw new IllegalArgumentException("数据保留期限必须大于零。");
        }
        if (!exportEnabled || !deletionEnabled) {
            throw new IllegalArgumentException("生产数据策略必须支持导出和删除。");
        }
    }
}

