package com.acme.agentstudio.domain.runtime.model;

import java.util.List;
import java.util.Map;

/**
 * 应用配置输入、输出和绑定的强类型契约类 Record（Application Configuration Contract）。
 * 约定运行输入契约 InputContract、输出契约 OutputContract 以及默认默认变量 Map，避免前端提交无法推演解析的无类型 JSON。
 *
 * @param input 应用输入契约描述
 * @param output 应用输出契约描述
 * @param defaults 默认变量与默认值配置 Map
 */
public record ApplicationConfigurationContract(
        InputContract input,
        OutputContract output,
        Map<String, Object> defaults
) {
    /** 紧凑构造函数做契约防空保护 */
    public ApplicationConfigurationContract {
        if (input == null) {
            throw new IllegalArgumentException("应用输入契约不能为空。");
        }
        if (output == null) {
            throw new IllegalArgumentException("应用输出契约不能为空。");
        }
        defaults = (defaults == null) ? Map.of() : Map.copyOf(defaults);
    }

    /** 输入契约参数 Record */
    public record InputContract(
            String name,
            String contentType,
            List<Field> fields
    ) {
        public InputContract {
            requireText(name, "输入名称不能为空。");
            requireText(contentType, "输入内容类型不能为空。");
            fields = (fields == null) ? List.of() : List.copyOf(fields);
        }
    }

    /** 输出契约参数 Record */
    public record OutputContract(
            String name,
            String contentType,
            List<Field> fields
    ) {
        public OutputContract {
            requireText(name, "输出名称不能为空。");
            requireText(contentType, "输出内容类型不能为空。");
            fields = (fields == null) ? List.of() : List.copyOf(fields);
        }
    }

    /** 单个参数字段描述 Record */
    public record Field(
            String name,
            String type,
            boolean required
    ) {
        public Field {
            requireText(name, "契约字段名称不能为空。");
            requireText(type, "契约字段类型不能为空。");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}

