package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 将 AgentScope 框架的 Worker 执行暴露为平台统一的对话（CHAT）、ReAct 循环与多 Agent 拓扑模式。
 */
@Component
public class AgentScopeStrategyAdapter extends AbstractFrameworkStrategyAdapter {

    /**
     * 构造函数注入受治理 Worker 协议。
     *
     * @param workerProtocol Worker 通信协议实现
     */
    public AgentScopeStrategyAdapter(GovernedWorkerProtocol workerProtocol) {
        super(workerProtocol);
    }

    /**
     * 获取对应的原生框架枚举类型。
     *
     * @return NativeFramework.AGENTSCOPE
     */
    @Override
    public NativeFramework framework() {
        return NativeFramework.AGENTSCOPE;
    }

    /**
     * 获取 AgentScope 适配器支持的运行模式集合。
     *
     * @return 包含 CHAT, REACT, MULTI_AGENT 的模式 Set 集合
     */
    @Override
    public Set<RuntimeMode> supportedModes() {
        return modes(RuntimeMode.CHAT, RuntimeMode.REACT, RuntimeMode.MULTI_AGENT);
    }
}

