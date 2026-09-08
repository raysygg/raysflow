package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.knowledge.model.KnowledgeLanguage;
import com.acme.agentstudio.domain.knowledge.model.RagModelSelection;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalOutcome;
import com.acme.agentstudio.domain.knowledge.model.RagRetrievalRequest;
import com.acme.agentstudio.domain.knowledge.model.RagSearchResult;
import com.acme.agentstudio.domain.knowledge.model.RetrievalLanguageStrategy;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalMetricService;
import com.acme.agentstudio.infrastructure.rag.RagRetrievalService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识检索节点执行器（RagNodeExecutor）。
 * 将工作流节点的 JSON 配置转换成统一的 RAG 检索请求，调用基础设施层向量数据库与混合检索模型，并记录检索质量与召回指标。
 */
@Service
public class RagNodeExecutor {

    /** 配置项：查询 Query 变量表达式 */
    private static final String CONFIG_QUERY = "query";

    /** 配置项：检索范围 */
    private static final String CONFIG_SCOPE = "retrievalScope";

    /** 配置项：关联文档列表 */
    private static final String CONFIG_DOCUMENT_IDS = "knowledgeDocumentIds";

    /** 配置项：多语言策略 */
    private static final String CONFIG_LANGUAGE_STRATEGY = "languageStrategy";

    /** 配置项：查询目标语言 */
    private static final String CONFIG_QUERY_LANGUAGE = "queryLanguage";

    /** 配置项：Top K 召回条数 */
    private static final String CONFIG_TOP_K = "topK";

    /** 配置项：Embedding 模型来源 */
    private static final String CONFIG_EMBEDDING_MODEL_SOURCE = "embeddingModelSource";

    /** 配置项：Embedding 模型 ID */
    private static final String CONFIG_EMBEDDING_MODEL_ID = "embeddingModelId";

    /** 配置项：Embedding 模型 Key */
    private static final String CONFIG_EMBEDDING_MODEL_KEY = "embeddingModelKey";

    /** RAG 统一检索服务 */
    private final RagRetrievalService retrievalService;

    /** RAG 检索指标收集服务 */
    private final RagRetrievalMetricService metricService;

    /**
     * 构造函数注入依赖服务。
     */
    public RagNodeExecutor(RagRetrievalService retrievalService, RagRetrievalMetricService metricService) {
        this.retrievalService = retrievalService;
        this.metricService = metricService;
    }

    /**
     * 执行 RAG 知识检索节点。
     *
     * @param tenantId 租户 ID
     * @param config 节点 JSON 配置
     * @param variables 节点当前可用变量 Map
     * @return 检索结果包裹对象 Result
     */
    public Result execute(Long tenantId, JsonNode config, Map<String, Object> variables) {
        String query = query(config, variables);
        RagRetrievalOutcome outcome = retrievalService.retrieveWithContext(new RagRetrievalRequest(
                tenantId,
                null,
                query,
                languageStrategy(config),
                queryLanguage(config),
                scope(config),
                documentIds(config),
                config.path(CONFIG_TOP_K).asInt(5),
                modelSelection(config)
        ));
        metricService.record(tenantId, "ORCHESTRATION_RAG", outcome, false);
        return Result.from(outcome);
    }

    /** 从节点配置或全局输入变量中解析检索 Query */
    private String query(JsonNode config, Map<String, Object> variables) {
        String fallback = String.valueOf(variables.getOrDefault(
                WorkflowVariableNames.VARIABLE_QUERY,
                variables.getOrDefault(WorkflowVariableNames.VARIABLE_INPUT, "")
        ));
        return config.path(CONFIG_QUERY).asText(fallback);
    }

    /** 解析语言策略 */
    private RetrievalLanguageStrategy languageStrategy(JsonNode config) {
        return enumValue(config.path(CONFIG_LANGUAGE_STRATEGY).asText(), RetrievalLanguageStrategy.class, RetrievalLanguageStrategy.AUTO);
    }

    /** 解析目标查询语言 */
    private KnowledgeLanguage queryLanguage(JsonNode config) {
        return enumValue(config.path(CONFIG_QUERY_LANGUAGE).asText(), KnowledgeLanguage.class, null);
    }

