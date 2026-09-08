package com.acme.agentstudio.infrastructure.rag;

import com.acme.agentstudio.domain.common.ApplicationMessages;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 回答 Grounding 事实验证与防幻觉守卫工具类（RAG Answer Guard）。
 * 对大模型输出做严格的 Grounding 校验：包括无命中知识库时强制拒答（NO_HIT_REPLY）、
 * 强制检查来源引用 [来源N]、弱事实支撑时自动降级拒绝回答（UNSUPPORTED_REPLY）或追加参考来源页脚。
 */
public final class RagAnswerGuard {

    public static final String NO_HIT_REPLY = ApplicationMessages.RAG_NO_HIT_REPLY;
    public static final String UNSUPPORTED_REPLY = ApplicationMessages.RAG_UNSUPPORTED_REPLY;

    private static final Pattern SOURCE_PATTERN = Pattern.compile("来源\\s*(\\d+)");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]{2,}|\\d+(?:\\.\\d+)?");

    private RagAnswerGuard() {
    }

        /**
         * hasHits 方法。
         *
         * @param references references 参数
         * @return static boolean 返回对象
         */
    public static boolean hasHits(List<RagSearchResult> references) {
        return references != null && !references.isEmpty();
    }

        /**
         * enforce 方法。
         *
         * @param modelReply modelReply 参数
         * @param references references 参数
         * @return static String 返回对象
         */
    public static String enforce(String modelReply, List<RagSearchResult> references) {
        if (!hasHits(references)) {
            return NO_HIT_REPLY;
        }
        if (modelReply == null || modelReply.isBlank()) {
            return UNSUPPORTED_REPLY;
        }
        String normalized = modelReply.trim();
        if (looksLikeRefusal(normalized)) {
            return normalized;
        }
        if (!containsSourceCitation(normalized, references.size()) && !isSupportedByReferences(normalized, references)) {
            return UNSUPPORTED_REPLY;
        }
        if (!containsSourceCitation(normalized, references.size())) {
            return normalized + "\n\n" + buildCitationFooter(references);
        }
        return normalized;
    }

        /**
         * isGrounded 方法。
         *
         * @param reply reply 参数
         * @param references references 参数
         * @return static boolean 返回对象
         */
    public static boolean isGrounded(String reply, List<RagSearchResult> references) {
        if (!hasHits(references) || reply == null || reply.isBlank()) {
            return false;
        }
        if (looksLikeRefusal(reply)) {
            return false;
        }
        return containsSourceCitation(reply, references.size()) || isSupportedByReferences(reply, references);
    }

    private static boolean looksLikeRefusal(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        return value.contains("未检索到")
                || value.contains("无法基于企业知识")
                || value.contains("无法充分支撑")
                || value.contains("知识库中没有")
                || value.contains("无法回答")
                || value.contains("no relevant")
                || value.contains("cannot answer");
    }

    private static boolean containsSourceCitation(String text, int referenceCount) {
        Matcher matcher = SOURCE_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                if (index >= 1 && index <= referenceCount) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // 忽略格式非法的引用标记
            }
        }
        return false;
    }

    private static boolean isSupportedByReferences(String reply, List<RagSearchResult> references) {
        Set<String> replyTokens = tokenize(reply);
        if (replyTokens.isEmpty()) {
            return false;
        }
        Set<String> contextTokens = new LinkedHashSet<>();
        for (RagSearchResult reference : references) {
            contextTokens.addAll(tokenize(reference.text()));
        }
        if (contextTokens.isEmpty()) {
            return false;
        }
        long overlap = replyTokens.stream().filter(contextTokens::contains).count();
        double coverage = (double) overlap / replyTokens.size();
        return coverage >= 0.28D || overlap >= 4;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static String buildCitationFooter(List<RagSearchResult> references) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < references.size(); i++) {
            labels.add("[来源" + (i + 1) + "] " + references.get(i).sourceLabel());
        }
        return "参考来源：" + String.join("；", labels);
    }
}
