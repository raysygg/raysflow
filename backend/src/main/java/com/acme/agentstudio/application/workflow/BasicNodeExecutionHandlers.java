package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.NodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础工作流节点执行处理器集合辅助类。
 * 提供节点输出 VariablePatch 的构造工具及全局默认变量键定义。
 */
final class BasicNodeExecutionHandlers {

    /** 默认输出变量名称 */
    static final String DEFAULT_OUTPUT_VARIABLE = "output";

    /** 私有构造函数，防止实例化工具类 */
    private BasicNodeExecutionHandlers() {
    }

    /**
     * 生成标准节点输出补丁 VariablePatch。
     *
     * @param request 节点执行请求
     * @param resolver 变量引用解析器
     * @param output 节点输出结果对象
     * @return 变量修改补丁
     */
    static VariablePatch outputPatch(NodeExecutionRequest request, VariableReferenceResolver resolver, Object output) {
        return VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), output);
    }
}

/**
 * 直接回复节点执行处理器（DIRECT_REPLY）。
 * 渲染配置的消息模板并将结果作为响应和 message 变量输出。
 */
@Component
class DirectReplyNodeExecutionHandler implements NodeExecutionHandler {

    /** 变量引用解析器 */
    private final VariableReferenceResolver resolver;

    /**
     * 构造函数注入变量解析器。
     */
    DirectReplyNodeExecutionHandler(VariableReferenceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 返回节点执行描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(NodeType.DIRECT_REPLY.code(), NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 执行直接回复节点，渲染文本模板。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        String output = resolver.render(request.config().path("messageTemplate").asText(""), request.inputs());
        VariablePatch patch = VariablePatch.builder()
                .put(resolver.variableKey("message"), output)
                .put(resolver.nodeOutputKey(request.nodeId()), output)
                .build();
        return NodeExecutionOutcome.output(output, patch);
    }
}

/**
 * 抽象直通型节点执行处理器。
 * 用于 START、END、USER_INPUT 等不需要特殊转换、直接传递输入的控制流节点。
 */
abstract class AbstractPassThroughNodeExecutionHandler implements NodeExecutionHandler {

    /** 节点类型编码 */
    private final String nodeType;

    /** 控制信号 */
    private final NodeControlSignal signal;

    /**
     * 构造函数初始化直通节点。
     */
    protected AbstractPassThroughNodeExecutionHandler(String nodeType, NodeControlSignal signal) {
        this.nodeType = nodeType;
        this.signal = signal;
    }

    /**
     * 返回直通节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.control(nodeType, NodeExecutionDescriptor.SideEffect.NONE, false, signal);
    }

    /**
     * 执行直通节点逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return NodeExecutionOutcome.control(signal, request.inputs().values(), null, VariablePatch.empty());
    }
}

/**
 * 工作流起始节点执行处理器（START）。
 */
@Component
class StartNodeExecutionHandler extends AbstractPassThroughNodeExecutionHandler {
    StartNodeExecutionHandler() {
        super(NodeType.START.code(), NodeControlSignal.CONTINUE);
    }
}

/**
 * 工作流结束节点执行处理器（END）。
 */
@Component
class EndNodeExecutionHandler extends AbstractPassThroughNodeExecutionHandler {
    EndNodeExecutionHandler() {
        super(NodeType.END.code(), NodeControlSignal.COMPLETE);
    }
}

/**
 * 用户输入节点执行处理器（USER_INPUT）。
 */
@Component
class UserInputNodeExecutionHandler extends AbstractPassThroughNodeExecutionHandler {
    UserInputNodeExecutionHandler() {
        super(NodeType.USER_INPUT.code(), NodeControlSignal.CONTINUE);
    }
}

/**
 * 抽象数据转换型节点执行处理器。
 * 用于 TemplateTransform、VariableAssignment、VariableAggregator 等数据加工节点。
 */
abstract class AbstractTransformNodeExecutionHandler implements NodeExecutionHandler {

    /** 变量引用解析器 */
    protected final VariableReferenceResolver resolver;

    /** 节点类型 */
    private final String nodeType;

    /**
     * 构造函数注入依赖。
     */
    protected AbstractTransformNodeExecutionHandler(VariableReferenceResolver resolver, String nodeType) {
        this.resolver = resolver;
        this.nodeType = nodeType;
    }

    /**
     * 返回转换节点描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(nodeType, NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 包装输出结果对象。
     */
    protected NodeExecutionOutcome result(NodeExecutionRequest request, Object output) {
        return NodeExecutionOutcome.output(output, BasicNodeExecutionHandlers.outputPatch(request, resolver, output));
    }
}

/**
 * 模板转换节点执行处理器（TEMPLATE_TRANSFORM）。
 */
@Component
class TemplateTransformNodeExecutionHandler extends AbstractTransformNodeExecutionHandler {
    TemplateTransformNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.TEMPLATE_TRANSFORM.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return result(request, resolver.render(request.config().path("template").asText(""), request.inputs()));
    }
}

/**
 * 兼容性 Legacy 转换节点执行处理器（TRANSFORM）。
 */
@Component
class LegacyTransformNodeExecutionHandler extends AbstractTransformNodeExecutionHandler {
    LegacyTransformNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.TRANSFORM.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        return result(request, resolver.render(request.config().path("template").asText(""), request.inputs()));
    }
}

/**
 * 变量赋值节点执行处理器（VARIABLE_ASSIGNMENT）。
 */
@Component
class VariableAssignmentNodeExecutionHandler extends AbstractTransformNodeExecutionHandler {
    VariableAssignmentNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.VARIABLE_ASSIGNMENT.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        Object output = resolver.resolve(request.config().get("value"), request.inputs());
        String variable = request.config().path("variable").asText(BasicNodeExecutionHandlers.DEFAULT_OUTPUT_VARIABLE);
        VariablePatch patch = VariablePatch.builder()
                .put(resolver.variableKey(variable), output)
                .put(resolver.nodeOutputKey(request.nodeId()), output)
                .build();
        return NodeExecutionOutcome.output(output, patch);
    }
}

/**
 * 变量聚合节点执行处理器（VARIABLE_AGGREGATOR）。
 */
@Component
class VariableAggregatorNodeExecutionHandler extends AbstractTransformNodeExecutionHandler {
    VariableAggregatorNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.VARIABLE_AGGREGATOR.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        Map<String, Object> output = new LinkedHashMap<>();
        request.config().path("variables").fields().forEachRemaining(entry ->
                output.put(entry.getKey(), resolver.resolve(entry.getValue(), request.inputs())));
        return result(request, output);
    }
}

/**
 * 列表数据操作节点执行处理器（LIST_OPERATOR）。
 */
@Component
class ListOperatorNodeExecutionHandler extends AbstractTransformNodeExecutionHandler {
    ListOperatorNodeExecutionHandler(VariableReferenceResolver resolver) {
        super(resolver, NodeType.LIST_OPERATOR.code());
    }

    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        String reference = request.config().path("inputReference").asText("");
        Object raw = resolver.require(reference, request.inputs());
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("列表处理节点输入必须是 List/Array 数组类型。");
        }
        List<Object> output = new ArrayList<>(list);
        int take = request.config().path("takeN").asInt(-1);
        Object result = take >= 0 && take < output.size() ? output.subList(0, take) : output;
        return result(request, result);
    }
}

