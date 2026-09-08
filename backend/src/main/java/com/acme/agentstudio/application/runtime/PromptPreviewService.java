package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.PromptAssetVersion;
import com.acme.agentstudio.domain.runtime.model.PromptPreviewResult;
import com.acme.agentstudio.domain.runtime.model.PromptVariable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 提示词实时渲染与结构化输出预览服务（Prompt Preview Service）。
 * 纯内存逻辑，在不触发真实 LLM 模型调用的情况下，为 Prompt 编辑器提供 Mustache 变量渲染预览（{{variable}}）、
 * 缺失变量诊断以及基于 JSON Schema 的结构化输出（Structured Output）语法校验。
 */
@Service
public class PromptPreviewService {

    /** 匹配 Prompt 变量占位符 {{variable}} 的正则表达式 */
    private static final String VARIABLE_PATTERN_SOURCE = "\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}";

    /** 正则 Pattern 编译对象 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(VARIABLE_PATTERN_SOURCE);

    /** JSON 类型常量 */
    private static final String JSON_TYPE = "json";

    /** Object 对象类型常量 */
    private static final String OBJECT_TYPE = "object";

    /** Array 数组类型常量 */
    private static final String ARRAY_TYPE = "array";

    /** Jackson JSON 映射器 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入 ObjectMapper 工具。
     */
    public PromptPreviewService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 渲染 Prompt 模板、计算缺失变量，并校验拟定的结构化输出 JSON。
     *
     * @param version Prompt 版本实体 PromptAssetVersion
     * @param variables 传入的渲染变量 Map
     * @param structuredOutput 拟定的结构化输出 JSON 字符串（可选）
     * @param outputSchema 期望的 JSON Schema Map（可选）
     * @return 预览校验结果对象 PromptPreviewResult
     */
    public PromptPreviewResult preview(
            PromptAssetVersion version,
            Map<String, Object> variables,
            String structuredOutput,
            Map<String, Object> outputSchema
    ) {
        if (version == null) {
            throw new IllegalArgumentException("预览校验的 Prompt 版本 PromptAssetVersion 不能为空。");
        }
        Map<String, Object> safeVariables = (variables == null) ? Map.of() : variables;
        List<String> missing = missingVariables(version, safeVariables);
        String rendered = render(version.template(), safeVariables);
        OutputValidation output = validateOutput(structuredOutput, outputSchema);

        return new PromptPreviewResult(
                version.versionId(),
                rendered,
                missing,
                output.valid(),
                output.value(),
                output.errors()
        );
    }

    /** 计算未在变量列表中提供的必填变量 */
    private List<String> missingVariables(PromptAssetVersion version, Map<String, Object> variables) {
        List<String> missing = new ArrayList<>();
        for (PromptVariable variable : version.variables()) {
            if (variable.required() && !variables.containsKey(variable.name())) {
                missing.add(variable.name());
            }
        }
        return List.copyOf(missing);
    }

    /** 使用变量填充模版中的 {{key}} 占位符 */
    private String render(String template, Map<String, Object> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /** 基于 Schema 校验结构化输出 JSON */
    private OutputValidation validateOutput(String raw, Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return new OutputValidation(true, Map.of(), List.of());
        }
        if (raw == null || raw.isBlank()) {
            return new OutputValidation(false, Map.of(), List.of("期望输出结构化 JSON，但提供的输出文本为空。"));
        }
        try {
            Object parsed = objectMapper.readValue(raw, Object.class);
            List<String> errors = new ArrayList<>();
            String expectedType = String.valueOf(schema.getOrDefault("type", JSON_TYPE));

            if (OBJECT_TYPE.equals(expectedType) && !(parsed instanceof Map<?, ?>)) {
                errors.add("结构化输出格式不匹配，期望类型为 JSON 对象 (Object)。");
            }
            if (ARRAY_TYPE.equals(expectedType) && !(parsed instanceof List<?>)) {
                errors.add("结构化输出格式不匹配，期望类型为 JSON 数组 (Array)。");
            }

            if (parsed instanceof Map<?, ?> map) {
                Object required = schema.get("required");
                if (required instanceof Iterable<?> fields) {
                    for (Object field : fields) {
                        if (!map.containsKey(String.valueOf(field))) {
                            errors.add("结构化输出缺少必填属性字段：" + field);
                        }
                    }
                }
            }

            Map<String, Object> value = (parsed instanceof Map<?, ?> map)
                    ? objectMapper.convertValue(map, new TypeReference<LinkedHashMap<String, Object>>() {})
                    : Map.of();
            return new OutputValidation(errors.isEmpty(), value, errors);
        } catch (Exception exception) {
            return new OutputValidation(false, Map.of(), List.of("结构化输出内容不是合法的 JSON 格式。"));
        }
    }

    /** 结构化输出校验中间逻辑传输 Record */
    private record OutputValidation(boolean valid, Map<String, Object> value, List<String> errors) {
    }
}

