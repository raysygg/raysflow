package com.acme.agentstudio.application.runtime;

import com.acme.agentstudio.domain.runtime.model.NativeFramework;
import com.acme.agentstudio.domain.runtime.model.RuntimeMode;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * CAMEL 外部框架策略适配器（CAMEL Strategy Adapter）。
 * 将 CAMEL 框架的 Worker 执行暴露为平台统一的多智能体（MULTI_AGENT）角色扮演协作模式。
 */
@Component
public class CamelStrategyAdapter extends AbstractFrameworkStrategyAdapter {

    /**
     * 构造函数注入受治理 Worker 协议。
     *
     * @param workerProtocol Worker 通信协议实现
     */
    public CamelStrategyAdapter(GovernedWorkerProtocol workerProtocol) {
        super(workerProtocol);
    }

    /**
     * 获取对应的原生框架枚举类型。
     *
     * @return NativeFramework.CAMEL
     */
    @Override
    public NativeFramework framework() {
        return NativeFramework.CAMEL;
    }

    /**
     * 获取 CAMEL 适配器支持的运行模式集合。
     *
     * @return 包含 MULTI_AGENT 的模式 Set 集合
     */
    @Override
    public Set<RuntimeMode> supportedModes() {
        return modes(RuntimeMode.MULTI_AGENT);
    }
}

