package com.acme.agentstudio.domain.runtime.model;

/**
 * 提示词模板动态参数变量定义 Record（Prompt Variable）。
 * 运行时按照此变量契约强类型校验输入，包含变量 Key name、数据类型 type、是否必填 required 以及中文说明描述 description。
 *
 * @param name 变量插值名称（如 user_query / context_docs）
 * @param type 变量类型声明（如 string, number, json, array）
 * @param required 渲染模板时是否强制必填
 * @param description 变量含义中文说明描述
 */
public record PromptVariable(
        String name,
        String type,
        boolean required,
        String description
) {
    /** 紧凑构造函数做输入验证校验 */
    public PromptVariable {
        if (name == null || name.isBlank() || type == null || type.isBlank()) {
            throw new IllegalArgumentException("Prompt 变量名称和类型不能为空");
        }
        description = (description == null) ? "" : description;
    }
}

