package com.acme.agentstudio.domain.runtime.model;

import java.util.Set;

/**
 * 外部多智能体框架适配器向 Agent 平台暴露的能力边界描述 Record（Framework Capability）。
 * 明确框架名称 framework、适配器版本 adapterVersion、支持的运行模式 modes、是否支持流式生成 streaming、
 * 检查点 Savepoint/Checkpoint、人工接管审批 humanHandoff、是否需要独立 Worker 节点执行 requiresWorker 与限制集合 limitations。
 *
 * @param framework 适配的框架标识（NativeFramework：AUTOGEN, LANGGRAPH 等）
 * @param adapterVersion 适配器插件版本号
 * @param modes 支持的运行模式集合 Set&lt;RuntimeMode&gt;
 * @param streaming 是否支持 SSE/WebSocket 增量流式生成
 * @param checkpoint 是否支持 Checkpoint 挂起保存与断点恢复
 * @param humanHandoff 是否支持 Human-in-the-loop 人工接管交互
 * @param requiresWorker 是否必须在专有分布式 Worker 中运行
 * @param limitations 框架目前存在的限制约束集合
 */
public record FrameworkCapability(
        NativeFramework framework,
        String adapterVersion,
        Set<RuntimeMode> modes,
        boolean streaming,
        boolean checkpoint,
        boolean humanHandoff,
        boolean requiresWorker,
        Set<String> limitations
) {
    /** 紧凑构造函数做输入验证防空保护 */
    public FrameworkCapability {
        if (framework == null || adapterVersion == null || adapterVersion.isBlank()) {
            throw new IllegalArgumentException("框架和适配器版本不能为空");
        }
        modes = (modes == null) ? Set.of() : Set.copyOf(modes);
        limitations = (limitations == null) ? Set.of() : Set.copyOf(limitations);
    }
}

