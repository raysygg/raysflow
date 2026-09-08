package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.application.application.ApplicationContracts;
import com.acme.agentstudio.application.application.ApplicationEntrypointService;
import com.acme.agentstudio.application.lifecycle.ApplicationLifecycleSummaryService;
import com.acme.agentstudio.application.workflow.OrchestrationAuthorizationService;
import com.acme.agentstudio.application.workflow.OrchestrationQueryService;
import com.acme.agentstudio.application.workflow.WorkflowDependencyResolver;
import com.acme.agentstudio.config.SecurityUser;
import com.acme.agentstudio.domain.application.ApplicationEntrypointContracts.EntrypointConfiguration;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.RuntimeReadiness;
import com.acme.agentstudio.domain.lifecycle.ApplicationLifecycleContracts.LifecycleSummary;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.workflow.model.OrchestrationVersionSummary;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationAppEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.OrchestrationDraftRevisionEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationAppMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.OrchestrationDraftRevisionMapper;
import com.acme.agentstudio.infrastructure.workflow.GraphDefinitionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运行时应用配置与面向前端的配置 Tab 聚合服务（Runtime Application Configuration Service）。
 * 聚合基础入口、发布版本历史、依赖图元数据、上下文策略（Memory / RAG / Prompt）以及各 RuntimeMode 模式的生产可用度诊断（RuntimeReadiness），
 * 并提供安全的只读 Prompt 预览渲染与 Model 参数解析能力。
 */
@Service
public class RuntimeApplicationConfigurationService {

    /** 运行时默认响应 Tab 键 */
    private static final String TAB_RUNTIME = "runtime";

    /** Prompt 配置 Tab 键 */
    private static final String TAB_PROMPT = "prompt";

    /** 工具配置 Tab 键 */
    private static final String TAB_TOOLS = "tools";

    /** 记忆策略 Tab 键 */
    private static final String TAB_MEMORY = "memory";

    /** 知识库检索 Tab 键 */
    private static final String TAB_KNOWLEDGE = "knowledge";

    /** 编排流程 Tab 键 */
    private static final String TAB_FLOW = "flow";

    /** 配置页 Tab 中文标签映射 */
    private static final Map<String, String> TAB_LABELS = Map.of(
            TAB_RUNTIME, "默认响应方式",
            TAB_PROMPT, "Prompt",
            TAB_TOOLS, "工具",
            TAB_MEMORY, "记忆",
            TAB_KNOWLEDGE, "知识",
            TAB_FLOW, "流程"
    );

    /** 配置页 Tab 的展示顺序列表 */
    private static final List<String> TAB_ORDER = List.of(
            TAB_RUNTIME,
            TAB_PROMPT,
            TAB_TOOLS,
            TAB_MEMORY,
            TAB_KNOWLEDGE,
            TAB_FLOW
    );

    /** 默认生产环境标识 */
    private static final String DEFAULT_ENVIRONMENT = "PRODUCTION";

    /** 默认 Content-Type */
    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    /** 默认最大重试次数 */
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    /** 默认对话记忆历史消息窗口大小 */
    private static final int DEFAULT_MEMORY_WINDOW = 10;

    /** 默认 RAG 检索返回 Chunk 数量 */
    private static final int DEFAULT_RAG_TOP_K = 5;

    /** 默认 RAG 检索最低相似度分数阈值 */
    private static final double DEFAULT_RAG_SCORE_THRESHOLD = 0.6D;

    /** 默认大模型采样温度 */
    private static final double DEFAULT_MODEL_TEMPERATURE = 0.7D;

    /** 预览状态下的 RAG 上下文占位提醒 */
    private static final String PREVIEW_RAG_CONTEXT = "（预览未执行检索，正式运行将按 RAG 策略读取知识来源）";

    /** LLM 流程节点类型标识 */
    private static final String NODE_LLM = "LLM";

    /** Memory 节点类型标识 */
    private static final String NODE_MEMORY = "MEMORY";

    /** RAG 节点类型标识 */
    private static final String NODE_RAG = "RAG";

    /** 应用入口服务 */
    private final ApplicationEntrypointService entrypointService;

    /** 编排查询服务 */
    private final OrchestrationQueryService queryService;

    /** 应用 Mapper */
    private final OrchestrationAppMapper appMapper;

    /** 草稿版本 Mapper */
    private final OrchestrationDraftRevisionMapper draftMapper;

    /** 编排鉴权服务 */
    private final OrchestrationAuthorizationService authorizationService;

