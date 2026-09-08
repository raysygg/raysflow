package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.FrameworkCapability;
import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 平台原生框架能力矩阵注册中心（Framework Capability Matrix）。
 * 存储与管理内置框架（AUTOGEN / LANGGRAPH / CAMEL / AGENTSCOPE）的能力契约（流式传输、检查点持久化、人工接管、Worker 依赖等），
 * 并由已注册的 FrameworkStrategyAdapter 动态推导并导出 AdapterDescriptor 供前端呈现。
 */
@Component
public class FrameworkCapabilityMatrix {

    /** 默认框架适配器版本标识 */
    private static final String DEFAULT_CAPABILITY_VERSION = RuntimeStrategyContracts.DEFAULT_ADAPTER_VERSION;

    /** 框架能力映射表 */
    private final Map<NativeFramework, FrameworkCapability> capabilities;

    /** 所有适配器描述符列表 */
    private final List<RuntimeStrategyContracts.AdapterDescriptor> adapterDescriptors;

    /**
     * 构造函数：解析所有已注册的 FrameworkStrategyAdapter 并初始化能力矩阵。
     *
     * @param adapters 容器注入的所有框架适配器列表
     */
    public FrameworkCapabilityMatrix(List<FrameworkStrategyAdapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            throw new IllegalStateException("平台未注册任何原生框架策略适配器，无法初始化能力矩阵。");
        }
        this.adapterDescriptors = adapters.stream()
                .map(FrameworkStrategyAdapter::descriptor)
                .toList();

        EnumMap<NativeFramework, Set<RuntimeMode>> adapterModes = new EnumMap<>(NativeFramework.class);
        for (RuntimeStrategyContracts.AdapterDescriptor descriptor : adapterDescriptors) {
            Set<RuntimeMode> modes = descriptor.mappings().stream()
                    .map(RuntimeStrategyContracts.SemanticMapping::mode)
                    .collect(Collectors.toUnmodifiableSet());
            if (adapterModes.put(descriptor.framework(), modes) != null) {
                throw new IllegalStateException("检测到重复注册的原生框架适配器：" + descriptor.framework());
            }
        }

        EnumMap<NativeFramework, FrameworkCapability> defaults = new EnumMap<>(NativeFramework.class);
        defaults.put(NativeFramework.AUTOGEN, capability(
                NativeFramework.AUTOGEN,
                adapterModes.get(NativeFramework.AUTOGEN),
                true,
                true,
                true,
                true,
                Set.of("平台内置协作语义已注册，具体 Worker 执行器仍需完成远端能力接入")
        ));
        defaults.put(NativeFramework.LANGGRAPH, capability(
                NativeFramework.LANGGRAPH,
                adapterModes.get(NativeFramework.LANGGRAPH),
                true,
                true,
                true,
                true,
                Set.of("图状态必须映射为平台公开状态和事件，当前保留为诊断能力")
        ));
        defaults.put(NativeFramework.CAMEL, capability(
                NativeFramework.CAMEL,
                adapterModes.get(NativeFramework.CAMEL),
                true,
                false,
                false,
                true,
                Set.of("部分协作拓扑需要平台补充检查点，当前保留为诊断能力")
        ));
        defaults.put(NativeFramework.AGENTSCOPE, capability(
                NativeFramework.AGENTSCOPE,
                adapterModes.get(NativeFramework.AGENTSCOPE),
                true,
                true,
                true,
                true,
                Set.of("内置执行器必须遵守平台事件、预算和权限协议，当前保留为诊断能力")
        ));
        this.capabilities = Map.copyOf(defaults);
    }

    /**
     * 获取指定原生框架的能力描述，若未注册则抛出异常。
     *
     * @param framework 原生框架枚举 NativeFramework
     * @return 对应的框架能力契约 FrameworkCapability
     */
    public FrameworkCapability require(NativeFramework framework) {
        FrameworkCapability capability = capabilities.get(framework);
        if (capability == null) {
            throw new IllegalArgumentException("未找到已注册的原生框架能力：" + framework);
        }
        return capability;
    }

    /**
     * 获取全部框架与能力描述的只读 Map。
     *
     * @return 框架能力 Map
     */
    public Map<NativeFramework, FrameworkCapability> all() {
        return capabilities;
    }

    /**
     * 返回所有适配器的结构化映射描述符列表供前端动态渲染能力面板。
     *
     * @return 适配器描述符列表 List&lt;AdapterDescriptor&gt;
     */
    public List<RuntimeStrategyContracts.AdapterDescriptor> adapterDescriptors() {
        return adapterDescriptors;
    }

    /** 内部构造能力描述实体 */
    private FrameworkCapability capability(
            NativeFramework framework,
            Set<RuntimeMode> modes,
            boolean streaming,
            boolean checkpoint,
            boolean humanHandoff,
            boolean requiresWorker,
            Set<String> limitations
    ) {
        return new FrameworkCapability(
                framework,
                DEFAULT_CAPABILITY_VERSION,
                modes,
                streaming,
                checkpoint,
                humanHandoff,
                requiresWorker,
                limitations
        );
    }
}