    /** 解析检索范围类型 */
    private RetrievalScopeType scope(JsonNode config) {
        return enumValue(config.path(CONFIG_SCOPE).asText(), RetrievalScopeType.class, RetrievalScopeType.VISIBLE_DOCUMENTS);
    }

    /** 解析限制关联的文档 ID 列表 */
    private List<Long> documentIds(JsonNode config) {
        List<Long> ids = new ArrayList<>();
        JsonNode values = config.path(CONFIG_DOCUMENT_IDS);
        if (values.isArray()) {
            values.forEach(value -> {
                Long id = parseLong(value);
                if (id != null) {
                    ids.add(id);
                }
            });
        }
        return List.copyOf(ids);
    }

    /** 解析节点冻结的模型引用 */
    private RagModelSelection modelSelection(JsonNode config) {
        RagModelSource source = enumValue(config.path(CONFIG_EMBEDDING_MODEL_SOURCE).asText(), RagModelSource.class, null);
        if (source == null) {
            return null;
        }
        Long modelId = parseLong(config.path(CONFIG_EMBEDDING_MODEL_ID));
        String modelKey = config.path(CONFIG_EMBEDDING_MODEL_KEY).asText(null);
        return new RagModelSelection(source, modelId, modelKey);
    }

    /** 安全解析数字或数字字符串为 Long，防止 Jackson 对 TextNode 调用 canConvertToLong 返回 false */
    private Long parseLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                String text = node.asText().trim();
                return text.isEmpty() ? null : Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 通用 Enum 转换小工具 */
    private <T extends Enum<T>> T enumValue(String value, Class<T> type, T fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    /** RAG 检索输出结果 Record */
    public record Result(
            String context,
            List<RagCitation> citations,
            List<RagSearchResult> results,
            RagRetrievalOutcome retrieval,
            RetrievalSummary summary
    ) {
        static Result from(RagRetrievalOutcome outcome) {
            List<RagCitation> citations = outcome.results().stream().map(RagCitation::from).toList();
            String rawContext = outcome.results().stream()
                    .map(result -> result.sourceLabel() + "\n" + result.text())
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElse("（无命中知识片段）");

            String languageName = outcome.actualLanguage() != null ? outcome.actualLanguage().name() : "中文";
            String alignmentInstruction = String.format(
                    "\n\n【回答语言规范】：用户提问语言为【%s】。无论上述参考知识切块为何种语言，生成最终回答时必须统一使用【%s】进行解答与输出。",
                    languageName,
                    languageName
            );
            String context = rawContext.equals("（无命中知识片段）") ? rawContext : rawContext + alignmentInstruction;

            return new Result(context, citations, outcome.results(), outcome, RetrievalSummary.from(outcome));
        }
    }

    /** RAG 检索统计摘要 Record */
    public record RetrievalSummary(
            KnowledgeLanguage actualLanguage,
            String languageSource,
            String scope,
            List<String> channels,
            int initialCandidateCount,
            int rerankedCandidateCount,
            int hitCount,
            double topScore,
            double scoreGap,
            boolean queryRewritten,
            boolean degraded,
            String embeddingProfile,
            Long indexGenerationId,
            double parentCoverage,
            long elapsedMs
    ) {
        static RetrievalSummary from(RagRetrievalOutcome outcome) {
            return new RetrievalSummary(
                    outcome.actualLanguage(),
                    outcome.languageSource().name(),
                    outcome.scope().name(),
                    outcome.channels().stream().map(Enum::name).toList(),
                    outcome.initialCandidateCount(),
                    outcome.rerankedCandidateCount(),
                    outcome.hitCount(),
                    outcome.topScore(),
                    outcome.scoreGap(),
                    outcome.queryRewritten(),
                    outcome.degraded(),
                    outcome.embeddingProfile(),
                    outcome.indexGenerationId(),
                    outcome.parentCoverage(),
                    outcome.elapsedMs()
            );
        }
    }

    /** RAG 检索引用详情 Record */
    public record RagCitation(
            String source,
            Long documentId,
            double score,
            String text,
            int chunkStart,
            int chunkEnd,
            KnowledgeLanguage language
    ) {
        static RagCitation from(RagSearchResult result) {
            return new RagCitation(
                    result.source(),
                    result.documentId(),
                    result.score(),
                    result.text(),
                    result.chunkStart(),
                    result.chunkEnd(),
                    result.language()
            );
        }
    }
}

