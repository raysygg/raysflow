package com.acme.agentstudio.domain.workflow.model;

import java.util.List;

/**
 * 工作流不可变发布版本中冻结的外部依赖关系快照 Record（Workflow Dependency Snapshot）。
 * 冻结引用的模型资源、知识文档与外部连接器配置（排除机密 Key 凭证），用于审计发布门禁与运行版本还原。
 *
 * @param models 引用的模型资源快照列表 List&lt;ModelDependencyReference&gt;
 * @param documents 引用的知识文档快照列表 List&lt;DocumentDependencyReference&gt;
 * @param connectors 引用的外部连接器快照列表 List&lt;ConnectorDependencyReference&gt;
 */
public record WorkflowDependencySnapshot(
        List<ModelDependencyReference> models,
        List<DocumentDependencyReference> documents,
        List<ConnectorDependencyReference> connectors
) {
    /** 紧凑构造函数做 List 空值防御转换 */
    public WorkflowDependencySnapshot {
        models = (models == null) ? List.of() : List.copyOf(models);
        documents = (documents == null) ? List.of() : List.copyOf(documents);
        connectors = (connectors == null) ? List.of() : List.copyOf(connectors);
    }

    /**
     * 静态工厂方法：构建空依赖关系快照。
     *
     * @return 空的 WorkflowDependencySnapshot 实例
     */
    public static WorkflowDependencySnapshot empty() {
        return new WorkflowDependencySnapshot(List.of(), List.of(), List.of());
    }

    /**
     * 查找特定节点关联的模型依赖快照。
     *
     * @param nodeId 节点 ID
     * @return ModelDependencyReference 模型依赖对象，未找到返回 null
     */
    public ModelDependencyReference modelForNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return null;
        }
        return models.stream()
                .filter(model -> nodeId.equals(model.nodeId()))
                .findFirst()
                .orElse(null);
    }

    /** 模型依赖关系引用 Record */
    public record ModelDependencyReference(
            String nodeId,
            Long modelId,
            String modelKey,
            String name,
            String provider,
            DependencySource source,
            Long sourceTenantId,
            String status
    ) {
    }

    /** 知识文档依赖关系引用 Record */
    public record DocumentDependencyReference(
            String nodeId,
            Long documentId,
            String name,
            String status,
            String language,
            boolean languageConfirmed
    ) {
    }

    /** 外部连接器依赖关系引用 Record */
    public record ConnectorDependencyReference(
            String nodeId,
            Long connectorId,
            String name,
            String status
    ) {
    }

    /** 依赖模型/资源来源类型枚举 */
    public enum DependencySource {
        /** 租户私有独占资源 */
        TENANT_PRIVATE,

        /** 平台全局共享资源 */
        PLATFORM_SHARED,

        /** 本地离线/嵌入式资源 */
        LOCAL
    }
}