    /** Jackson JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** 流程依赖分析器 */
    private final WorkflowDependencyResolver dependencyResolver;

    /** 图拓扑解析器 */
    private final GraphDefinitionParser graphParser;

    /** 应用生命周期摘要服务 */
    private final ApplicationLifecycleSummaryService lifecycleSummaryService;

    /**
     * 构造函数注入所有依赖服务。
     */
    public RuntimeApplicationConfigurationService(
            ApplicationEntrypointService entrypointService,
            OrchestrationQueryService queryService,
            OrchestrationAppMapper appMapper,
            OrchestrationDraftRevisionMapper draftMapper,
            OrchestrationAuthorizationService authorizationService,
            ObjectMapper objectMapper,
            WorkflowDependencyResolver dependencyResolver,
            GraphDefinitionParser graphParser,
            ApplicationLifecycleSummaryService lifecycleSummaryService
    ) {
        this.entrypointService = entrypointService;
        this.queryService = queryService;
        this.appMapper = appMapper;
        this.draftMapper = draftMapper;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
        this.dependencyResolver = dependencyResolver;
        this.graphParser = graphParser;
        this.lifecycleSummaryService = lifecycleSummaryService;
    }

    /**
     * 查询构造应用配置页所需的完整聚合数据结构。
     *
     * @param user 当前登录 SecurityUser
     * @param appId 应用 ID
     * @return 导出的应用运行时配置对象 RuntimeApplicationConfiguration
     */
    public RuntimeApplicationConfiguration configuration(SecurityUser user, Long appId) {
        List<OrchestrationVersionSummary> versions = queryService.versions(user, appId);
        JsonNode draftGraph = readDraftGraph(user, appId);
        WorkflowDependencySnapshot dependencies = resolveDependencies(user, draftGraph);

        return new RuntimeApplicationConfiguration(
                TAB_ORDER.stream().map(tab -> new RuntimeTab(tab, TAB_LABELS.get(tab))).toList(),
                entrypointService.list(user, appId),
                versions,
                buildSpec(user, appId, versions, dependencies),
                contextConfiguration(draftGraph),
                dependencies,
                Arrays.stream(RuntimeMode.values()).map(this::runtimeCapability).toList(),
                lifecycleSummaryService.summarize(user, appId)
        );
    }

    /** 推导当前运行模式的可用性级别与中文引导文案 */
    private RuntimeCapability runtimeCapability(RuntimeMode mode) {
        RuntimeReadiness readiness = switch (mode) {
            case CHAT, WORKFLOW -> RuntimeReadiness.PRODUCTION_READY;
            case REACT, PLAN -> RuntimeReadiness.EXPERIMENTAL;
            case MULTI_AGENT -> RuntimeReadiness.NOT_READY;
        };
        String guidance = switch (readiness) {
            case PRODUCTION_READY -> "功能稳定，推荐用于生产发布使用";
            case EXPERIMENTAL -> "处于实验阶段，仅用于草稿测试，生产推荐使用 Chat 或 Workflow";
            case NOT_READY -> "暂未就绪，当前不可用于生产，请切换为 Workflow 模式";
        };
        return new RuntimeCapability(mode, readiness, guidance);
    }

    /** 解析草稿节点的依赖快照 */
    private WorkflowDependencySnapshot resolveDependencies(SecurityUser user, JsonNode graph) {
        WorkflowDependencyResolver.Resolution resolution = dependencyResolver.resolve(
                user.getTenantId(),
                graphParser.normalize(graph)
        );
        return resolution.snapshot();
    }

    /** 应用运行时配置页面传输聚合 Record */
    public record RuntimeApplicationConfiguration(
            List<RuntimeTab> tabs,
            List<EntrypointConfiguration> entrypoints,
            List<OrchestrationVersionSummary> versions,
            ApplicationContracts.ApplicationSpec spec,
            ContextConfiguration contextConfiguration,
            WorkflowDependencySnapshot dependencies,
            List<RuntimeCapability> runtimeCapabilities,
            LifecycleSummary lifecycle
    ) {
    }

    /** 运行模式能力描述 Record */
    public record RuntimeCapability(RuntimeMode mode, RuntimeReadiness readiness, String guidance) {
    }

    /** 配置页 Tab 定义 Record */
    public record RuntimeTab(String key, String label) {
    }

