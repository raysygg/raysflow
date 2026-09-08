package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 提示词在线调试与实时渲染预览结果 Record（Prompt Preview Result）。
 * 输出版本 ID versionId、渲染替换后的完整文本 renderedPrompt、未填写的缺失变量列表 missingVariables、
 * 结构化输出校验是否合规 structuredOutputValid、解析后的 JSON 结构 structuredOutput 与校验报错集合 validationErrors。
 *
 * @param versionId 对应的 Prompt 版本 ID
 * @param renderedPrompt 插值渲染后的最终 System/User Prompt 字符串
 * @param missingVariables 模板中未获得赋值填写的缺失变量 Key 列表
 * @param structuredOutputValid 结构化输出（JSON Schema）校验是否通过
 * @param structuredOutput 模拟或真实解析的 JSON 结构数据 Map
 * @param validationErrors 语法与结构化 Schema 校验报错明细
 */
public record PromptPreviewResult(
        String versionId,
        String renderedPrompt,
        List<String> missingVariables,
        boolean structuredOutputValid,
        Map<String, Object> structuredOutput,
        List<String> validationErrors
) {
    /** 紧凑构造函数做输入属性校验 */
    public PromptPreviewResult {
        if (versionId == null || versionId.isBlank()) {
            throw new IllegalArgumentException("Prompt 版本标识不能为空");
        }
        renderedPrompt = (renderedPrompt == null) ? "" : renderedPrompt;
        missingVariables = (missingVariables == null) ? List.of() : List.copyOf(missingVariables);
        structuredOutput = (structuredOutput == null) ? Map.of() : Map.copyOf(structuredOutput);
        validationErrors = (validationErrors == null) ? List.of() : List.copyOf(validationErrors);
    }
}

