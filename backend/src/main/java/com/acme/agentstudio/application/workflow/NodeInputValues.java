package com.acme.agentstudio.application.workflow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点只读输入变量封装 Record（Node Input Values）。
 * 为具体的 NodeExecutionHandler 提供只读且不可修改的变量 Map 视图，防止节点处理器绕过编排引擎强行修改全局流程变量状态。
 *
 * @param values 不可变的只读输入变量 Map
 */
public record NodeInputValues(Map<String, Object> values) {

    /** 构造函数防护深拷贝与不可变封装 */
    public NodeInputValues {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
    }

    /**
     * 根据引用表达式 key 获取对应的原始变量对象。
     *
     * @param reference 变量引用键
     * @return 变量值对象，缺失时返回 null
     */
    public Object get(String reference) {
        return values.get(reference);
    }

    /**
     * 根据引用表达式 key 获取字符串形式的变量值。
     *
     * @param reference 变量引用键
     * @return 字符串变量值，缺失时返回空字符串 ""
     */
    public String text(String reference) {
        Object value = get(reference);
        return value == null ? "" : String.valueOf(value);
    }
}

