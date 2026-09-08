package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.GovernedWorkerMessage;
import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeContext;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import com.acme.agentstudio.domain.runtime.model.RuntimeStrategyContracts;

import java.util.Set;

/**
 * 原生第三方框架策略适配器契约接口（Framework Strategy Adapter）。
 * 定义第三方开源 Agent 框架（如 AutoGen / LangGraph / Camel / AgentScope）接入平台时的通用标准：
 * 暴露支持的模式集合、描述符结构（Descriptor）以及转化为治理型 Worker 消息的启动抽象（GovernedWorkerMessage）。
 */
public interface FrameworkStrategyAdapter {

    /**
     * 获取当前适配器所针对的原生框架类型。
     *
     * @return 原生框架枚举 NativeFramework
     */
    NativeFramework framework();

    /**
     * 获取当前适配器所支持的平台运行模式集合。
     *
     * @return 运行模式集合 Set&lt;RuntimeMode&gt;
     */
    Set<RuntimeMode> supportedModes();

    /**
     * 返回框架模式与平台内置语义的显式映射描述符。
     *
     * @return 适配器描述符 AdapterDescriptor
     */
    RuntimeStrategyContracts.AdapterDescriptor descriptor();

    /**
     * 接收运行时上下文并初始化生成符合平台安全协议的治理 Worker 启动消息。
     *
     * @param context 运行上下文对象 RuntimeContext
     * @return 平台标准的 Worker 治理消息 GovernedWorkerMessage
     */
    GovernedWorkerMessage start(RuntimeContext context);
}

