package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.common.BusinessStatus;
import com.acme.agentstudio.domain.knowledge.model.RagModelSource;
import com.acme.agentstudio.domain.knowledge.model.RetrievalScopeType;
import com.acme.agentstudio.domain.model.ModelCapability;
import com.acme.agentstudio.domain.workflow.model.GraphDefinition;
import com.acme.agentstudio.domain.workflow.model.GraphNode;
import com.acme.agentstudio.domain.workflow.model.ModelTenantScope;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.PlatformToolConnectorEntity;
import com.acme.agentstudio.infrastructure.persistence.entity.SysModelConfigEntity;
import com.acme.agentstudio.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.PlatformToolConnectorMapper;
import com.acme.agentstudio.infrastructure.persistence.mapper.SysModelConfigMapper;
import com.acme.agentstudio.infrastructure.rag.model.LocalEmbeddingModel;
import com.acme.agentstudio.infrastructure.workflow.ValidationIssue;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot.DependencySource.LOCAL;
import static com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot.DependencySource.PLATFORM_SHARED;
import static com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot.DependencySource.TENANT_PRIVATE;

/**
 * 工作流静态图资源依赖分析与依赖快照生成器（Workflow Dependency Resolver）。
 * 负责扫描解析流程图中所引用的大语言模型（LLM）、RAG 嵌入模型（Embedding）、知识库文档（Knowledge Document）以及工具连接器（Platform Tool Connector），
 * 强校验依赖项的租户归属与生命周期状态，并生成固化的不可化发布依赖快照（WorkflowDependencySnapshot）。
 */
@Component
public class WorkflowDependencyResolver {

    /** 配置项字段：模型 Key */
    private static final String FIELD_MODEL_KEY = "modelKey";

    /** 配置项字段：模型 ID */
    private static final String FIELD_MODEL_ID = "modelId";

    /** 配置项字段：Embedding 模型来源 */
    private static final String FIELD_EMBEDDING_MODEL_SOURCE = "embeddingModelSource";

    /** 配置项字段：Embedding 模型 ID */
    private static final String FIELD_EMBEDDING_MODEL_ID = "embeddingModelId";

    /** 配置项字段：Embedding 模型 Key */
    private static final String FIELD_EMBEDDING_MODEL_KEY = "embeddingModelKey";

    /** 配置项字段：知识文档 ID 列表 */
    private static final String FIELD_DOCUMENT_IDS = "knowledgeDocumentIds";

    /** 配置项字段：连接器 ID */
    private static final String FIELD_CONNECTOR_ID = "connectorId";

    /** 配置项字段：检索范围类型 */
    private static final String FIELD_RETRIEVAL_SCOPE = "retrievalScope";

    /** RAG 节点类型字符串 */
    private static final String NODE_TYPE_RAG = "RAG";

    /** 本地 ONNX Embedding 模型提供商名称 */
    private static final String LOCAL_MODEL_PROVIDER = "LOCAL_ONNX";

    /** 大模型配置 Mapper */
    private final SysModelConfigMapper modelMapper;

    /** 知识文档 Mapper */
    private final KnowledgeDocumentMapper documentMapper;

    /** 工具连接器 Mapper */
    private final PlatformToolConnectorMapper connectorMapper;

    /**
     * 构造函数注入资源 Mapper 依赖。
     */
    public WorkflowDependencyResolver(
            SysModelConfigMapper modelMapper,
            KnowledgeDocumentMapper documentMapper,
            PlatformToolConnectorMapper connectorMapper
    ) {
        this.modelMapper = modelMapper;
        this.documentMapper = documentMapper;
        this.connectorMapper = connectorMapper;
    }

    /**
     * 扫描解析并强校验工作流图中所引用的全部依赖项，返回依赖快照与校验缺陷报告。
     *
     * @param tenantId 当前租户 ID
     * @param graph 包含节点与连线定义的工作流图
     * @return 包含依赖快照与校验 Issue 的 Resolution 结果对象
     */
    public Resolution resolve(Long tenantId, GraphDefinition graph) {
        List<WorkflowDependencySnapshot.ModelDependencyReference> models = new ArrayList<>();
        List<WorkflowDependencySnapshot.DocumentDependencyReference> documents = new ArrayList<>();
        List<WorkflowDependencySnapshot.ConnectorDependencyReference> connectors = new ArrayList<>();
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (GraphNode node : graph.nodes()) {
            resolveModel(tenantId, node, models, issues, seen);
            resolveDocuments(tenantId, node, documents, issues, seen);
            resolveConnector(tenantId, node, connectors, issues, seen);
        }

        return new Resolution(new WorkflowDependencySnapshot(models, documents, connectors), issues);
    }

