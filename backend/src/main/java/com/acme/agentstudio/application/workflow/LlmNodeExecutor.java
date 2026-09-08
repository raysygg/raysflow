package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.application.metrics.ModelCallMetricService;
import com.acme.agentstudio.application.saas.TenantEntitlementService;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionDecision;
import com.acme.agentstudio.domain.saas.SaasGovernanceContracts.AdmissionRequest;
import com.acme.agentstudio.domain.common.PromptPlaceholders;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.infrastructure.model.ChatModelRegistry;
import com.acme.agentstudio.infrastructure.prompt.PromptRenderService;
import com.acme.agentstudio.infrastructure.rag.RagAnswerGuard;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalMetricService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 工作流 LLM 大模型节点执行器（LlmNodeExecutor）。
 * 负责解析提示词模板、注入会话历史与 RAG 检索知识上下文、多模型备用/降级重试调度、多模态视觉输入处理、结构化输出 JSON Schema 校验及 LangChain4j 实时流式响应广播。
 */
@Service
public class LlmNodeExecutor {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(LlmNodeExecutor.class);

    /** 配置项：上下文策略 */
    private static final String CONTEXT_POLICY = "contextPolicy";

    /** 配置项：包含短期记忆 */
    private static final String INCLUDE_SHORT_TERM_MEMORY = "includeShortTermMemory";

    /** 配置项：包含长期记忆 */
    private static final String INCLUDE_LONG_TERM_MEMORY = "includeLongTermMemory";

    /** 配置项：包含检索知识 */
    private static final String INCLUDE_RETRIEVED_KNOWLEDGE = "includeRetrievedKnowledge";

    /** 变量 Key：对话历史 */
    private static final String CONVERSATION_HISTORY = WorkflowVariableNames.CONTEXT_CONVERSATION_HISTORY;

    /** 变量 Key：短期记忆 */
    private static final String SHORT_TERM_MEMORY = WorkflowVariableNames.CONTEXT_SHORT_TERM_MEMORY;

    /** 变量 Key：长期记忆 */
    private static final String LONG_TERM_MEMORY = WorkflowVariableNames.CONTEXT_LONG_TERM_MEMORY;

    /** 变量 Key：检索到的知识 */
    private static final String RETRIEVED_KNOWLEDGE = WorkflowVariableNames.CONTEXT_RETRIEVED_KNOWLEDGE;

    /** 变量 Key：RAG 上下文 */
    private static final String RAG_CONTEXT = WorkflowVariableNames.VARIABLE_RAG_CONTEXT;

    /** 变量 Key：RAG 检索结果清单 */
    private static final String RAG_RESULTS = WorkflowVariableNames.VARIABLE_RAG_RESULTS;

    /** 默认滑动记忆窗口大小 */
    private static final int DEFAULT_MEMORY_WINDOW = 10;

    /** 模型注册表 */
    private final ChatModelRegistry models;

    /** 模型调用指标服务 */
    private final ModelCallMetricService metrics;

    /** RAG 检索指标服务 */
    private final RagRetrievalMetricService ragRetrievalMetricService;

    /** 提示词渲染服务 */
    private final PromptRenderService promptRenderService;

    /** Jackson JSON 映射组件 */
    private final ObjectMapper objectMapper;

    /** 模型调用前统一权益准入服务 */
    private final TenantEntitlementService entitlementService;

