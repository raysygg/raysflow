package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ShortTermMemoryPolicy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时 Agent 会话短期记忆构建与策略裁剪服务（Short Term Memory Service）。
 * 根据配置的 ShortTermMemoryPolicy（限制最高消息数 maxMessages、最高字符数 maxCharacters、是否排除敏感信息 excludeSensitive 以及摘要压缩 enableSummary），
 * 从历史会话 Message 列表中筛选、优先级排序并恢复原始时间顺序构建窗口记忆，超出的历史自动追加总结摘要前缀。
 */
@Service
public class ShortTermMemoryService {

    /** 消息内容属性键 */
    private static final String CONTENT_KEY = "content";

    /** 角色属性键 */
    private static final String ROLE_KEY = "role";

    /** 摘要标志属性键 */
    private static final String SUMMARY_KEY = "summary";

    /** 敏感内容替代字符串 */
    private static final String REDACTED = "[已过滤敏感内容]";

    /**
     * 根据 ShortTermMemoryPolicy 短期记忆策略构建修剪后的会话消息列表。
     *
     * @param messages 原始完整历史消息列表 Map
     * @param policy 短期记忆裁剪策略 ShortTermMemoryPolicy（为空时使用默认策略）
     * @return 构建并符合预算限制的消息列表 List&lt;Map&lt;String, Object&gt;&gt;
     */
    public List<Map<String, Object>> build(List<Map<String, Object>> messages, ShortTermMemoryPolicy policy) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        ShortTermMemoryPolicy effective = (policy == null) ? ShortTermMemoryPolicy.defaults() : policy;

        List<Map<String, Object>> ranked = messages.stream()
                .map(this::copy)
                .filter(message -> !effective.excludeSensitive() || !Boolean.TRUE.equals(message.get("sensitive")))
                .sorted(Comparator.comparingInt((Map<String, Object> message) -> priority(message, effective.priorityField())).reversed())
                .limit(effective.maxMessages())
                .toList();

        ranked = new ArrayList<>(ranked);
        ranked.sort(Comparator.comparingInt(message -> originalIndex(messages, message)));

        List<Map<String, Object>> result = new ArrayList<>();
        int characters = 0;

        for (Map<String, Object> message : ranked) {
            String content = String.valueOf(message.getOrDefault(CONTENT_KEY, ""));
            if (characters + content.length() > effective.maxCharacters()) {
                continue;
            }
            if (effective.excludeSensitive() && Boolean.TRUE.equals(message.get("sensitive"))) {
                message.put(CONTENT_KEY, REDACTED);
            }
            result.add(message);
            characters += content.length();
        }

        if (effective.enableSummary() && result.size() < messages.size()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put(ROLE_KEY, "system");
            summary.put(CONTENT_KEY, "历史消息已按短期记忆预算限制自动压缩与截断。");
            summary.put(SUMMARY_KEY, true);
            result.add(0, summary);
        }

        return List.copyOf(result);
    }

    /** 浅拷贝 Map 防止污染原对象 */
    private Map<String, Object> copy(Map<String, Object> message) {
        return (message == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(message);
    }

    /** 获取优先级属性数字 */
    private int priority(Map<String, Object> message, String priorityField) {
        Object value = message.get(priorityField);
        return (value instanceof Number number) ? number.intValue() : 0;
    }

    /** 计算在原列表中的下标索引 */
    private int originalIndex(List<Map<String, Object>> messages, Map<String, Object> message) {
        int index = messages.indexOf(message);
        return (index < 0) ? Integer.MAX_VALUE : index;
    }
}