    /** 解析大语言模型或 Embedding 模型依赖 */
    private void resolveModel(
            Long tenantId,
            GraphNode node,
            List<WorkflowDependencySnapshot.ModelDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        if (NODE_TYPE_RAG.equals(node.nodeType())) {
            resolveRagEmbeddingModel(tenantId, node, dependencies, issues, seen);
            return;
        }

        String reference = text(node.config(), FIELD_MODEL_KEY);
        String field = FIELD_MODEL_KEY;
        if (reference == null) {
            reference = text(node.config(), FIELD_MODEL_ID);
            field = FIELD_MODEL_ID;
        }
        if (reference == null) {
            return;
        }

        SysModelConfigEntity model = findModel(tenantId, reference);
        if (model == null) {
            issues.add(issue("MODEL_UNAVAILABLE", node.nodeId(), field,
                    "模型引用不存在、未启用或不属于当前租户。", "请重新选择租户私有模型或平台共享模型。"));
            return;
        }

        String key = "MODEL:" + node.nodeId() + ":" + model.getId();
        if (seen.add(key)) {
            dependencies.add(new WorkflowDependencySnapshot.ModelDependencyReference(
                    node.nodeId(),
                    model.getId(),
                    model.getModelKey(),
                    model.getModelName(),
                    model.getProvider(),
                    ModelTenantScope.PLATFORM_TENANT_ID == model.getTenantId() ? PLATFORM_SHARED : TENANT_PRIVATE,
                    model.getTenantId(),
                    model.getStatus()
            ));
        }
    }

