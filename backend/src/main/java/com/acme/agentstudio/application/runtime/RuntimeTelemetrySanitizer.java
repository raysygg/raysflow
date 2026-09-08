package com.acme.agentstudio.application.runtime;

import java.util.Locale;
import java.util.Set;

/**
 * 运行时遥测日志脱敏与摘要截断工具类（Runtime Telemetry Sanitizer）。
 * 过滤暴露在运营工作台、日志、SLO 告警和指标中的文本摘要（summary），
 * 自动识别并过滤敏感词（token, secret, password, authorization, api-key, credential），替换为 "已隐藏敏感信息"；
 * 同时控制摘要文本的最大输出长度（MAX_SUMMARY_LENGTH = 256），防止过长 payload 数据膨胀。
 */
public final class RuntimeTelemetrySanitizer {

    /** 敏感词关键词过滤集合 */
    private static final Set<String> SENSITIVE = Set.of(
            "token",
            "secret",
            "password",
            "authorization",
            "api-key",
            "credential"
    );

    /** 安全摘要最大限制长度（字符） */
    private static final int MAX_SUMMARY_LENGTH = 256;

    /**
     * 私有构造函数，防止实例化工具类。
     */
    private RuntimeTelemetrySanitizer() {
    }

    /**
     * 对给定的文本字符串进行脱敏与截断处理，返回可安全公开展示的摘要。
     *
     * @param value 原始可能包含敏感词或过长内容的字符串
     * @return 经过安全处理后的字符串摘要
     */
    public static String summary(String value) {
        String text = (value == null) ? "" : value.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        if (SENSITIVE.stream().anyMatch(lower::contains)) {
            return "已隐藏敏感信息";
        }

        if (text.length() <= MAX_SUMMARY_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_SUMMARY_LENGTH);
    }
}

