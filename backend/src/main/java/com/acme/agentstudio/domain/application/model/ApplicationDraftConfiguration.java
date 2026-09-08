package com.acme.agentstudio.domain.application.model;

/**
 * ApplicationDrafturation 组件配置类。
 * 负责 Spring 容器内部 ApplicationDrafturation 相关 Bean 的装配与属性初始化。
 */
/**
 * 新建应用时允许提交的业务配置。
 * 技术执行策略不属于创建参数，应用创建后统一由主工作流承载执行过程。
 */
/**
 * ApplicationDrafturation 模块 Spring 容器配置类。
 * 负责 ApplicationDrafturation 相关的 Bean 初始化与组件注册。
 */
public record ApplicationDraftConfiguration(
        String goal,
        String inputName,
        String inputType,
        String outputName,
        String outputFormat
) {
    private static final String DEFAULT_GOAL = "完成用户请求";
    private static final String DEFAULT_INPUT_NAME = "request";
    private static final String DEFAULT_OUTPUT_NAME = "result";
    private static final String DEFAULT_OUTPUT_FORMAT = "text";

        /**
         * defaults 方法。
         * @return static ApplicationDraftConfiguration 返回对象
         */
    public static ApplicationDraftConfiguration defaults() {
        return new ApplicationDraftConfiguration(null, null, null, null, null);
    }

        /**
         * resolvedGoal 方法。
         * @return String 返回对象
         */
    public String resolvedGoal() {
        return textOrDefault(goal, DEFAULT_GOAL);
    }

        /**
         * resolvedInputName 方法。
         * @return String 返回对象
         */
    public String resolvedInputName() {
        return textOrDefault(inputName, DEFAULT_INPUT_NAME);
    }

    /** 解析输入字段类型，仅允许产品表单支持的基础类型。 */
    public String resolvedInputType() {
        return switch (textOrDefault(inputType, "string").toLowerCase()) {
            case "number", "boolean" -> textOrDefault(inputType, "string").toLowerCase();
            default -> "string";
        };
    }

        /**
         * resolvedOutputName 方法。
         * @return String 返回对象
         */
    public String resolvedOutputName() {
        return textOrDefault(outputName, DEFAULT_OUTPUT_NAME);
    }

        /**
         * resolvedOutputFormat 方法。
         * @return String 返回对象
         */
    public String resolvedOutputFormat() {
        return textOrDefault(outputFormat, DEFAULT_OUTPUT_FORMAT);
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