    /**
     * 构造函数注入核心依赖。
     */
    public LlmNodeExecutor(ChatModelRegistry models,
                           ModelCallMetricService metrics,
                           RagRetrievalMetricService ragRetrievalMetricService,
                           PromptRenderService promptRenderService,
                           ObjectMapper objectMapper,
                           TenantEntitlementService entitlementService) {
        this.models = models;
        this.metrics = metrics;
        this.ragRetrievalMetricService = ragRetrievalMetricService;
        this.promptRenderService = promptRenderService;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    /**
     * 执行大模型节点逻辑（使用默认配置与空监听器）。
     *
     * @param tenantId 租户 ID
     * @param config 节点配置 JSON
     * @param variables 当前工作流变量 Map
     * @return 包含文本内容与模型键的 Result 对象
     */
    public Result execute(Long tenantId, JsonNode config, Map<String, Object> variables) {
        return execute(tenantId, config, variables, null, ModelDeltaListener.NOOP);
    }

    /**
     * 执行大模型节点逻辑（带 Token 流式响应监听器）。
     *
     * @param tenantId 租户 ID
     * @param config 节点配置 JSON
     * @param variables 当前工作流变量 Map
     * @param deltaListener Token 实时流监听器
     * @return 包含文本内容与模型键的 Result 对象
     */
    public Result execute(Long tenantId, JsonNode config, Map<String, Object> variables,
                          ModelDeltaListener deltaListener) {
        return execute(tenantId, config, variables, null, deltaListener);
    }

    /**
     * 执行大模型节点逻辑（带冻结模型版本引用与流监听器）。
     *
     * @param tenantId 租户 ID
     * @param config 节点配置 JSON
     * @param variables 当前工作流变量 Map
     * @param modelReference 发布的不可变模型依赖引用
     * @param deltaListener Token 实时流监听器
     * @return 包含文本内容与模型键的 Result 对象
     */
    public Result execute(Long tenantId, JsonNode config, Map<String, Object> variables,
                          WorkflowDependencySnapshot.ModelDependencyReference modelReference,
                          ModelDeltaListener deltaListener) {
        log.info("开始执行模型节点，tenantId=[{}], 变量数量=[{}]", tenantId, variables == null ? 0 : variables.size());

        List<String> candidates = new ArrayList<>();
        String primary = modelReference == null
                ? config.path("modelKey").asText(config.path("modelId").asText(""))
                : modelReference.modelKey();

        if (!primary.isBlank()) {
            candidates.add(primary);
        }

        if (modelReference == null) {
            String backup = config.path("backupModelKey").asText("");
            if (!backup.isBlank() && !candidates.contains(backup)) {
                candidates.add(backup);
            }
            if (config.path("fallbackModels").isArray()) {
                config.path("fallbackModels").forEach(candidate -> {
                    if (!candidate.asText().isBlank() && !candidates.contains(candidate.asText())) {
                        candidates.add(candidate.asText());
                    }
                });
            }
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("当前 LLM 节点未配置有效的模型引用标识。");
        }

        ContextPolicy contextPolicy = contextPolicy(config, variables);
        Map<String, Object> promptVariables = resolvePromptVariables(variables, contextPolicy);
        List<RagSearchResult> references = extractReferences(promptVariables);

        boolean requireGrounding = usesRagContext(config);
        if (requireGrounding && !RagAnswerGuard.hasHits(references)) {
            String refusal = RagAnswerGuard.NO_HIT_REPLY;
            ragRetrievalMetricService.record(tenantId, "ORCHESTRATION_LLM", 0, 0D, false);
            return new Result(refusal, primary.isBlank() ? "NO_MODEL" : primary, 0, contextUsage(contextPolicy, references));
        }

        String prompt = buildPrompt(config, promptVariables, references, contextPolicy);
        long estimatedTokens = Math.max(1, (prompt.length() + 3L) / 4L);
        var admission = entitlementService.admitForConsumption(new AdmissionRequest(
                tenantId, null, "MODEL_TOKEN", estimatedTokens,
                "工作流模型调用", "model-call-" + UUID.randomUUID()), true);
        if (admission.decision() == AdmissionDecision.DENY) {
            throw new IllegalStateException(admission.reason() + " " + admission.remediation());
        }
        Exception last = null;
        int maxAttempts = Math.max(1, Math.min(5, config.path("retryPolicy").path("maxAttempts").asInt(1)));
        int attemptNo = 0;

        for (String modelKey : candidates) {
            for (int retry = 0; retry < maxAttempts; retry++) {
                attemptNo++;
                long started = System.nanoTime();
                try {
                    Response<AiMessage> response = generate(tenantId, modelKey, modelReference,
                            buildUserMessage(config, promptVariables, prompt), deltaListener);
                    String text = response.content().text();

                    if (requireGrounding) {
                        text = RagAnswerGuard.enforce(text, references);
                        double topScore = references.stream().mapToDouble(RagSearchResult::score).max().orElse(0D);
                        ragRetrievalMetricService.record(
                                tenantId,
                                "ORCHESTRATION_LLM",
                                references.size(),
                                topScore,
                                RagAnswerGuard.isGrounded(text, references)
                        );
                    } else {
                        validateStructured(config.path("structuredOutputSchema"), text);
                    }

                    metrics.recordSuccess(tenantId, null, modelKey, "ORCHESTRATION_LLM", (System.nanoTime() - started) / 1_000_000, response);
                    log.info("模型节点执行成功，tenantId=[{}], modelKey=[{}], 尝试次数=[{}], 输出长度=[{}]",
                            tenantId, modelKey, attemptNo, text == null ? 0 : text.length());
                    return new Result(text, modelKey, attemptNo, contextUsage(contextPolicy, references));
                } catch (Exception ex) {
                    last = ex;
                    metrics.recordFailure(tenantId, null, modelKey, "ORCHESTRATION_LLM", (System.nanoTime() - started) / 1_000_000, ex);
                    log.warn("模型节点调用失败，tenantId=[{}], modelKey=[{}], 尝试次数=[{}], 原因：{}",
                            tenantId, modelKey, attemptNo, ex.getMessage());
                }
            }
        }
        throw new IllegalStateException("所有 LLM 候选模型均调用失败，最终错误：" + (last == null ? "未知原因" : last.getMessage()), last);
    }

    /**
     * 底层模型生成方法（支持标准 Blocking 阻塞调用与 Streaming 流式监听）。
     */
    private Response<AiMessage> generate(Long tenantId, String modelKey,
                                           WorkflowDependencySnapshot.ModelDependencyReference modelReference,
                                           UserMessage message,
                                           ModelDeltaListener deltaListener) {
        if (deltaListener == null || deltaListener == ModelDeltaListener.NOOP) {
            ChatLanguageModel model = modelReference == null
                    ? models.getModel(tenantId, modelKey)
                    : models.getModel(modelReference);
            return model.generate(message);
        }

        StreamingChatLanguageModel model = modelReference == null
                ? models.getStreamingModel(tenantId, modelKey)
                : models.getStreamingModel(modelReference);

        @SuppressWarnings("unchecked")
        final Response<AiMessage>[] response = new Response[1];
        final Throwable[] error = new Throwable[1];
        CountDownLatch completionLatch = new CountDownLatch(1);

        model.generate(List.of(message), new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                if (token != null && !token.isEmpty()) {
                    deltaListener.onDelta(token);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> completed) {
                response[0] = completed;
                completionLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                error[0] = throwable;
                completionLatch.countDown();
            }
        });

        try {
            if (!completionLatch.await(90, TimeUnit.SECONDS)) {
                throw new IllegalStateException("流式模型响应超时（超过 90 秒），请检查网络连接或模型服务可达性。");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("流式模型等待过程被外部中断。", interrupted);
        }

        if (error[0] != null) {
            throw new IllegalStateException("流式模型调用抛出异常：" + error[0].getMessage(), error[0]);
        }
        if (response[0] == null || response[0].content() == null) {
            throw new IllegalStateException("流式模型未返回有效的响应内容。");
        }
        return response[0];
    }

    /**
     * 模型 Token 增量流监听器函数式接口。
     */
    @FunctionalInterface
    public interface ModelDeltaListener {
        /** 空操作监听器 */
        ModelDeltaListener NOOP = delta -> { };

        /** 接收 Token 增量回调 */
        void onDelta(String delta);
    }

    /**
     * 根据模板与变量填充组装 System Prompt 与 User Prompt。
     */
    private String buildPrompt(JsonNode config, Map<String, Object> variables, List<RagSearchResult> references,
                               ContextPolicy contextPolicy) {
        String template = config.path("promptTemplate").asText("");
        String userMessage = String.valueOf(variables.getOrDefault(WorkflowVariableNames.VARIABLE_QUERY,
                variables.getOrDefault(WorkflowVariableNames.VARIABLE_INPUT,
                        variables.getOrDefault(WorkflowVariableNames.VARIABLE_USER_MESSAGE, ""))));
        String result;

        if (template.contains(PromptPlaceholders.RAG_CONTEXT) || template.contains(PromptPlaceholders.USER_MESSAGE) || !references.isEmpty()) {
            result = promptRenderService.renderTemplate(template, userMessage, references);
            for (Map.Entry<String, Object> item : variables.entrySet()) {
                if (item.getKey() == null || RAG_RESULTS.equals(item.getKey())) {
                    continue;
                }
                result = result.replace("{{" + item.getKey() + "}}", String.valueOf(item.getValue()));
            }
        } else {
            result = render(template, variables);
        }

        String system = config.path("systemMessage").asText("");
        if (!system.isBlank()) {
            result = system + "\n\n" + result;
        }

        Object history = variables.get(CONVERSATION_HISTORY);
        if (history instanceof List<?> messages && !messages.isEmpty()) {
            int limit = contextPolicy.memoryWindow();
            int from = Math.max(0, messages.size() - limit);
            StringBuilder memory = new StringBuilder("对话历史记忆:\n");
            for (Object item : messages.subList(from, messages.size())) {
                memory.append(item).append('\n');
            }
            result = memory + "\n" + result;
        }

        int maxChars = config.path("maxContextChars").asInt(0);
        return maxChars > 0 && result.length() > maxChars ? result.substring(result.length() - maxChars) : result;
    }

    /**
     * 按节点声明裁剪 Prompt 可见上下文。
     */
    private Map<String, Object> resolvePromptVariables(Map<String, Object> variables, ContextPolicy policy) {
        Map<String, Object> result = new LinkedHashMap<>(variables == null ? Map.of() : variables);
        if (!policy.includeShortTermMemory()) {
            result.remove(CONVERSATION_HISTORY);
            result.remove(SHORT_TERM_MEMORY);
        }
        if (!policy.includeLongTermMemory()) {
            result.remove(LONG_TERM_MEMORY);
        }
        if (!policy.includeRetrievedKnowledge()) {
            result.remove(RETRIEVED_KNOWLEDGE);
            result.remove(RAG_CONTEXT);
            result.remove(RAG_RESULTS);
            result.remove("rag_hit_count");
            result.remove("rag_no_hit");
        }
        return result;
    }

    /** 计算上下文策略配置 */
    private ContextPolicy contextPolicy(JsonNode config, Map<String, Object> variables) {
        JsonNode policy = config.path(CONTEXT_POLICY);
        boolean hasHistory = variables != null && (variables.containsKey(CONVERSATION_HISTORY) || variables.containsKey(SHORT_TERM_MEMORY));
        boolean hasLongTermMemory = variables != null && variables.containsKey(LONG_TERM_MEMORY);
        boolean hasRetrievedKnowledge = variables != null && (variables.containsKey(RAG_RESULTS)
                || variables.containsKey(RAG_CONTEXT) || variables.containsKey(RETRIEVED_KNOWLEDGE));

        return new ContextPolicy(
                policy.path(INCLUDE_SHORT_TERM_MEMORY).asBoolean(hasHistory),
                policy.path(INCLUDE_LONG_TERM_MEMORY).asBoolean(hasLongTermMemory),
                policy.path(INCLUDE_RETRIEVED_KNOWLEDGE).asBoolean(hasRetrievedKnowledge),
                Math.max(1, policy.path("memoryWindow").asInt(config.path("memoryWindow").asInt(DEFAULT_MEMORY_WINDOW)))
        );
    }

    /** 生成上下文使用小结 */
    private ContextUsage contextUsage(ContextPolicy policy, List<RagSearchResult> references) {
        double topScore = references.stream().mapToDouble(RagSearchResult::score).max().orElse(0D);
        List<ContextSource> sources = references.stream()
                .map(item -> new ContextSource(item.sourceLabel(), item.documentId(), item.score()))
                .toList();

        return new ContextUsage(
                policy.includeShortTermMemory(),
                policy.includeLongTermMemory(),
                policy.includeRetrievedKnowledge(),
                policy.memoryWindow(),
                references.size(),
                topScore,
                sources
        );
    }

    /** 判断 Prompt 模板是否包含 RAG 占位符 */
    private boolean usesRagContext(JsonNode config) {
        String template = config.path("promptTemplate").asText("");
        return template.contains(PromptPlaceholders.RAG_CONTEXT) || template.contains(RAG_CONTEXT);
    }

    /** 提取变量中的 RAG 搜索引用列表 */
    @SuppressWarnings("unchecked")
    private List<RagSearchResult> extractReferences(Map<String, Object> variables) {
        Object raw = variables.get(RAG_RESULTS);
        if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof RagSearchResult) {
            return (List<RagSearchResult>) list;
        }
        return List.of();
    }

