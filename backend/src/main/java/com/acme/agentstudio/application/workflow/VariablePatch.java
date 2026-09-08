package com.acme.agentstudio.application.workflow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点输出变量更新补丁（Variable Patch Record）。
 * 节点在执行成功后，通过提交 VariablePatch 将新计算的数据统一写回工作流全局变量存储区 WorkflowVariableStore。
 *
 * @param values 不可变变量键值 Map
 */
public record VariablePatch(Map<String, Object> values) {

    /** 空变量补丁单例 */
    private static final VariablePatch EMPTY = new VariablePatch(Map.of());

    /**
     * 紧凑构造函数，防空并转换为只读 Map 结构。
     */
    public VariablePatch {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
    }

    /**
     * 获取空变量补丁对象。
     *
     * @return 空补丁实例
     */
    public static VariablePatch empty() {
        return EMPTY;
    }

    /**
     * 快速构建单变量更新补丁。
     *
     * @param name 变量名称
     * @param value 变量值
     * @return 变量补丁对象
     */
    public static VariablePatch of(String name, Object value) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(name, value);
        return new VariablePatch(values);
    }

    /**
     * 创建变量补丁构建器 Builder。
     *
     * @return Builder 构建器对象
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 变量补丁构建器。
     */
    public static final class Builder {

        /** 暂存键值字典 Map */
        private final Map<String, Object> values = new LinkedHashMap<>();

        /**
         * 向补丁中添加一个变量名值对。
         *
         * @param name 变量名称（非空）
         * @param value 变量值
         * @return 当前 Builder 实例
         */
        public Builder put(String name, Object value) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("流程变量名称不能为空。");
            }
            values.put(name, value);
            return this;
        }

        /**
         * 构建成不可变的 VariablePatch 实例。
         *
         * @return 变量补丁对象
         */
        public VariablePatch build() {
            return new VariablePatch(values);
        }
    }
}

