package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.NodeType;
import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型与 AI 规则节点执行处理器集合抽象基类。
 */
abstract class AbstractModelNodeExecutionHandler implements NodeExecutionHandler {

    /** 变量引用解析器 */
    protected final VariableReferenceResolver resolver;

    /** 节点类型 */
    protected final String nodeType;

    /** 侧效应类型 */
    protected final NodeExecutionDescriptor.SideEffect sideEffect;

    /**
     * 构造函数初始化通用字段。
     */
    protected AbstractModelNodeExecutionHandler(VariableReferenceResolver resolver, String nodeType,
                                                NodeExecutionDescriptor.SideEffect sideEffect) {
        this.resolver = resolver;
        this.nodeType = nodeType;
        this.sideEffect = sideEffect;
    }

    /**
     * 返回节点执行描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(nodeType, sideEffect);
    }

    /**
     * 生成标准节点输出补丁。
     */
    protected VariablePatch outputPatch(NodeExecutionRequest request, Object output) {
        return VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), output);
    }

    /**
     * 构造 LLM 执行 Trace 追溯信息。
     */
    protected NodeExecutionTrace trace(LlmNodeExecutor.Result result) {
        return new NodeExecutionTrace(result.modelKey(), result.attempt(), result.contextUsage().retrievedCount());
    }

    /**
     * 校验并获取已冻结的模型引用。
     */
    protected WorkflowDependencySnapshot.ModelDependencyReference requireModelReference(NodeExecutionRequest request) {
        WorkflowDependencySnapshot.ModelDependencyReference reference = request.dependencies().modelForNode(request.nodeId());
        if (reference == null) {
            throw new IllegalStateException("当前节点 [" + request.nodeId() + "] 缺少已冻结的模型版本引用，请先保存并重新发布工作流版本。");
        }
        return reference;
    }
}

/**
 * 大语言模型节点执行处理器（LLM）。
 */
@Component
class LlmNodeExecutionHandler extends AbstractModelNodeExecutionHandler {

    /** LLM 大模型通用执行器 */
    private final LlmNodeExecutor executor;

    /**
     * 构造函数注入模型执行器与解析器。
     */
    LlmNodeExecutionHandler(LlmNodeExecutor executor, VariableReferenceResolver resolver) {
        super(resolver, NodeType.LLM.code(), NodeExecutionDescriptor.SideEffect.MODEL_CALL);
        this.executor = executor;
    }

    /**
     * 执行大语言模型生成逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        LlmNodeExecutor.Result result = executor.execute(
                request.tenantId(),
                request.config(),
                request.inputs().values(),
                requireModelReference(request),
                request.modelDeltaListener()
        );
        return NodeExecutionOutcome.model(result.text(), outputPatch(request, result.text()), trace(result));
    }
}

/**
 * 知识检索节点执行处理器（RAG）。
 */
@Component
class RagNodeExecutionHandler extends AbstractModelNodeExecutionHandler {

    /** RAG 检索执行器 */
    private final RagNodeExecutor executor;

    /**
     * 构造函数注入 RAG 执行器。
     */
    RagNodeExecutionHandler(RagNodeExecutor executor, VariableReferenceResolver resolver) {
        super(resolver, NodeType.RAG.code(), NodeExecutionDescriptor.SideEffect.KNOWLEDGE_RETRIEVAL);
        this.executor = executor;
    }

    /**
     * 执行知识检索逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        RagNodeExecutor.Result result = executor.execute(request.tenantId(), request.config(), request.inputs().values());
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("context", result.context());
        output.put("citations", result.citations());
        output.put("retrieval", result.summary());

        VariablePatch patch = VariablePatch.builder()
                .put(resolver.nodeOutputKey(request.nodeId()), output)
                .put(resolver.variableKey("rag_context"), result.context())
                .put(resolver.variableKey("rag_results"), result.results())
                .put(resolver.variableKey("rag_hit_count"), result.retrieval().hitCount())
                .build();
        return NodeExecutionOutcome.output(output, patch);
    }
}

/**
 * 抽象决策分支路由节点执行处理器。
 */
abstract class AbstractDecisionNodeExecutionHandler extends AbstractModelNodeExecutionHandler {

    /** 决策执行器 */
    protected final DecisionNodeExecutor executor;

    /**
     * 构造函数初始化决策处理器。
     */
    protected AbstractDecisionNodeExecutionHandler(DecisionNodeExecutor executor, VariableReferenceResolver resolver,
                                                    String nodeType, NodeExecutionDescriptor.SideEffect sideEffect) {
        super(resolver, nodeType, sideEffect);
        this.executor = executor;
    }

    /**
     * 构造路由 Outcome 结果。
     */
    protected NodeExecutionOutcome route(NodeExecutionRequest request, DecisionNodeExecutor.Result result) {
        return NodeExecutionOutcome.route(result.value(), result.port(), outputPatch(request, result.value()));
    }

    /**
     * 返回路由能力节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.routingCapability(nodeType, sideEffect);
    }
}

/**
 * 问题/意图分类节点执行处理器（QUESTION_CLASSIFIER）。
 */
@Component
class QuestionClassifierNodeExecutionHandler extends AbstractDecisionNodeExecutionHandler {

    /**
     * 构造函数初始化问题分类器。
     */
    QuestionClassifierNodeExecutionHandler(DecisionNodeExecutor executor, VariableReferenceResolver resolver) {
        super(executor, resolver, NodeType.QUESTION_CLASSIFIER.code(), NodeExecutionDescriptor.SideEffect.MODEL_CALL);
    }

    /**
     * 执行 LLM 问题分类分支路由。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return route(request, executor.classify(
                request.tenantId(),
                request.config(),
                request.inputs().values(),
                requireModelReference(request)
        ));
    }
}

/**
 * 条件分支路由节点执行处理器（ROUTER）。
 */
@Component
class RouterNodeExecutionHandler extends AbstractDecisionNodeExecutionHandler {

    /**
     * 构造函数初始化路由器。
     */
    RouterNodeExecutionHandler(DecisionNodeExecutor executor, VariableReferenceResolver resolver) {
        super(executor, resolver, NodeType.ROUTER.code(), NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 执行规则条件分支路由。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return route(request, executor.route(request.config(), request.inputs().values()));
    }
}

/**
 * 结构化参数提取节点执行处理器（PARAMETER_EXTRACTOR）。
 */
@Component
class ParameterExtractorNodeExecutionHandler extends AbstractModelNodeExecutionHandler {

    /** LLM 大模型通用执行器 */
    private final LlmNodeExecutor executor;

    /** Jackson JSON 映射组件 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入模型执行器与 JSON 映射器。
     */
    ParameterExtractorNodeExecutionHandler(LlmNodeExecutor executor, ObjectMapper objectMapper,
                                           VariableReferenceResolver resolver) {
        super(resolver, NodeType.PARAMETER_EXTRACTOR.code(), NodeExecutionDescriptor.SideEffect.MODEL_CALL);
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行结构化参数抽取消化逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        LlmNodeExecutor.Result result = executor.execute(
                request.tenantId(),
                request.config(),
                request.inputs().values(),
                requireModelReference(request),
                request.modelDeltaListener()
        );
        JsonNode output = parse(result.text());
        return NodeExecutionOutcome.model(output, outputPatch(request, output), trace(result));
    }

    /** 解析模型输出为 JSONNode */
    private JsonNode parse(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (Exception exception) {
            throw new IllegalArgumentException("参数提取节点模型生成的输出文本无法解析为合法 JSON 结构。", exception);
        }
    }
}

