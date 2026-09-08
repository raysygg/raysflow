package com.acme.agentstudio.application.workflow;

import com.acme.agentstudio.domain.workflow.model.NodeType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文档内容提取节点执行处理器（DOCUMENT_EXTRACTOR）。
 * 从输入的文档对象（如 PDF/Word/TXT）中抽取纯文本正文内容并作为节点输出。
 */
@Component
class DocumentExtractorNodeExecutionHandler implements NodeExecutionHandler {

    /** 变量引用解析器 */
    private final VariableReferenceResolver resolver;

    /**
     * 构造函数注入依赖组件。
     */
    DocumentExtractorNodeExecutionHandler(VariableReferenceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 返回节点执行描述符。
     */
    @Override
    public NodeExecutionDescriptor descriptor() {
        return NodeDescriptors.capability(NodeType.DOCUMENT_EXTRACTOR.code(), NodeExecutionDescriptor.SideEffect.NONE);
    }

    /**
     * 执行文档解析提取逻辑。
     */
    @Override
    public NodeExecutionOutcome execute(NodeExecutionRequest request) {
        String reference = request.config().path("inputReference").asText("input.document");
        Object file = resolver.require(reference, request.inputs());
        Object output = file instanceof Map<?, ?> map && map.containsKey("text") ? map.get("text") : file;
        return NodeExecutionOutcome.output(output, VariablePatch.of(resolver.nodeOutputKey(request.nodeId()), output));
    }
}

