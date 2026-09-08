package com.acme.agentstudio.domain.runtime.model;

import java.util.Map;
import java.util.Set;

/**
 * Agent Runtime 生产环境部署边界与依赖组件能力声明 Record（Runtime Deployment Boundary）。
 * 包含部署版本 edition (DeploymentEdition)、组件集合 components (Set&lt;RuntimeComponent&gt;)、
 * 依赖中间件映射 dependencies、是否多实例集群模式 multiInstance、是否具备共享存储 sharedStorage 及是否开启灾备容灾 disasterRecoveryEnabled。
 *
 * @param edition 部署形态版本编码（CLOUD, PRIVATE_CLOUD, ON_PREMISE 等）
 * @param components 包含了组件集合（API, WORKER, MODEL_EXECUTOR 等）
 * @param dependencies 依赖的底层中间件（如 MySQL, Redis, Qdrant）
 * @param multiInstance 是否为多节点高可用集群部署
 * @param sharedStorage 是否配置了 NAS/S3 等跨节点共享存储
 * @param disasterRecoveryEnabled 是否开启同城双活/异地灾备
 */
public record RuntimeDeploymentBoundary(
        String edition,
        Set<RuntimeComponent> components,
        Map<String, String> dependencies,
        boolean multiInstance,
        boolean sharedStorage,
        boolean disasterRecoveryEnabled
) {
    /** 紧凑构造函数做多实例与共享存储关联断言 */
    public RuntimeDeploymentBoundary {
        if (edition == null || edition.isBlank() || components == null || components.isEmpty()) {
            throw new IllegalArgumentException("部署版本和 Runtime 组件不能为空");
        }
        components = Set.copyOf(components);
        dependencies = (dependencies == null) ? Map.of() : Map.copyOf(dependencies);
        if (multiInstance && !sharedStorage) {
            throw new IllegalArgumentException("多实例部署必须使用共享存储");
        }
    }
}

