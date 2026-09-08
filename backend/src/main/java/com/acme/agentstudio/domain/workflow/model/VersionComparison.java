package com.acme.agentstudio.domain.workflow.model;

import java.util.List;

/**
 * 两个不可变发布版本（或草稿与版本）之间的结构化拓扑 Diff 比较结果 Record（Version Comparison）。
 * 明确输出节点新增（addedNodeIds）、节点删除（removedNodeIds）、节点配置修改（modifiedNodeIds）
 * 以及连线结构变更、输入/输出契约变化与依赖组件变动的结构化对比报告。
 *
 * @param left 基准版本摘要对象（Left Version Summary）
 * @param right 目标对比版本摘要对象（Right Version Summary）
 * @param addedNodeIds 新增的节点 ID 列表
 * @param removedNodeIds 删除的节点 ID 列表
 * @param modifiedNodeIds 配置发生变更的节点 ID 列表
 * @param edgesChanged 有向边连线拓扑是否发生变化
 * @param inputContractChanged 输入参数契约是否发生变化
 * @param outputContractChanged 输出结构契约是否发生变化
 * @param dependenciesChanged 依赖的模型/工具资源快照是否发生变化
 */
public record VersionComparison(
        OrchestrationVersionSummary left,
        OrchestrationVersionSummary right,
        List<String> addedNodeIds,
        List<String> removedNodeIds,
        List<String> modifiedNodeIds,
        boolean edgesChanged,
        boolean inputContractChanged,
        boolean outputContractChanged,
        boolean dependenciesChanged
) {
}

