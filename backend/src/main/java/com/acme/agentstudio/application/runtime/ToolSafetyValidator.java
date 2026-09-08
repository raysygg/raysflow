package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.ContentTrust;
import com.acme.agentstudio.domain.runtime.model.ToolInvocationRequest;
import com.acme.agentstudio.domain.runtime.model.ToolPolicy;
import com.acme.agentstudio.domain.runtime.model.ToolValidationResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运行时工具安全校验与提示词注入检测器（Tool Safety Validator）。
 * 在 Tool 工具真正执行前，针对 ToolInvocationRequest 执行多重维度校验：
 * 1. 业务与数据权限边界校验（validateScopes：dataScopes 与 secretScopes 包含检查）；
 * 2. 网络出口与安全域名校验（validateTarget：结合 ToolNetworkGuard 防范 SSRF）；
 * 3. 提示词注入攻击 Prompt Injection 检测（detectTrust：识别 "ignore previous", "忽略之前" 等注入词汇并标为 ContentTrust.SUSPICIOUS）；
 * 4. 参数自动脱敏 Sanitizer（sanitize：深度嵌套递归脱敏 password / secret / token / api_key 字段并限制最大深度 MAX_ARGUMENT_DEPTH = 8）。
 */
@Component
public class ToolSafetyValidator {

    /** 脱敏占位文本 */
    private static final String REDACTED = "[已脱敏]";

    /** 不可信/疑似提示注入内容标记 */
    private static final String SUSPICIOUS_CONTENT_MARKER = "[不可信内容]";

    /** 参数结构最大递归深度 */
    private static final int MAX_ARGUMENT_DEPTH = 8;

    /** 网络出口出口守卫组件 */
    private final ToolNetworkGuard networkGuard;

    /**
     * 构造函数注入依赖 ToolNetworkGuard。
     */
    public ToolSafetyValidator(ToolNetworkGuard networkGuard) {
        this.networkGuard = networkGuard;
    }

    /**
     * 对工具调用请求 ToolInvocationRequest 执行全面安全与策略合规性校验。
     *
     * @param request 工具调用请求实体
     * @param policy 对应的工具安全策略配置 ToolPolicy
     * @return 综合安全校验结果对象 ToolValidationResult
     */
    public ToolValidationResult validate(ToolInvocationRequest request, ToolPolicy policy) {
        if (policy == null || !policy.toolId().equals(request.toolId())) {
            return rejected(ContentTrust.UNTRUSTED, "工具尚未配置安全策略，或请求的工具标识与策略标识不一致。");
        }

        List<String> reasons = new ArrayList<>();
        boolean dataScopeAllowed = validateScopes(request.requestedDataScopes(), policy.dataScopes(), "数据范围", reasons);
        boolean secretScopeAllowed = validateScopes(request.requestedSecretScopes(), policy.secretScopes(), "密钥范围", reasons);
        boolean targetAllowed = validateTarget(request.targetUri(), policy.allowedDomains(), policy.networkEgress(), reasons);

        ContentTrust trust = detectTrust(request.arguments());
        if (trust == ContentTrust.SUSPICIOUS) {
            reasons.add("工具参数中检测到疑似提示词注入攻击（Prompt Injection）内容，已被系统标记为不可信。");
        }

        boolean requiresConfirmation = policy.requireConfirmation();
        if (requiresConfirmation) {
            reasons.add("该工具属于高风险或带副作用操作，按策略要求需要人工审批确认。");
        }

        Map<String, Object> sanitized = sanitize(request.arguments(), 0);
        boolean allowed = dataScopeAllowed && secretScopeAllowed && targetAllowed;

        return new ToolValidationResult(allowed, requiresConfirmation, trust, sanitized, reasons);
    }

    /** 拒绝执行辅助构造 */
    private ToolValidationResult rejected(ContentTrust trust, String reason) {
        return new ToolValidationResult(false, false, trust, Map.of(), List.of(reason));
    }