    /** 解析知识库文档依赖 */
    private void resolveDocuments(
            Long tenantId,
            GraphNode node,
            List<WorkflowDependencySnapshot.DocumentDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        RetrievalScopeType scope = scope(node);
        List<Long> documentIds = longValues(node.config(), FIELD_DOCUMENT_IDS);
        if (NODE_TYPE_RAG.equals(node.nodeType()) && scope == RetrievalScopeType.EXPLICIT_DOCUMENTS && documentIds.isEmpty()) {
            issues.add(issue("KNOWLEDGE_DOCUMENTS_REQUIRED", node.nodeId(), FIELD_DOCUMENT_IDS,
                    "指定文档范围类型至少需要指定一个有效知识文档。", "请选择已完成切块与向量化的知识文档，或切换为可见文档范围。"));
        }

        for (Long documentId : documentIds) {
            KnowledgeDocumentEntity document = documentMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                            .eq(KnowledgeDocumentEntity::getTenantId, tenantId)
                            .eq(KnowledgeDocumentEntity::getId, documentId)
            );
            if (document == null || !BusinessStatus.INDEXED.equals(document.getDocumentStatus())) {
                issues.add(issue("KNOWLEDGE_DOCUMENT_UNAVAILABLE", node.nodeId(), FIELD_DOCUMENT_IDS,
                        "关联的知识文档不存在、尚未索引完成或不属于当前租户。", "请在知识库中重新选择已就绪的文档。"));
                continue;
            }

            String key = "DOCUMENT:" + node.nodeId() + ":" + documentId;
            if (seen.add(key)) {
                dependencies.add(new WorkflowDependencySnapshot.DocumentDependencyReference(
                        node.nodeId(),
                        documentId,
                        document.getTitle(),
                        document.getDocumentStatus(),
                        document.getLanguage(),
                        Boolean.TRUE.equals(document.getLanguageConfirmed())
                ));
            }

            if (NODE_TYPE_RAG.equals(node.nodeType()) && !Boolean.TRUE.equals(document.getLanguageConfirmed())) {
                issues.add(issue("KNOWLEDGE_LANGUAGE_UNCONFIRMED", node.nodeId(), FIELD_DOCUMENT_IDS,
                        "知识文档的提炼语言尚未在控制台中进行人工确认。", "请在文档管理页面中确认语言标注后再发布工作流。"));
            }
        }
    }

    /** 分流解析 RAG Embedding 模型依赖 */
    private void resolveRagEmbeddingModel(
            Long tenantId,
            GraphNode node,
            List<WorkflowDependencySnapshot.ModelDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        RagModelSource source = ragModelSource(node, issues);
        if (source == null) {
            return;
        }
        if (source == RagModelSource.LOCAL) {
            resolveLocalEmbeddingModel(node, dependencies, issues, seen);
            return;
        }
        resolveRemoteEmbeddingModel(tenantId, node, source, dependencies, issues, seen);
    }

    /** 解析 Embedding 来源配置 */
    private RagModelSource ragModelSource(GraphNode node, List<ValidationIssue> issues) {
        String value = text(node.config(), FIELD_EMBEDDING_MODEL_SOURCE);
        if (value == null) {
            issues.add(issue("RAG_EMBEDDING_MODEL_REQUIRED", node.nodeId(), FIELD_EMBEDDING_MODEL_SOURCE,
                    "RAG 检索节点尚未配置 Embedding 向量模型。", "请从模型配置中选择租户私有、平台共享或本地内置模型。"));
            return null;
        }
        try {
            return RagModelSource.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            issues.add(issue("RAG_EMBEDDING_SOURCE_INVALID", node.nodeId(), FIELD_EMBEDDING_MODEL_SOURCE,
                    "RAG 节点的 Embedding 模型来源字符串非法。", "请在节点配置面板中重新选择 Embedding 模型。"));
            return null;
        }
    }

    /** 解析平台内置 ONNX 本地 Embedding 模型 */
    private void resolveLocalEmbeddingModel(
            GraphNode node,
            List<WorkflowDependencySnapshot.ModelDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        String modelKey = text(node.config(), FIELD_EMBEDDING_MODEL_KEY);
        try {
            LocalEmbeddingModel model = LocalEmbeddingModel.require(modelKey);
            String key = "RAG_MODEL:" + node.nodeId() + ":LOCAL:" + model.modelKey();
            if (seen.add(key)) {
                dependencies.add(new WorkflowDependencySnapshot.ModelDependencyReference(
                        node.nodeId(),
                        null,
                        model.modelKey(),
                        model.modelName(),
                        LOCAL_MODEL_PROVIDER,
                        LOCAL,
                        null,
                        BusinessStatus.ACTIVE
                ));
            }
        } catch (IllegalArgumentException exception) {
            issues.add(issue("RAG_LOCAL_MODEL_UNAVAILABLE", node.nodeId(), FIELD_EMBEDDING_MODEL_KEY,
                    "选择的平台内置本地 Embedding 模型不可用或未部署。", "请重新选择当前系统支持的内置模型。"));
        }
    }

    /** 解析远程 Embedding 模型（租户私有或平台共享） */
    private void resolveRemoteEmbeddingModel(
            Long tenantId,
            GraphNode node,
            RagModelSource source,
            List<WorkflowDependencySnapshot.ModelDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        Long modelId = longValue(node.config().get(FIELD_EMBEDDING_MODEL_ID));
        String modelKey = text(node.config(), FIELD_EMBEDDING_MODEL_KEY);
        Long expectedTenantId = source == RagModelSource.PLATFORM_SHARED
                ? ModelTenantScope.PLATFORM_TENANT_ID
                : tenantId;

        SysModelConfigEntity model = modelId == null ? null : modelMapper.selectById(modelId);
        boolean valid = model != null
                && expectedTenantId.equals(model.getTenantId())
                && BusinessStatus.ACTIVE.equals(model.getStatus())
                && ModelCapability.EMBEDDING.name().equals(model.getModelCapability())
                && model.getVectorDimension() != null
                && model.getVectorDimension() > 0
                && model.getModelKey().equals(modelKey);

        if (!valid) {
            issues.add(issue("RAG_REMOTE_MODEL_UNAVAILABLE", node.nodeId(), FIELD_EMBEDDING_MODEL_ID,
                    "选择的远程 Embedding 模型不存在、已禁用或向量维度非法。", "请在模型中心检查能力、向量维度和启用状态。"));
            return;
        }

        String key = "RAG_MODEL:" + node.nodeId() + ":" + model.getId();
        WorkflowDependencySnapshot.DependencySource dependencySource = source == RagModelSource.PLATFORM_SHARED
                ? PLATFORM_SHARED
                : TENANT_PRIVATE;
        if (seen.add(key)) {
            dependencies.add(new WorkflowDependencySnapshot.ModelDependencyReference(
                    node.nodeId(),
                    model.getId(),
                    model.getModelKey(),
                    model.getModelName(),
                    model.getProvider(),
                    dependencySource,
                    model.getTenantId(),
                    model.getStatus()
            ));
        }
    }

    /** 解析检索范围 */
    private RetrievalScopeType scope(GraphNode node) {
        String value = text(node.config(), FIELD_RETRIEVAL_SCOPE);
        try {
            return value == null ? RetrievalScopeType.VISIBLE_DOCUMENTS : RetrievalScopeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return RetrievalScopeType.VISIBLE_DOCUMENTS;
        }
    }

    /** 解析工具连接器依赖 */
    private void resolveConnector(
            Long tenantId,
            GraphNode node,
            List<WorkflowDependencySnapshot.ConnectorDependencyReference> dependencies,
            List<ValidationIssue> issues,
            Set<String> seen
    ) {
        Long connectorId = longValue(node.config().get(FIELD_CONNECTOR_ID));
        if (connectorId == null) {
            return;
        }
        PlatformToolConnectorEntity connector = connectorMapper.selectOne(
                new LambdaQueryWrapper<PlatformToolConnectorEntity>()
                        .eq(PlatformToolConnectorEntity::getTenantId, tenantId)
                        .eq(PlatformToolConnectorEntity::getId, connectorId)
                        .eq(PlatformToolConnectorEntity::getStatus, BusinessStatus.ACTIVE)
        );
        if (connector == null) {
            issues.add(issue("TOOL_CONNECTOR_UNAVAILABLE", node.nodeId(), FIELD_CONNECTOR_ID,
                    "引用的工具连接器不存在、尚未启用或不属于当前租户。", "请选择已开启且属于当前租户的平台工具连接器。"));
            return;
        }

        String key = "CONNECTOR:" + node.nodeId() + ":" + connectorId;
        if (seen.add(key)) {
            dependencies.add(new WorkflowDependencySnapshot.ConnectorDependencyReference(
                    node.nodeId(),
                    connectorId,
                    connector.getConnectorCode(),
                    connector.getStatus()
            ));
        }
    }

    /** 优先按当前租户查找模型，找不到再回退按平台租户查找共享模型 */
    private SysModelConfigEntity findModel(Long tenantId, String reference) {
        SysModelConfigEntity tenantModel = findModelByTenant(tenantId, reference);
        return tenantModel == null ? findModelByTenant(ModelTenantScope.PLATFORM_TENANT_ID, reference) : tenantModel;
    }

    /** 按租户和 ModelKey / ID 检索模型配置 */
    private SysModelConfigEntity findModelByTenant(Long tenantId, String reference) {
        LambdaQueryWrapper<SysModelConfigEntity> query = new LambdaQueryWrapper<SysModelConfigEntity>()
                .eq(SysModelConfigEntity::getTenantId, tenantId)
                .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)
                .and(wrapper -> wrapper.eq(SysModelConfigEntity::getModelKey, reference));
        Long id = longValue(reference);
        if (id != null) {
            query.or(wrapper -> wrapper.eq(SysModelConfigEntity::getTenantId, tenantId)
                    .eq(SysModelConfigEntity::getStatus, BusinessStatus.ACTIVE)
                    .eq(SysModelConfigEntity::getId, id));
        }
        return modelMapper.selectOne(query.last("LIMIT 1"));
    }

    /** 提取 Long 类型的 JSON 数组列表 */
    private List<Long> longValues(JsonNode config, String arrayField) {
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        JsonNode array = config.get(arrayField);
        if (array != null && array.isArray()) {
            array.forEach(item -> {
                Long value = longValue(item);
                if (value != null) {
                    result.add(value);
                }
            });
        }
        return List.copyOf(result);
    }

    /** 安全提取 Long */
    private Long longValue(JsonNode value) {
        return value == null || value.isNull() ? null : longValue(value.asText());
    }

    /** 安全解析字符串为 Long */
    private Long longValue(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 提取文本参数 */
    private String text(JsonNode config, String field) {
        JsonNode value = config.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    /** 快捷构造 ValidationIssue 对象 */
    private ValidationIssue issue(String code, String nodeId, String field, String reason, String suggestion) {
        return ValidationIssue.error(code, nodeId, "nodes[].config." + field, reason, suggestion);
    }

    /** 依赖分析解析结果 Record */
    public record Resolution(WorkflowDependencySnapshot snapshot, List<ValidationIssue> issues) {

        /**
         * 紧凑构造函数，防空列表。
         */
        public Resolution {
            issues = List.copyOf(issues);
        }

        /**
         * 判定依赖解析结果是否包含阻断异常。
         *
         * @return true 表示无校验错误
         */
        public boolean valid() {
            return issues.isEmpty();
        }
    }
}

