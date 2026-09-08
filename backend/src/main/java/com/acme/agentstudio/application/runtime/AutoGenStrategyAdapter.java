package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * AutoGen 外部框架策略适配器（AutoGen Strategy Adapter）。
 * 将 AutoGen 框架的 Worker 执行暴露为平台统一的多智能体（MULTI_AGENT）拓扑运行模式。
 */
@Component
public class AutoGenStrategyAdapter extends AbstractFrameworkStrategyAdapter {

    /**
     * 构造函数注入受治理 Worker 协议。
     *
     * @param workerProtocol Worker 通信协议实现
     */
    public AutoGenStrategyAdapter(GovernedWorkerProtocol workerProtocol) {
        super(workerProtocol);
    }

    /**
     * 获取对应的原生框架枚举类型。
     *
     * @return NativeFramework.AUTOGEN
     */
    @Override
    public NativeFramework framework() {
        return NativeFramework.AUTOGEN;
    }

    /**
     * 获取 AutoGen 适配器支持的运行模式集合。
     *
     * @return 包含 MULTI_AGENT 的模式 Set 集合
     */
    @Override
    public Set<RuntimeMode> supportedModes() {
        return modes(RuntimeMode.MULTI_AGENT);
    }
}