    /**
     * 仅提取当前草稿的 Prompt 模板并渲染预览，不产生真实 LLM 调用或侧效应。
     *
     * @param user 当前登录 SecurityUser
     * @param appId 应用 ID
     * @param input 输入变量 Map
     * @return 预览结果对象 PromptPreview
     */
    public PromptPreview previewPrompt(SecurityUser user, Long appId, Map<String, Object> input) {
        JsonNode graph = readDraftGraph(user, appId);
        JsonNode llmConfig = findLlmConfig(graph);
        if (llmConfig == null || llmConfig.isMissingNode()) {
            throw new IllegalArgumentException("当前流程草稿中未包含任何 LLM 节点，无法配置与预览 Prompt。");
        }
        String template = llmConfig.path("promptTemplate").asText("");
        if (template.isBlank()) {
            throw new IllegalArgumentException("当前草稿中的 LLM 节点尚未配置 Prompt 提示词模板。");
        }

        Map<String, Object> variables = new LinkedHashMap<>((input == null) ? Map.of() : input);
        Object message = variables.getOrDefault("user_message", variables.getOrDefault("input", variables.getOrDefault("query", "")));

        variables.putIfAbsent("input", message);
        variables.putIfAbsent("query", message);
        variables.putIfAbsent("user_message", message);
        variables.putIfAbsent("rag_context", PREVIEW_RAG_CONTEXT);

        String rendered = template;
        for (Map.Entry<String, Object> variable : variables.entrySet()) {
            rendered = rendered.replace("{{" + variable.getKey() + "}}", String.valueOf(variable.getValue()));
        }

        List<String> contextSources = List.of(
                "input",
                "variables",
                "shortTermMemory",
                "longTermMemory",
                "retrievedKnowledge"
        );

        return new PromptPreview(
                template,
                rendered,
                variables,
                contextSources,
                modelParameters(llmConfig)
        );
    }

    /** 寻找流程图中的首个 LLM 节点配置 */
    private JsonNode findLlmConfig(JsonNode graph) {
        for (JsonNode node : graph.path("nodes")) {
            String type = node.path("nodeType").asText(node.path("type").asText(""));
            if (NODE_LLM.equals(type)) {
                return node.path("config");
            }
        }
        return null;
    }

    /** 从节点 JSON 节点提取模型控制参数 */
    private ModelParameters modelParameters(JsonNode config) {
        return new ModelParameters(
                config.path("modelKey").asText(config.path("modelId").asText("未配置")),
                config.path("backupModelKey").asText("未配置"),
                config.path("temperature").asDouble(DEFAULT_MODEL_TEMPERATURE),
                config.path("maxTokens").asInt(0),
                config.path("maxContextChars").asInt(0)
        );
    }

    /** Prompt 预览结构 Record */
    public record PromptPreview(
            String template,
            String renderedPrompt,
            Map<String, Object> variables,
            List<String> contextSources,
            ModelParameters modelParameters
    ) {
    }

    /** 模型参数 Record */
    public record ModelParameters(
            String modelKey,
            String backupModelKey,
            double temperature,
            int maxTokens,
            int maxContextChars
    ) {
    }

    /**
     * 聚合应用当前草稿的上下文与记忆策略。
     */
    private ContextConfiguration contextConfiguration(JsonNode graph) {
        boolean hasMemoryNode = false;
        boolean hasRagNode = false;
        boolean hasShortTermMemory = false;
        boolean hasLongTermMemory = false;
        int memoryWindow = DEFAULT_MEMORY_WINDOW;
        int ragTopK = DEFAULT_RAG_TOP_K;
        double ragScoreThreshold = DEFAULT_RAG_SCORE_THRESHOLD;
        String ragRetrievalMode = "HYBRID";
        String promptTemplate = "";

        for (JsonNode node : graph.path("nodes")) {
            String type = node.path("nodeType").asText(node.path("type").asText(""));
            JsonNode config = node.path("config");

            if (NODE_LLM.equals(type) && promptTemplate.isBlank()) {
                promptTemplate = config.path("promptTemplate").asText("");
            }
            if (NODE_MEMORY.equals(type) || config.has("memoryWindow")) {
                hasMemoryNode = true;
                memoryWindow = config.path("memoryWindow").asInt(DEFAULT_MEMORY_WINDOW);
                hasShortTermMemory = !config.path("shortTermEnabled").isBoolean()
                        || config.path("shortTermEnabled").asBoolean();
                hasLongTermMemory = config.path("longTermEnabled").asBoolean(false);
            }
            if (NODE_RAG.equals(type)) {
                hasRagNode = true;
                ragTopK = config.path("topK").asInt(DEFAULT_RAG_TOP_K);
                ragScoreThreshold = config.path("scoreThreshold").asDouble(DEFAULT_RAG_SCORE_THRESHOLD);
                ragRetrievalMode = config.path("retrievalMode").asText(ragRetrievalMode);
            }
        }

        return new ContextConfiguration(
                new ContextStrategyConfiguration(
                        true,
                        true,
                        hasShortTermMemory,
                        hasLongTermMemory,
                        hasRagNode,
                        List.of("LONG_TERM_MEMORY", "SHORT_TERM_MEMORY", "RETRIEVED_KNOWLEDGE", "VARIABLES", "INPUT")
                ),
                new MemoryStrategyConfiguration(
                        hasMemoryNode,
                        hasShortTermMemory,
                        hasLongTermMemory,
                        memoryWindow,
                        hasLongTermMemory ? "EXPLICIT_WRITE" : "READ_ONLY"
                ),
                new PromptConfiguration(
                        promptTemplate,
                        List.of("input", "variables", "shortTermMemory", "longTermMemory", "retrievedKnowledge")
                ),
                new RagConfiguration(hasRagNode, ragRetrievalMode, ragTopK, ragScoreThreshold)
        );
    }

