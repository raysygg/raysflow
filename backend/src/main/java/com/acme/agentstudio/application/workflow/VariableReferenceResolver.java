package com.acme.agentstudio.application.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量引用与 Mustache 模板渲染解析器（Variable Reference Resolver）。
 * 统一解析节点输入配置中的 variables.xxx、input.xxx、nodes.nodeId.output 等命名空间变量表达式，并支持动态字符串模板渲染。
 */
@Component
public class VariableReferenceResolver {

    /** Mustache 风格变量引用匹配正则：{{ reference }} */
    private static final Pattern TEMPLATE_REFERENCE = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}");

    /** 前缀：全局流程变量命名空间 */
    private static final String PREFIX_VARIABLES = "variables.";

    /** 前缀：流程入口输入参数命名空间 */
    private static final String PREFIX_INPUT = "input.";

    /** 前缀：前置节点输出命名空间 */
    private static final String PREFIX_NODES = "nodes.";

    /** 前缀：上下文环境变量命名空间 */
    private static final String PREFIX_CONTEXT = "context.";

    /**
     * 解析 JsonNode 配置节点，若是变量引用字符串则提取对应的值，否则返回原始类型。
     *
     * @param value JsonNode 配置项
     * @param inputs 节点当前可用的输入变量映射容器
     * @return 解析后的对应变量值或原始对象
     */
    public Object resolve(JsonNode value, NodeInputValues inputs) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            return value.isValueNode() ? value.asText() : value;
        }
        String text = value.asText();
        return isReference(text) ? require(text, inputs) : text;
    }

    /**
     * 强校验并提取指定的变量引用值，若不存在则抛出 IllegalArgumentException 异常。
     *
     * @param reference 变量引用表达式（如 variables.query 或 nodes.node_1.output）
     * @param inputs 节点输入变量容器
     * @return 变量的具体真实值
     */
    public Object require(String reference, NodeInputValues inputs) {
        String storageKey = storageKey(reference);
        Object value = inputs.get(storageKey);
        if (value == null) {
            throw new IllegalArgumentException("当前节点引用的输入变量不存在或尚未计算赋值：" + reference);
        }
        return value;
    }

    /**
     * 渲染包含 Mustache 格式变量占位符的文本模板。
     *
     * @param template 包含 {{ reference }} 占位符的模板字符串
     * @param inputs 节点输入变量容器
     * @return 替换变量后的最终字符串结果
     */
    public String render(String template, NodeInputValues inputs) {
        Matcher matcher = TEMPLATE_REFERENCE.matcher(template == null ? "" : template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            Object value = require(matcher.group(1), inputs);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /**
     * 构造指定节点的默认输出 StorageKey（nodes.nodeId.output）。
     *
     * @param nodeId 节点 ID
     * @return 对应的 StorageKey 字符串
     */
    public String nodeOutputKey(String nodeId) {
        return PREFIX_NODES + nodeId + ".output";
    }

    /**
     * 构造全局变量的 StorageKey（variables.name）。
     *
     * @param name 变量名
     * @return 对应的 StorageKey 字符串
     */
    public String variableKey(String name) {
        return PREFIX_VARIABLES + name;
    }

    /** 判定字符串是否符合合法变量引用前缀 */
    private boolean isReference(String value) {
        return value.startsWith(PREFIX_INPUT) || value.startsWith(PREFIX_VARIABLES)
                || value.startsWith(PREFIX_NODES) || value.startsWith(PREFIX_CONTEXT);
    }

    /** 校验引用合法性并返回标准的 StorageKey */
    private String storageKey(String reference) {
        if (!isReference(reference)) {
            throw new IllegalArgumentException("变量引用格式非法，必须以 input.、variables.、nodes. 或 context. 开头：" + reference);
        }
        return reference;
    }
}