    /** 校验请求的作用域范围是否被允许范围完全包含 */
    private boolean validateScopes(Set<String> requested, Set<String> allowed, String name, List<String> reasons) {
        requested.stream()
                .filter(scope -> !allowed.contains(scope))
                .forEach(scope -> reasons.add(name + "未授权：" + scope));

        return requested.stream().allMatch(allowed::contains);
    }

    /** 校验目标 URL 网络出口与允许域名白名单 */
    private boolean validateTarget(
            String targetUri,
            Set<String> allowedDomains,
            Set<String> networkEgress,
            List<String> reasons
    ) {
        if (targetUri == null || targetUri.isBlank()) {
            return true;
        }

        try {
            networkGuard.requireSafeTarget(targetUri);
        } catch (IllegalArgumentException exception) {
            reasons.add(exception.getMessage());
            return false;
        }

        try {
            URI uri = URI.create(targetUri);
            String host = uri.getHost();
            if (host == null) {
                reasons.add("目标网络地址无效，缺少 Host 主机名。");
                return false;
            }

            boolean allowed = true;
            if (!allowedDomains.isEmpty() && allowedDomains.stream().noneMatch(domain -> matchesDomain(host, domain))) {
                reasons.add("目标域名 [" + host + "] 不在系统配置的允许域名白名单中。");
                allowed = false;
            }

            String egress = uri.getScheme() + ":" + uri.getPort();
            if (!networkEgress.isEmpty() && !networkEgress.contains(egress)) {
                reasons.add("网络出口协议端口 [" + egress + "] 未在允许的出口规则中定义。");
                allowed = false;
            }

            return allowed;
        } catch (IllegalArgumentException exception) {
            reasons.add("目标网络地址 URI 格式无效。");
            return false;
        }
    }

    /** 域名模糊匹配（主域名与子域名） */
    private boolean matchesDomain(String host, String allowedDomain) {
        String normalizedHost = host.toLowerCase();
        String normalizedDomain = allowedDomain.toLowerCase();
        return normalizedHost.equals(normalizedDomain) || normalizedHost.endsWith("." + normalizedDomain);
    }

    /** 递归检测文本与数据结构中的提示词注入特征 */
    private ContentTrust detectTrust(Object value) {
        if (value == null) {
            return ContentTrust.TRUSTED;
        }

        String text = String.valueOf(value).toLowerCase();
        if (text.contains("ignore previous") || text.contains("system prompt") || text.contains("developer message")
                || text.contains("忽略之前") || text.contains("系统提示词") || text.contains("开发者消息")) {
            return ContentTrust.SUSPICIOUS;
        }

        if (value instanceof Map<?, ?> map) {
            return map.values().stream()
                    .map(this::detectTrust)
                    .filter(trust -> trust != ContentTrust.TRUSTED)
                    .findFirst()
                    .orElse(ContentTrust.TRUSTED);
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (detectTrust(item) != ContentTrust.TRUSTED) {
                    return ContentTrust.SUSPICIOUS;
                }
            }
        }

        return ContentTrust.TRUSTED;
    }

    /** 递归对参数结构中的敏感字段进行脱敏 */
    private Map<String, Object> sanitize(Map<String, Object> values, int depth) {
        if (depth > MAX_ARGUMENT_DEPTH) {
            return Map.of("content", REDACTED);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalized = key.toLowerCase();
            if (normalized.contains("password") || normalized.contains("secret") || normalized.contains("token")
                    || normalized.contains("apikey") || normalized.contains("api_key")) {
                result.put(key, REDACTED);
            } else if (value instanceof Map<?, ?> nested) {
                Map<String, Object> nestedValues = new LinkedHashMap<>();
                nested.forEach((nestedKey, nestedValue) -> nestedValues.put(String.valueOf(nestedKey), nestedValue));
                result.put(key, sanitize(nestedValues, depth + 1));
            } else {
                result.put(key, value);
            }
        });

        if (detectTrust(values) == ContentTrust.SUSPICIOUS) {
            result.put("_contentTrust", SUSPICIOUS_CONTENT_MARKER);
        }

        return result;
    }
}

