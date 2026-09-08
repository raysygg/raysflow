package com.acme.agentstudio.application.application;

import com.acme.agentstudio.domain.workflow.model.WorkflowDependencySnapshot;
import com.acme.agentstudio.domain.application.ApplicationEntrypointType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务应用领域传输契约与值对象容器。
 * 包含跨模块与对外 REST/FE 交互的应用聚合体（ApplicationAggregate）、发布摘要（ReleaseSummary）、输入输出协议规范（Protocol）及运行策略定义。
 */
public final class ApplicationContracts {

    /** 私有构造函数，防止工具类被实例化 */
    private ApplicationContracts() {
    }

    /** 应用核心聚合体传输契约 */
    public record ApplicationAggregate(
            Long id,
            Long tenantId,
            String code,
            String name,
            String status,
            Long ownerId,
            String ownerName,
            WorkflowBinding mainWorkflow,
            WorkflowDependencySnapshot dependencies,
            List<EntryPointReference> entryPoints,
            ReleaseSummary currentRelease,
            LocalDateTime updatedAt
    ) {
        public ApplicationAggregate {
            dependencies = dependencies == null ? WorkflowDependencySnapshot.empty() : dependencies;
            entryPoints = entryPoints == null ? List.of() : List.copyOf(entryPoints);
        }
    }

    /** 发布版本摘要信息契约 */
    public record ReleaseSummary(String versionId, String environment, String status, LocalDateTime releasedAt,
                                 String bundleHash) {
    }

    /** 应用开放端点引用契约 */
    public record EntryPointReference(String id, ApplicationEntrypointType type, boolean enabled,
                                      String boundVersionId, boolean followProductionVersion) {
        public EntryPointReference {
            requireText(id, "应用入口标识不能为空");
            if (type == null) {
                throw new IllegalArgumentException("应用入口类型不能为空");
            }
            if (!followProductionVersion) {
                requireText(boundVersionId, "固定版本入口必须绑定发布版本");
            }
        }
    }

    /** 应用统一运行规格与快照契约 */
    public record ApplicationSpec(
            ApplicationIdentity identity,
            InputProtocol input,
            OutputProtocol output,
            RuntimePolicy runtime,
            WorkflowBinding workflow,
            WorkflowDependencySnapshot dependencies,
            ConfigurationSources sources
    ) {
    }

    /** 应用业务身份契约 */
    public record ApplicationIdentity(Long id, String code, String name, String status) {
    }

    /** 输入参数协议契约 */
    public record InputProtocol(String contentType, List<ProtocolField> fields) {
    }

    /** 输出结果协议契约 */
    public record OutputProtocol(String contentType, List<ProtocolField> fields) {
    }

    /** 协议字段元数据说明 */
    public record ProtocolField(String name, String type, boolean required, String description,
                                List<String> options, String resourceType) {
        public ProtocolField(String name, String type, boolean required, String description) {
            this(name, type, required, description, List.of(), null);
        }

        public ProtocolField {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    /** 运行策略契约 */
    public record RuntimePolicy(String environment, boolean requireRelease, int maxAttempts) {
    }

    /** 工作流绑定关系契约 */
    public record WorkflowBinding(String graphType, Integer draftRevisionNo, ReleaseSummary release) {
    }

    /** 配置来源依赖关系契约 */
    public record ConfigurationSources(String prompt, String model, String context,
                                       String memory, String knowledge, String tools) {
    }

    /**
     * 辅助校验非空字符串。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}

