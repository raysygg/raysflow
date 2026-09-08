package com.acme.agentstudio.domain.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 生产运行环境唯一允许绑定的不可变发布包 Record（Workflow Release Bundle）。
 * 打包应用快照（ApplicationSnapshot）、版本号元数据（ReleaseSnapshot）、输入/输出 Schema、
 * 结构化拓扑图（GraphDefinition）、依赖项组件快照（WorkflowDependencySnapshot）以及运行时隔离策略（RuntimePolicy）。
 *
 * @param schema 发布包 Schema 版本号
 * @param application 应用级别快照
 * @param release 版本与指纹快照
 * @param inputSchema 生产入参校验 JSON Schema
 * @param outputSchema 生产出参结构 JSON Schema
 * @param graph 拓扑图结构对象
 * @param dependencies 冻结的依赖服务快照
 * @param runtimePolicy 运行时生产策略
 */
public record WorkflowReleaseBundle(
        String schema,
        ApplicationSnapshot application,
        ReleaseSnapshot release,
        JsonNode inputSchema,
        JsonNode outputSchema,
        GraphDefinition graph,
        WorkflowDependencySnapshot dependencies,
        RuntimePolicy runtimePolicy
) {
    /** 应用元数据快照 Record */
    public record ApplicationSnapshot(
            Long id,
            String code,
            String name,
            String graphType
    ) {
    }

    /** 版本号与指纹信息快照 Record */
    public record ReleaseSnapshot(
            String versionId,
            int versionNo,
            String environment,
            Long candidateId,
            String candidateFingerprint
    ) {
    }

    /** 运行时安全策略控制 Record */
    public record RuntimePolicy(
            boolean productionOnly,
            boolean snapshotRequired
    ) {
    }
}