    /** 读取草稿图 JSON 结构 */
    private JsonNode readDraftGraph(SecurityUser user, Long appId) {
        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationDraftRevisionEntity::getAppId, appId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo)
                .last("LIMIT 1"));
        return readGraph((draft == null) ? null : draft.getGraphJson());
    }

    /** 上下文组合配置整体框架 Record */
    public record ContextConfiguration(
            ContextStrategyConfiguration context,
            MemoryStrategyConfiguration memory,
            PromptConfiguration prompt,
            RagConfiguration rag
    ) {
    }

    /** 上下文依赖优先级 Record */
    public record ContextStrategyConfiguration(
            boolean inputEnabled,
            boolean variablesEnabled,
            boolean shortTermMemoryEnabled,
            boolean longTermMemoryEnabled,
            boolean retrievedKnowledgeEnabled,
            List<String> priority
    ) {
    }

    /** 记忆策略 Record */
    public record MemoryStrategyConfiguration(
            boolean configured,
            boolean shortTermEnabled,
            boolean longTermEnabled,
            int windowMessages,
            String writePolicy
    ) {
    }

    /** Prompt 配置 Record */
    public record PromptConfiguration(String template, List<String> variableSources) {
    }

    /** RAG 配置 Record */
    public record RagConfiguration(
            boolean enabled,
            String retrievalMode,
            int topK,
            double scoreThreshold
    ) {
    }

    /** 从元数据与依赖构建应用规约 ApplicationSpec */
    private ApplicationContracts.ApplicationSpec buildSpec(
            SecurityUser user,
            Long appId,
            List<OrchestrationVersionSummary> versions,
            WorkflowDependencySnapshot dependencies
    ) {
        OrchestrationAppEntity application = appMapper.selectOne(new LambdaQueryWrapper<OrchestrationAppEntity>()
                .eq(OrchestrationAppEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationAppEntity::getId, appId));

        if (application == null) {
            throw new IllegalArgumentException("未找到当前企业账号归属下的目标业务应用。");
        }

        OrchestrationDraftRevisionEntity draft = draftMapper.selectOne(new LambdaQueryWrapper<OrchestrationDraftRevisionEntity>()
                .eq(OrchestrationDraftRevisionEntity::getTenantId, user.getTenantId())
                .eq(OrchestrationDraftRevisionEntity::getAppId, appId)
                .orderByDesc(OrchestrationDraftRevisionEntity::getRevisionNo)
                .last("LIMIT 1"));

        OrchestrationVersionSummary release = currentRelease(versions);
        JsonNode graph = readGraph((draft == null) ? null : draft.getGraphJson());

        ApplicationContracts.ReleaseSummary releaseSummary = (release == null) ? null
                : new ApplicationContracts.ReleaseSummary(
                release.versionId(),
                release.environmentCode(),
                release.status(),
                release.releasedAt(),
                release.releaseBundleHash()
        );

        return new ApplicationContracts.ApplicationSpec(
                new ApplicationContracts.ApplicationIdentity(
                        application.getId(),
                        application.getAppCode(),
                        application.getAppName(),
                        application.getStatus()
                ),
                new ApplicationContracts.InputProtocol(
                        DEFAULT_CONTENT_TYPE,
                        protocolFields(graph.path("inputSchema"), "input", "object", true, "业务请求输入。")
                ),
                new ApplicationContracts.OutputProtocol(
                        DEFAULT_CONTENT_TYPE,
                        protocolFields(graph.path("outputSchema"), "output", "object", false, "Runtime Run 最终输出。")
                ),
                new ApplicationContracts.RuntimePolicy(DEFAULT_ENVIRONMENT, true, DEFAULT_MAX_ATTEMPTS),
                new ApplicationContracts.WorkflowBinding(
                        application.getGraphType(),
                        (draft == null) ? null : draft.getRevisionNo(),
                        releaseSummary
                ),
                dependencies,
                configurationSources(graph)
        );
    }

    /** 获取活动生产版本 */
    private OrchestrationVersionSummary currentRelease(List<OrchestrationVersionSummary> versions) {
        return versions.stream()
                .filter(OrchestrationVersionSummary::current)
                .findFirst()
                .orElseGet(() -> versions.stream()
                        .filter(version -> "PUBLISHED".equals(version.status()))
                        .findFirst()
                        .orElse(null));
    }

    /** 从 JSON Schema 解析协议字段定义 */
    private List<ApplicationContracts.ProtocolField> protocolFields(
            JsonNode schema,
            String defaultName,
            String defaultType,
            boolean defaultRequired,
            String defaultDescription
    ) {
        if (schema != null && schema.isObject() && schema.path("properties").isObject()) {
            List<ApplicationContracts.ProtocolField> fields = new ArrayList<>();
            for (Iterator<Map.Entry<String, JsonNode>> iterator = schema.path("properties").fields(); iterator.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String name = entry.getKey();
                JsonNode definition = entry.getValue();

                boolean required = schema.path("required").isArray() && contains(schema.path("required"), name);
                fields.add(new ApplicationContracts.ProtocolField(
                        name,
                        definition.path("type").asText(defaultType),
                        required,
                        definition.path("description").asText(""),
                        values(definition.path("enum")),
                        definition.path("x-resourceType").asText(null)
                ));
            }
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        return List.of(new ApplicationContracts.ProtocolField(defaultName, defaultType, defaultRequired, defaultDescription));
    }

    /** 将 JSON 数组转换为协议字段的字符串选项。 */
    private List<String> values(JsonNode values) {
        if (values == null || !values.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        values.forEach(value -> result.add(value.asText()));
        return List.copyOf(result);
    }

    /** 检查节点分布并推导配置源层次 */
    private ApplicationContracts.ConfigurationSources configurationSources(JsonNode graph) {
        boolean hasPrompt = false;
        boolean hasModel = false;
        boolean hasKnowledge = false;
        boolean hasMemory = false;
        boolean hasTools = false;

        for (JsonNode node : graph.path("nodes")) {
            String type = node.path("nodeType").asText(node.path("type").asText(""));
            JsonNode config = node.path("config");

            if ("LLM".equals(type) && !config.path("promptTemplate").asText("").isBlank()) {
                hasPrompt = true;
            }
            if ("LLM".equals(type) && (!config.path("modelId").asText("").isBlank() || !config.path("modelKey").asText("").isBlank())) {
                hasModel = true;
            }
            if ("RAG".equals(type)) {
                hasKnowledge = true;
            }
            if (!config.path("memoryWindow").isMissingNode() || "MEMORY".equals(type)) {
                hasMemory = true;
            }
            if (Set.of("HTTP_REQUEST", "MCP", "OPENAPI", "TOOL").contains(type)) {
                hasTools = true;
            }
        }

        return new ApplicationContracts.ConfigurationSources(
                hasPrompt ? "workflow" : "application",
                hasModel ? "workflow" : "application",
                hasKnowledge ? "workflow" : "runtime",
                hasMemory ? "workflow" : "application",
                hasKnowledge ? "workflow" : "application",
                hasTools ? "workflow" : "application"
        );
    }

    /** 判重工具类 */
    private boolean contains(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (expected.equals(value.asText())) {
                return true;
            }
        }
        return false;
    }

    /** 解析图 JSON 树节点 */
    private JsonNode readGraph(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(graphJson);
        } catch (Exception exception) {
            throw new IllegalStateException("应用草稿流程 JSON 文本无法解析为树节点结构。", exception);
        }
    }
}

