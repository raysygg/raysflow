package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.RuntimeBudget;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeContextLayers;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.TerminationPolicy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 运行时上下文构建器组件（Runtime Context Builder）。
 * 负责在 Agent Run 执行初始化阶段，分层解析并层叠覆盖配置：
 * 租户全局默认（tenantDefaults） -&gt; 应用全局配置（applicationConfig） -&gt; 流程图配置（flowConfig） -&gt; 节点运行覆盖（nodeConfig）。
 * 提取并约束运行预算（RuntimeBudget）、终止策略（TerminationPolicy），并对上下文快照中的敏感 Key（Password / API Key / Secret）进行脱敏掩码（[REDACTED]）安全审计。
 */
@Component
public class RuntimeContextBuilder {

    /** 运行策略配置 Key */
    private static final String KEY_POLICY = "policy";

    /** 运行预算配置 Key */
    private static final String KEY_BUDGET = "budget";

    /** 终止策略配置 Key */
    private static final String KEY_TERMINATION = "termination";

    /** 运行模式 Key */
    private static final String KEY_MODE = "mode";

    /** 配置项集合 Key */
    private static final String KEY_CONFIGURATION = "configuration";

    /** 运行变量 Key */
    private static final String KEY_VARIABLES = "variables";

    /** 短期对话记忆 Key */
    private static final String KEY_SHORT_TERM_MEMORY = "shortTermMemory";

    /** 长期提取记忆 Key */
    private static final String KEY_LONG_TERM_MEMORY = "longTermMemory";

    /** 检索知识来源 Key */
    private static final String KEY_RETRIEVED_KNOWLEDGE = "retrievedKnowledge";

    /** 工具绑定 Key */
    private static final String KEY_TOOLS = "tools";

    /**
     * 层叠解析并构建单次运行的只读上下文实体 RuntimeContext。
     *
     * @param tenantId 租户 ID
     * @param applicationId 应用 ID
     * @param releaseId 发布版本 ID
     * @param actorId 操作者 ID
     * @param conversationId 会话 ID
     * @param mode 运行模式 RuntimeMode
     * @param input 原始请求输入 Map
     * @param tenantDefaults 租户默认配置
     * @param applicationConfig 应用级配置
     * @param flowConfig 流程图级配置
     * @param nodeConfig 节点覆盖配置
     * @return 构建完成的运行上下文对象 RuntimeContext
     */
    public RuntimeContext build(
            long tenantId,
            long applicationId,
            String releaseId,
            String actorId,
            String conversationId,
            RuntimeMode mode,
            Map<String, Object> input,
            Map<String, Object> tenantDefaults,
            Map<String, Object> applicationConfig,
            Map<String, Object> flowConfig,
            Map<String, Object> nodeConfig
    ) {
        Map<String, Object> resolved = merge(tenantDefaults, applicationConfig, flowConfig, nodeConfig);
        Map<String, Object> policy = redact(mapValue(resolved, KEY_POLICY));
        RuntimeBudget budget = budget(mapValue(resolved, KEY_BUDGET));
        TerminationPolicy termination = termination(mapValue(resolved, KEY_TERMINATION));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put(KEY_MODE, mode.name());
        snapshot.put(KEY_CONFIGURATION, redact(resolved));
        snapshot.put(KEY_BUDGET, budget);
        snapshot.put(KEY_TERMINATION, termination);

        RuntimeContextLayers layers = new RuntimeContextLayers(
                input,
                mapValue(resolved, KEY_VARIABLES),
                mapValue(resolved, KEY_SHORT_TERM_MEMORY),
                mapValue(resolved, KEY_LONG_TERM_MEMORY),
                mapValue(resolved, KEY_RETRIEVED_KNOWLEDGE)
        );
        snapshot.put("contextPriority", "LONG_TERM_MEMORY < SHORT_TERM_MEMORY < RETRIEVED_KNOWLEDGE < VARIABLES < INPUT");

        return new RuntimeContext(
                UUID.randomUUID().toString(),
                tenantId,
                applicationId,
                releaseId,
                actorId,
                conversationId,
                mode,
                layers,
                mapValue(resolved, KEY_TOOLS),
                policy,
                budget,
                termination,
                snapshot,
                Instant.now()
        );
    }

    /** 顺序深度合并多层 Map 配置 */
    @SafeVarargs
    private final Map<String, Object> merge(Map<String, Object>... layers) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> layer : layers) {
            if (layer == null) {
                continue;
            }
            layer.forEach((key, value) -> {
                if (value instanceof Map<?, ?> nested && result.get(key) instanceof Map<?, ?> previous) {
                    Map<String, Object> merged = new LinkedHashMap<>();
                    previous.forEach((k, v) -> merged.put(String.valueOf(k), v));
                    nested.forEach((k, v) -> merged.put(String.valueOf(k), v));
                    result.put(key, merged);
                } else {
                    result.put(key, value);
                }
            });
        }
        return result;
    }

    /** 从配置 Map 安全获取嵌套 Map 类型的子配置 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return (value instanceof Map<?, ?> map) ? (Map<String, Object>) map : Map.of();
    }

    /** 解析运行预算定义 */
    private RuntimeBudget budget(Map<String, Object> values) {
        return new RuntimeBudget(
                integer(values, "maxSteps", RuntimeBudget.DEFAULT_MAX_STEPS),
                integer(values, "maxTokens", RuntimeBudget.DEFAULT_MAX_TOKENS),
                longValue(values, "maxCostMicros", RuntimeBudget.DEFAULT_MAX_COST_MICROS),
                longValue(values, "timeoutMillis", RuntimeBudget.DEFAULT_TIMEOUT_MILLIS)
        );
    }

    /** 解析运行终止与人工挂起策略 */
    private TerminationPolicy termination(Map<String, Object> values) {
        return new TerminationPolicy(
                integer(values, "maxToolCalls", TerminationPolicy.DEFAULT_MAX_TOOL_CALLS),
                booleanValue(values, "allowHumanHandoff", TerminationPolicy.DEFAULT_ALLOW_HUMAN_HANDOFF),
                booleanValue(values, "stopOnBudgetExhaustion", TerminationPolicy.DEFAULT_STOP_ON_BUDGET_EXHAUSTION)
        );
    }

    /** 安全提取 int 值 */
    private int integer(Map<String, Object> values, String key, int fallback) {
        return (values.get(key) instanceof Number number) ? number.intValue() : fallback;
    }

    /** 安全提取 long 值 */
    private long longValue(Map<String, Object> values, String key, long fallback) {
        return (values.get(key) instanceof Number number) ? number.longValue() : fallback;
    }

    /** 安全提取 boolean 值 */
    private boolean booleanValue(Map<String, Object> values, String key, boolean fallback) {
        return (values.get(key) instanceof Boolean value) ? value : fallback;
    }

    /** 脱敏处理包含 Key/Token 等敏感字段的 Map 数据结构 */
    private Map<String, Object> redact(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (sensitive(key)) {
                result.put(key, "[REDACTED]");
            } else if (value instanceof Map<?, ?> nested) {
                Map<String, Object> nestedValues = new LinkedHashMap<>();
                nested.forEach((nestedKey, nestedValue) -> nestedValues.put(String.valueOf(nestedKey), nestedValue));
                result.put(key, redact(nestedValues));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    /** 匹配敏感字段判断逻辑 */
    private boolean sensitive(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.contains("privatekey");
    }
}