    /** 构造多模态 UserMessage（支持图像和文本组合） */
    private UserMessage buildUserMessage(JsonNode config, Map<String, Object> variables, String prompt) {
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(prompt));
        JsonNode images = config.path("imageVariables");
        if (images.isArray()) {
            for (JsonNode imageVariable : images) {
                Object raw = variables.get(imageVariable.asText());
                String location = raw instanceof Map<?, ?> map
                        ? String.valueOf(map.containsKey("url") ? map.get("url") : map.get("uri"))
                        : String.valueOf(raw);
                if (raw == null || location.isBlank() || "null".equals(location)) {
                    throw new IllegalArgumentException("当前视觉图像输入变量不可用：" + imageVariable.asText());
                }
                contents.add(ImageContent.from(location));
            }
        }
        return UserMessage.from(contents);
    }

    /** 替换模板变量 */
    private String render(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> item : variables.entrySet()) {
            if (RAG_RESULTS.equals(item.getKey())) {
                continue;
            }
            result = result.replace("{{" + item.getKey() + "}}", String.valueOf(item.getValue()));
        }
        return result;
    }

    /** 校验结构化 JSON 输出是否契合 Schema 要求 */
    private void validateStructured(JsonNode schema, String text) throws Exception {
        if (schema == null || schema.isMissingNode() || schema.isNull() || schema.isEmpty()) {
            return;
        }
        JsonNode parsed = objectMapper.readTree(text);
        String type = schema.path("type").asText("object");
        if (("object".equals(type) && !parsed.isObject()) || ("array".equals(type) && !parsed.isArray())
                || ("string".equals(type) && !parsed.isTextual()) || ("number".equals(type) && !parsed.isNumber())
                || ("boolean".equals(type) && !parsed.isBoolean())) {
            throw new IllegalArgumentException("模型生成的结构化输出格式类型不匹配，预期类型：" + type);
        }
        for (JsonNode required : schema.path("required")) {
            if (!parsed.has(required.asText())) {
                throw new IllegalArgumentException("模型生成的结构化输出缺失必填 JSON 字段：" + required.asText());
            }
        }
    }

    /** LLM 执行结果 Record */
    public record Result(String text, String modelKey, int attempt, ContextUsage contextUsage) {
    }

    /** 上下文使用明细 Record */
    public record ContextUsage(
            boolean shortTermMemoryUsed,
            boolean longTermMemoryUsed,
            boolean retrievedKnowledgeUsed,
            int memoryWindow,
            int retrievedCount,
            double topScore,
            List<ContextSource> sources
    ) { }

    /** 上下文引用来源 Record */
    public record ContextSource(String source, Long documentId, double score) {
    }

    /** 内部上下文策略 Record */
    private record ContextPolicy(
            boolean includeShortTermMemory,
            boolean includeLongTermMemory,
            boolean includeRetrievedKnowledge,
            int memoryWindow
    ) { }
}

