package com.acme.agentstudio.application.workflow;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流程运行时变量只读与变更管理存储器（Workflow Variable Store）。
 * 作为运行过程中所有 input.xxx、variables.xxx、nodes.nodeId.output 等变量的唯一真理持有者，
 * 节点仅可读取只读快照 NodeInputValues，并通过提交 VariablePatch 更新全局变量状态。
 */
public final class WorkflowVariableStore {

    /** 内部维护的可变变量 Map 容器 */
    private final Map<String, Object> values;

    /**
     * 构造函数，使用初始变量 Map 字典进行初始化。
     *
     * @param initialValues 初始变量字典 Map
     */
    public WorkflowVariableStore(Map<String, Object> initialValues) {
        this.values = new LinkedHashMap<>(initialValues == null ? Map.of() : initialValues);
    }

    /**
     * 导出当前时刻所有变量的只读快照包裹对象。
     *
     * @return 节点输入只读快照 NodeInputValues 对象
     */
    public NodeInputValues snapshot() {
        return new NodeInputValues(values);
    }

    /**
     * 接收节点提交的补丁并更新当前全局变量存储状态。
     *
     * @param patch 节点输出变量更新补丁 VariablePatch 对象
     */
    public void apply(VariablePatch patch) {
        if (patch != null && patch.values() != null) {
            values.putAll(patch.values());
        }
    }

    /**
     * 获取内部包含所有变量项的可变 Map 实例（供引擎持久化运行状态时使用）。
     *
     * @return 变量项 Map 字典
     */
    public Map<String, Object> mutableState() {
        return values;
    }
}

